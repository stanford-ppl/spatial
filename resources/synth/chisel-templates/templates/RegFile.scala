package templates

import util._
import chisel3._
import templates.Utils.log2Up
import chisel3.util.{MuxLookup, Mux1H}
import Utils._
import ops._
import fringe._

/*
           Registers Layout                                       
                                                  
        0 -> 1 -> 2 ->  3                                
                                                  
        4 -> 5 -> 6 ->  7                                
                                                  
        8 -> 9 -> 10 -> 11                           
                                                  
                                                                                       
                                                  
                                                  
*/


class RegW_Info(val ofs_width:Int, val bank_width:List[Int], val data_width:Int) extends Bundle {
  val banks = HVec.tabulate(bank_width.length){i => UInt(bank_width(i).W)}
  val ofs = UInt(ofs_width.W)
  val data = UInt(data_width.W)
  val en = Bool()
  val shiftEn = Bool()

  override def cloneType = (new RegW_Info(ofs_width, bank_width, data_width)).asInstanceOf[this.type] // See chisel3 bug 358
}

class RegR_Info(val ofs_width:Int, val bank_width:List[Int]) extends Bundle {
  val banks = HVec.tabulate(bank_width.length){i => UInt(bank_width(i).W)}
  val ofs = UInt(ofs_width.W)
  val en = Bool()

  override def cloneType = (new RegR_Info(ofs_width, bank_width)).asInstanceOf[this.type] // See chisel3 bug 358
}

// This exposes all registers as output ports now
class ShiftRegFile(val dims: List[Int], val banks: List[Int], val bankDepth: Int, val inits: Option[Map[List[Int], Double]], val stride: Int, 
  val wPar: Int, val isBuf: Boolean, val numReaders: Int, val bitWidth: Int, val fracBits: Int) extends Module {

  def this(tuple: (List[Int], List[Int], Int, Option[Map[List[Int], Double]], Int, Int, Boolean, Int, Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9, tuple._10)

  val muxWidth = Utils.log2Up(dims.reduce{_*_})
  val portWidth = banks.length+1
  val numMems = banks.product * bankDepth
  assert(numMems == dims.product)

  // Console.println(s"dims are $dims, banks $banks $bankDepth, num mmems $numMems, wparstride $wPar * $stride, readers $numReaders")

  // Console.println(" " + dims.reduce{_*_} + " " + wPar + " " + dims.length)
  val io = IO(new Bundle { 
    // Signals for dumping data from one buffer to next
    val dump_out = Vec(numMems, Output(UInt(bitWidth.W)))
    val dump_data = Vec(numMems, Input(UInt(bitWidth.W)))
    val dump_en = Input(Bool())

    val w = Vec(1 max (wPar * stride), Input(new RegW_Info(32, List.fill(banks.length)(32), bitWidth)))
    val r = Vec(1 max numReaders, Input(new RegR_Info(32, List.fill(banks.length)(32)))) 

    val reset    = Input(Bool())
    val data_out = Vec(1 max numReaders, Output(UInt(bitWidth.W)))

  })

  val registers = (0 until numMems).map{ i => 
    val coords = (banks :+ bankDepth).zipWithIndex.map{ case (b,j) => 
      i % ((banks :+ bankDepth).drop(j).product) / (banks :+ bankDepth).drop(j+1).product
    }

    val initval = if (inits.isDefined) (inits.get.apply(coords)*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W) else 0.U(bitWidth.W)
    val mem = RegInit(initval)
    io.dump_out(i) := mem
    (mem,coords,i)
  }


  (0 until numReaders).map{ j => 
    val bitmask = registers.map{mem => (0 until banks.length).map{k => io.r(j).banks(k) === mem._2(k).U}.reduce{_&&_} && io.r(j).ofs === mem._2.last.U}
    io.data_out(j) := Mux1H(bitmask, registers.map(_._1))
  }

  if (wPar > 0) { // If it is not >0, then this should just be a pass-through in an nbuf
    // Connect a w port to each reg
    (numMems-1 to 0 by -1).foreach { i => 
      // Construct n-D coords
      val coords = registers(i)._2
      when(io.reset) {
        if (inits.isDefined) {
          registers(i)._1 := (inits.get.apply(coords)*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W)
        } else {
          registers(i)._1 := 0.U(bitWidth.W)            
        }
      }.elsewhen(io.dump_en) {
        registers(i)._1 := io.dump_data(i)
      }.otherwise {
        if (wPar * stride > 1) {
          // Address flattening
          val w_addrs_match = (0 until wPar*stride).map{ wnum => (0 until portWidth - 1).map{j => io.w(wnum).banks(j) === coords(j).U(32.W)}.reduce{_&&_} && io.w(wnum).ofs === coords.last.U(32.W)}

          val write_here = (0 until wPar * stride).map{ wnum => io.w(wnum).en & w_addrs_match(wnum) }
          val shift_entry_here =  (0 until wPar * stride).map{ wnum => io.w(wnum).shiftEn & w_addrs_match(wnum) }
          val write_data = Mux1H(write_here.zip(shift_entry_here).map{case (a,b) => a|b}, io.w)
          // val shift_data = Mux1H(shift_entry_here, io.w)
          val has_writer = write_here.reduce{_|_}
          val has_shifter = shift_entry_here.reduce{_|_}

          // Assume no bozos will shift mid-axis
          val shift_axis = (0 until wPar * stride).map{ wnum => io.w(wnum).shiftEn & {if (dims.length > 1) {(coords.last >= stride).B & io.w(wnum).banks.zip(coords.dropRight(1)).map{case(a,b) => a === b.U(32.W)}.reduce{_&_}} else {(coords.last >= stride).B} }}.reduce{_|_}
          val producing_reg = coords.dropRight(1) :+ (0 max (coords.last - stride))
          // Console.println(s"coords $coords receives shift from ${producing_reg}")
          registers(i)._1 := Mux(shift_axis, registers.filter(_._2 == producing_reg).head._1, Mux(has_writer | has_shifter, write_data.data, registers(i)._1))
        } else {
          // Address flattening
          val w_addr_match = (0 until portWidth - 1).map{j => io.w(0).banks(j) === coords(j).U(32.W)}.reduce{_&&_} && io.w(0).ofs === coords.last.U(32.W)

          val write_here = io.w(0).en & w_addr_match
          val shift_entry_here =  io.w(0).shiftEn & w_addr_match
          val write_data = io.w(0).data
          // val shift_data = Mux1H(shift_entry_here, io.w)
          val has_writer = write_here
          val has_shifter = shift_entry_here

          // Assume no bozos will shift mid-axis
          val shift_axis = io.w(0).shiftEn & {if (dims.length > 1) {(coords.last >= stride).B & io.w(0).banks.zip(coords.dropRight(1)).map{case(a,b) => a === b.U}.reduce{_&_} } else {(coords.last >= stride).B} }
          val producing_reg = coords.dropRight(1) :+ (0 max (coords.last - stride))
          registers(i)._1 := Mux(shift_axis, registers.filter(_._2 == producing_reg).head._1, Mux(has_writer | has_shifter, write_data.data, registers(i)._1))
        }
      }
    }
  } else {
    when(io.reset) {
      for (i <- 0 until numMems) {
        val coords = registers(i)._2
        if (inits.isDefined) {
          registers(i)._1 := (inits.get.apply(coords)*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W)
        } else {
          registers(i)._1 := 0.U(bitWidth.W)            
        }
      }
    }.elsewhen(io.dump_en) {
      for (i <- 0 until dims.reduce{_*_}) {
        registers(i)._1 := io.dump_data(i)
      }
    }.otherwise{
      for (i <- 0 until dims.reduce{_*_}) {
        registers(i)._1 := registers(i)._1
      }      
    }
  }




  var wId = 0
  def connectWPort(wBundle: Vec[RegW_Info], ports: List[Int]) {
    assert(ports.head == 0)
    (0 until wBundle.length).foreach{ i => 
      io.w(wId+i) := wBundle(i)
    }
    wId += wBundle.length
  }

  def connectShiftPort(wBundle: Vec[RegW_Info], ports: List[Int]) {
    assert(ports.head == 0)
    (0 until wBundle.length).foreach{ i => 
      io.w(wId+i) := wBundle(i)
    }
    wId += wBundle.length
  }

  var rId = 0
  def connectRPort(addrs: RegR_Info, port: Int): Int = {
    io.r(rId) := addrs
    rId = rId + 1
    rId - 1
  }
  
}



// TODO: Currently assumes one write port, possible read port on every buffer
class NBufShiftRegFile(val dims: List[Int], val banks: List[Int], val bankDepth: Int, val inits: Option[Map[List[Int], Double]], val stride: Int, val numBufs: Int,
                       val wPar: Map[Int,Int], val numReaders: List[Int], val bitWidth: Int, val fracBits: Int) extends Module { 

  def this(tuple: (List[Int], List[Int], Int, Option[Map[List[Int], Double]], Int, Int, Map[Int,Int], List[Int], Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9, tuple._10)
  assert(numBufs == numReaders.length)

  val muxWidth = Utils.log2Up(numBufs*dims.reduce{_*_})
  val portWidth = banks.length+1
  val numMems = banks.product * bankDepth
  assert(numMems == dims.product)

  val io = IO(new Bundle { 
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val w = Vec(wPar.values.reduce{_+_}*stride, Input(new RegW_Info(32, List.fill(banks.length)(32), bitWidth)))
    val r = Vec(numReaders.sum, Input(new RegR_Info(32, List.fill(banks.length)(32))))
    val reset    = Input(Bool())
    val data_out = Vec(numReaders.sum, Output(UInt(bitWidth.W)))
  })
  
  val sEn_latch = (0 until numBufs).map{i => Module(new SRFF())}
  val sDone_latch = (0 until numBufs).map{i => Module(new SRFF())}

  val swap = Wire(Bool())

  // Latch whether each buffer's stage is enabled and when they are done
  (0 until numBufs).foreach{ i => 
    sEn_latch(i).io.input.set := io.sEn(i) & ~io.sDone(i)
    sEn_latch(i).io.input.reset := Utils.getRetimed(swap,1)
    sEn_latch(i).io.input.asyn_reset := reset
    sDone_latch(i).io.input.set := io.sDone(i)
    sDone_latch(i).io.input.reset := Utils.getRetimed(swap,1)
    sDone_latch(i).io.input.asyn_reset := reset
  }
  val anyEnabled = sEn_latch.map{ en => en.io.output.data }.reduce{_|_}
  // swap := sEn_latch.zip(sDone_latch).map{ case (en, done) => en.io.output.data === done.io.output.data }.reduce{_&_} & anyEnabled
  swap := Utils.risingEdge(sEn_latch.zip(sDone_latch).zipWithIndex.map{ case ((en, done), i) => en.io.output.data === (done.io.output.data || io.sDone(i)) }.reduce{_&_} & anyEnabled)

  val shiftRegs = (0 until numBufs).map{i => Module(new ShiftRegFile(dims, banks, bankDepth, inits, stride, wPar.getOrElse(i,1), isBuf = {i>0}, numReaders(i), bitWidth, fracBits))}

  var cnts = scala.collection.mutable.ListBuffer.fill(numBufs)(0)
  for (i <- 0 until numBufs) {
    for (j <- 0 until numReaders(i)) {
      io.data_out(cnts.sum) := shiftRegs(i).io.data_out(j)
      cnts(i) = cnts(i) + 1
    }
  }

  wPar.foreach{ case (regId, par) => 
    val base = ((0 until regId).map{i => wPar.getOrElse(i,0)} :+ 0).reduce{_+_}*stride
    (0 until par*stride).foreach{ i => shiftRegs(regId).io.w(i) := io.w(base + (par*stride-i-1))}
    if (regId == 0) {
      shiftRegs(regId).io.reset := io.reset
      shiftRegs(regId).io.dump_en := false.B // No dumping into first regfile
    }
  }

  for (i <- numBufs-1 to 1 by -1) {
    shiftRegs(i).io.dump_en := swap
    shiftRegs(i).io.dump_data := shiftRegs(i-1).io.dump_out
    if (!wPar.keys.toList.contains(i)) {
      shiftRegs(i).io.w.foreach{p => p.shiftEn := false.B; p.en := false.B; p.data := 0.U}
    }
    shiftRegs(i).io.reset := io.reset
  }

  var x = 0
  shiftRegs.zip(numReaders).zipWithIndex.foreach{ case ((reg, nr),k) => 
    (0 until numReaders(k)).foreach{ i =>
      reg.io.r(i) := io.r(x)
      x = x + 1
    }
  }

  var wIdMap = (0 until numBufs).map{ i => (i -> 0) }.toMap
  def connectWPort(wBundle: Vec[RegW_Info], ports: List[Int]) {
    assert(ports.length == 1)
    val base = ((0 until ports.head).map{i => wPar.getOrElse(i,0)} :+ 0).reduce{_+_}
    val portbase = wIdMap(ports.head)
    (0 until wBundle.length).foreach{ i => 
      io.w(base+portbase+i) := wBundle(i)
    }
    val newbase = portbase + wBundle.length
    wIdMap += (ports.head -> newbase)
  }

  def connectShiftPort(wBundle: Vec[RegW_Info], ports: List[Int]) {
    assert(ports.length == 1)
    val base = ((0 until ports.head).map{i => wPar.getOrElse(i,0)} :+ 0).reduce{_+_}
    val portbase = wIdMap(ports.head)
    (0 until wBundle.length).foreach{ i => 
      io.w(base+portbase+i) := wBundle(i)
    }
    val newbase = portbase + wBundle.length
    wIdMap += (ports.head -> newbase)
  }

  var rId = scala.collection.mutable.ListBuffer.fill(numBufs)(0)
  def connectRPort(addrs: RegR_Info, port: Int): Int = {
    val base = (cnts.take(port).sum + rId(port))
    io.r(base) := addrs
    rId(port) = rId(port) + 1
    cnts.take(port).sum + rId(port) - 1
  }


  def connectStageCtrl(done: Bool, en: Bool, ports: List[Int]) {
    ports.foreach{ port => 
      io.sEn(port) := en
      io.sDone(port) := done
    }
  }

  
}


class LUT(val dims: List[Int], val banks: List[Int], val bankDepth: Int, val inits: Map[List[Int], Double], val numReaders: Int, val width: Int, val fracBits: Int) extends Module {

  def this(tuple: (List[Int], List[Int], Int, Map[List[Int], Double], Int, Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7)
  val muxWidth = Utils.log2Up(dims.reduce{_*_})
  val portWidth = banks.length+1

  val io = IO(new Bundle { 
    val r = Vec(numReaders, Input(new RegR_Info(32, List.fill(banks.length)(32))))
    // val en = Vec(numReaders, Input(Bool()))
    val data_out = Vec(numReaders, Output(UInt(width.W)))
  })

  val numMems = banks.product * bankDepth
  val m = (0 until numMems).map{ i => 
    val coords = (banks :+ bankDepth).zipWithIndex.map{ case (b,j) => 
      i % ((banks :+ bankDepth).drop(j).product) / (banks :+ bankDepth).drop(j+1).product
    }

    val initval = (inits(coords)*scala.math.pow(2,fracBits)).toLong
    val mem = initval.S((width+1).W).apply(width-1,0)
    (mem,coords,i)
  }

  (0 until numReaders).map{ j => 
    val bitmask = m.map{mem => (0 until banks.length).map{k => io.r(j).banks(k) === mem._2(k).U}.reduce{_&&_} && io.r(j).ofs === mem._2.last.U}
    io.data_out(j) := Mux1H(bitmask, m.map(_._1))
  }

  var rId = 0
  def connectRPort(addrs: RegR_Info): Int = {
    io.r(rId) := addrs
    rId = rId + 1
    rId - 1
  }
  
}
