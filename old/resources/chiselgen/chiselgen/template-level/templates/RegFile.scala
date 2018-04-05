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


class multidimRegW(val N: Int, val dims: List[Int], val w: Int) extends Bundle {
  val addr = HVec.tabulate(N){i => UInt((Utils.log2Up(dims(i))).W)}
  val data = UInt(w.W)
  val en = Bool()
  val shiftEn = Bool()

  override def cloneType = (new multidimRegW(N, dims, w)).asInstanceOf[this.type] // See chisel3 bug 358
}


// This exposes all registers as output ports now
class ShiftRegFile(val dims: List[Int], val inits: Option[List[Double]], val stride: Int, 
  val wPar: Int, val isBuf: Boolean, val bitWidth: Int, val fracBits: Int) extends Module {

  def this(tuple: (List[Int], Option[List[Double]], Int, Int, Boolean, Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7)

  val muxWidth = Utils.log2Up(dims.reduce{_*_})

  // Console.println(" " + dims.reduce{_*_} + " " + wPar + " " + dims.length)
  val io = IO(new Bundle { 
    // Signals for dumping data from one buffer to next
    val dump_data = Vec(dims.reduce{_*_}, Input(UInt(bitWidth.W)))
    val dump_en = Input(Bool())

    val w = Vec(wPar * stride, Input(new multidimRegW(dims.length, dims, bitWidth)))

    val reset    = Input(Bool())
    val data_out = Vec(dims.reduce{_*_}, Output(UInt(bitWidth.W)))
  })
  
  // if (!isBuf) {io.dump_en := false.B}

  // val size_rounded_up = ((dims.head+stride-1)/stride)*stride // Unlike shift reg, for shift reg file it is user's problem if width does not match (no functionality guarantee)
  val registers = if (inits.isDefined){
    List.tabulate(dims.reduce{_*_}){i => 
      val initval = (inits.get.apply(i)*-*scala.math.pow(2,fracBits)).toLong
      RegInit(initval.U(bitWidth.W))
    }
  } else {
    List.fill(dims.reduce{_*_})(RegInit(0.U(bitWidth.W))) // Note: Can change to use FF template
  }
  for (i <- 0 until dims.reduce{_*_}) {
    io.data_out(i) := registers(i)
  }


  // val flat_sh_addrs = if (dims.length == 1) 0.U else {
  //     io.w.addr.dropRight(1).zipWithIndex.map{ case (addr, i) =>
  //     addr *-* (dims.drop(i).reduce{_.*-*(_,None)}/-/dims(i)).U
  //   }.reduce{_+_}
  // }

  if (wPar > 0) { // If it is not >0, then this should just be a pass-through in an nbuf
    // Connect a w port to each reg
    (dims.reduce{_*_}-1 to 0 by -1).foreach { i => 
      // Construct n-D coords
      val coords = (0 until dims.length).map { k => 
        if (k + 1 < dims.length) {(i /-/ dims.drop(k+1).reduce{_*_}) % dims(k)} else {i % dims(k)}
      }
      when(io.reset) {
        if (inits.isDefined) {
          registers(i) := (inits.get.apply(i)*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W)
        } else {
          registers(i) := 0.U(bitWidth.W)            
        }
      }.elsewhen(io.dump_en) {
        for (i <- 0 until dims.reduce{_*_}) {
          registers(i) := io.dump_data(i)
        }
      }.otherwise {
        if (wPar * stride > 1) {
          // Address flattening
          val flat_w_addrs = io.w.zipWithIndex.map{ case (bundle, port_num) =>
            bundle.addr.zipWithIndex.map{case (a, ii) => 
              // fringe.FringeGlobals.bigIP.multiply(a, (dims.drop(ii).reduce{_*_}/-/dims(ii)).U, 0)
              a.*-*((dims.drop(ii).reduce{_*_}/-/dims(ii)).U, None)
            }.reduce{_+_} + (port_num % stride).U // Remove the port_num % stride part if strided shifts actually address into regfile correctly
          }

          val write_here = (0 until wPar * stride).map{ ii => io.w(ii).en & (flat_w_addrs(ii) === i.U) }
          val shift_entry_here =  (0 until wPar * stride).map{ ii => io.w(ii).shiftEn & (flat_w_addrs(ii) === i.U) }
          val write_data = Mux1H(write_here.zip(shift_entry_here).map{case (a,b) => a|b}, io.w)
          // val shift_data = Mux1H(shift_entry_here, io.w)
          val has_writer = write_here.reduce{_|_}
          val has_shifter = shift_entry_here.reduce{_|_}

          // Assume no bozos will shift mid-axis
          val shift_axis = (0 until wPar * stride).map{ ii => io.w(ii).shiftEn & {if (dims.length > 1) {(coords.last >= stride).B & io.w(ii).addr.dropRight(1).zip(coords.dropRight(1)).map{case(a,b) => a === b.U}.reduce{_&_} } else {(coords.last >= stride).B} }}.reduce{_|_}
          val producing_reg = 0 max (i - stride)
          registers(i) := Mux(shift_axis, registers(producing_reg), Mux(has_writer | has_shifter, write_data.data, registers(i)))
        } else {
          // Address flattening
          val flat_w_addrs = io.w(0).addr.zipWithIndex.map{case (a, i) => 
            // fringe.FringeGlobals.bigIP.multiply(a, (dims.drop(i).reduce{_.*-*(_,None)}/-/dims(i)).U, 0)
            a.*-*((dims.drop(i).reduce{_*_}/-/dims(i)).U, None)
          }.reduce{_+_}

          val write_here = io.w(0).en & (flat_w_addrs === i.U)
          val shift_entry_here =  io.w(0).shiftEn & (flat_w_addrs === i.U) 
          val write_data = io.w(0).data
          // val shift_data = Mux1H(shift_entry_here, io.w)
          val has_writer = write_here
          val has_shifter = shift_entry_here

          // Assume no bozos will shift mid-axis
          val shift_axis = io.w(0).shiftEn & {if (dims.length > 1) {(coords.last >= stride).B & io.w(0).addr.dropRight(1).zip(coords.dropRight(1)).map{case(a,b) => a === b.U}.reduce{_&_} } else {(coords.last >= stride).B} }
          val producing_reg = 0 max (i - stride)
          registers(i) := Mux(shift_axis, registers(producing_reg), Mux(has_writer | has_shifter, write_data.data, registers(i)))
        }
      }
    }
  } else {
    when(io.reset) {
      for (i <- 0 until dims.reduce{_*_}) {
        if (inits.isDefined) {
          registers(i) := (inits.get.apply(i)*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W)
        } else {
          registers(i) := 0.U(bitWidth.W)            
        }
      }
    }.elsewhen(io.dump_en) {
      for (i <- 0 until dims.reduce{_*_}) {
        registers(i) := io.dump_data(i)
      }
    }.otherwise{
      for (i <- 0 until dims.reduce{_*_}) {
        registers(i) := registers(i)
      }      
    }
  }


  var wId = 0
  def connectWPort(wBundle: Vec[multidimRegW], ports: List[Int]) {
    assert(ports.head == 0)
    (0 until wBundle.length).foreach{ i => 
      io.w(wId+i) := wBundle(i)
    }
    wId += wBundle.length
  }

  def connectShiftPort(wBundle: Vec[multidimRegW], ports: List[Int]) {
    assert(ports.head == 0)
    (0 until wBundle.length).foreach{ i => 
      io.w(wId+i) := wBundle(i)
    }
    wId += wBundle.length
  }

  def readValue(addrs: List[UInt], port: Int): UInt = { // This randomly screws up sometimes
    // chisel seems to have broke MuxLookup here...

    val result = Wire(UInt(bitWidth.W))
    val regvals = (0 until dims.reduce{_*_}).map{ i => 
      (i.U(muxWidth.W) -> io.data_out(i)) 
    }
    val flat_addr = addrs.zipWithIndex.map{ case( a,i ) =>
      val aa = Wire(UInt(muxWidth.W))
      aa := a
      // fringe.FringeGlobals.bigIP.multiply(aa, (dims.drop(i).reduce{_.*-*(_,None)}/-/dims(i)).U(muxWidth.W), 0)
      aa.*-*((dims.drop(i).reduce{_*_}/-/dims(i)).U(muxWidth.W), None)
    }.reduce{_+_}
    result := chisel3.util.MuxLookup(flat_addr, 0.U(bitWidth.W), regvals)
    result

    // val result = Wire(UInt(bitWidth.W))
    // val flat = row_addr*width.U + col_addr
    // val bitvec = Vec((0 until dims.reduce{_*_}).map{ i => i.U === flat })
    // for (i <- 0 until dims.reduce{_*_}) {
    //   when(i.U === flat) {
    //     result := io.data_out(i)
    //   }
    // }
    // result

    // // // Sum hack because chisel keeps messing things up
    // val result = Wire(UInt(bitWidth.W))
    // val flat = row_addr*width.U + col_addr
    // result := (0 until width).map { i=> 
    //   (0 until height).map{ j => Mux(j.U === row_addr && i.U === col_addr, io.data_out(i), 0.U) }.reduce{_+_}}.reduce{_+_}
    // result

  }
  
}



// TODO: Currently assumes one write port, possible read port on every buffer
class NBufShiftRegFile(val dims: List[Int], val inits: Option[List[Double]], val stride: Int, val numBufs: Int,
                       val wPar: Map[Int,Int], val bitWidth: Int, val fracBits: Int) extends Module { 

  def this(tuple: (List[Int], Option[List[Double]], Int, Int, Map[Int,Int], Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7)

  val muxWidth = Utils.log2Up(numBufs*dims.reduce{_*_})
  val io = IO(new Bundle { 
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val w = Vec(wPar.values.reduce{_+_}*stride, Input(new multidimRegW(dims.length, dims, bitWidth)))
    val reset    = Input(Bool())
    val data_out = Vec(dims.reduce{_*_}*numBufs, Output(UInt(bitWidth.W)))
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

  val shiftRegs = (0 until numBufs).map{i => Module(new ShiftRegFile(dims, inits, stride, wPar.getOrElse(i,1), isBuf = {i>0}, bitWidth, fracBits))}

  for (i <- 0 until numBufs) {
    for (j <- 0 until dims.reduce{_*_}) {
      io.data_out(i*dims.reduce{_*_} + j) := shiftRegs(i).io.data_out(j)
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
    shiftRegs(i).io.dump_data := shiftRegs(i-1).io.data_out
    if (!wPar.keys.toList.contains(i)) {
      shiftRegs(i).io.w.foreach{p => p.shiftEn := false.B; p.en := false.B; p.data := 0.U}
    }
    shiftRegs(i).io.reset := io.reset
  }

  var wIdMap = (0 until numBufs).map{ i => (i -> 0) }.toMap
  def connectWPort(wBundle: Vec[multidimRegW], ports: List[Int]) {
    assert(ports.length == 1)
    val base = ((0 until ports.head).map{i => wPar.getOrElse(i,0)} :+ 0).reduce{_+_}
    val portbase = wIdMap(ports.head)
    (0 until wBundle.length).foreach{ i => 
      io.w(base+portbase+i) := wBundle(i)
    }
    val newbase = portbase + wBundle.length
    wIdMap += (ports.head -> newbase)
  }

  def connectShiftPort(wBundle: Vec[multidimRegW], ports: List[Int]) {
    assert(ports.length == 1)
    val base = ((0 until ports.head).map{i => wPar.getOrElse(i,0)} :+ 0).reduce{_+_}
    val portbase = wIdMap(ports.head)
    (0 until wBundle.length).foreach{ i => 
      io.w(base+portbase+i) := wBundle(i)
    }
    val newbase = portbase + wBundle.length
    wIdMap += (ports.head -> newbase)
  }

  def readValue(addrs: List[UInt], port: Int): UInt = { // This randomly screws up sometimes, so I don't use it anywhere anymore
    // chisel seems to have broke MuxLookup here...
    val result = Wire(UInt(bitWidth.W))
    val regvals = (0 until numBufs*dims.reduce{_*_}).map{ i => 
      (i.U(muxWidth.W) -> io.data_out(i)) 
    }
    val flat_addr = (port*dims.reduce{_*_}).U(muxWidth.W) + addrs.zipWithIndex.map{ case( a,i ) =>
      val aa = Wire(UInt(muxWidth.W))
      aa := a
      // fringe.FringeGlobals.bigIP.multiply(aa, (dims.drop(i).reduce{_.*-*(_,None)}/-/dims(i)).U(muxWidth.W), 0)
      aa.*-*((dims.drop(i).reduce{_*_}/-/dims(i)).U(muxWidth.W), None)
    }.reduce{_+_}
    result := chisel3.util.MuxLookup(flat_addr, 0.U(bitWidth.W), regvals)
    result

    // val result = Wire(UInt(bitWidth.W))
    // val flat = row_addr*width.U + col_addr
    // val bitvec = Vec((0 until dims.reduce{_*_}).map{ i => i.U === flat })
    // for (i <- 0 until dims.reduce{_*_}) {
    //   when(i.U === flat) {
    //     result := io.data_out(i)
    //   }
    // }
    // result

    // // // Sum hack because chisel keeps messing things up
    // val result = Wire(UInt(bitWidth.W))
    // val flat = row_addr*width.U + col_addr
    // result := (0 until width).map { i=> 
    //   (0 until height).map{ j => Mux(j.U === row_addr && i.U === col_addr, io.data_out(i), 0.U) }.reduce{_+_}}.reduce{_+_}
    // result

  }

  def connectStageCtrl(done: Bool, en: Bool, ports: List[Int]) {
    ports.foreach{ port => 
      io.sEn(port) := en
      io.sDone(port) := done
    }
  }

  
}

class LUT(val dims: List[Int], val inits: List[Double], val numReaders: Int, val width: Int, val fracBits: Int) extends Module {

  def this(tuple: (List[Int], List[Double], Int, Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)
  val muxWidth = Utils.log2Up(dims.reduce{_*_})

  val io = IO(new Bundle { 
    val addr = Vec(numReaders*dims.length, Input(UInt(muxWidth.W)))
    // val en = Vec(numReaders, Input(Bool()))
    val data_out = Vec(numReaders, Output(UInt(width.W)))
  })

  assert(dims.reduce{_*_} == inits.length)
  val options = (0 until dims.reduce{_*_}).map { i => 
    val initval = (inits(i)*scala.math.pow(2,fracBits)).toLong
    // initval.U
    ( i.U(muxWidth.W) -> initval.S((width+1).W).apply(width-1,0) )
  }

  val flat_addr = (0 until numReaders).map{ k => 
    val base = k*dims.length
    (0 until dims.length).map{ i => 
      // (fringe.FringeGlobals.bigIP.multiply(io.addr(i + base), (dims.drop(i).reduce{_.*-*(_,None)}/-/dims(i)).U(muxWidth.W), 0))
      (io.addr(i + base).*-*((dims.drop(i).reduce{_*_}/-/dims(i)).U(muxWidth.W),None))
    }.reduce{_+_}
  }

  // val active_addr = Mux1H(io.en, flat_addr)

  // io.data_out := Mux1H(onehot, options)
  (0 until numReaders).foreach{i =>
    io.data_out(i) := MuxLookup(flat_addr(i), 0.U(width.W), options).asUInt
  }
  // val selected = MuxLookup(active_addr, 0.S, options)

  var rId = 0
  def connectRPort(addrs: List[UInt], en: Bool): Int = {
    (0 until addrs.length).foreach{ i => 
      val base = rId *-* addrs.length
      io.addr(base + i) := addrs(i)
    }
    // io.en(rId) := en
    rId = rId + 1
    rId - 1
  }
  
}
