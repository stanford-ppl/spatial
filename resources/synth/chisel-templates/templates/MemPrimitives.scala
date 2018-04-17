package templates

import util._
import chisel3._
import chisel3.util._
import ops._
import fringe._
import chisel3.util.MuxLookup

import scala.collection.mutable.HashMap

sealed trait BankingMode
object DiagonalMemory extends BankingMode
object BankedMemory extends BankingMode

sealed trait MemPrimitive
object SRAMType extends MemPrimitive
object FFType extends MemPrimitive
object ShiftRegFileType extends MemPrimitive
object LineBufferType extends MemPrimitive

class R_XBar(val ofs_width:Int, val bank_width:List[Int]) extends Bundle {
  val banks = HVec.tabulate(bank_width.length){i => UInt(bank_width(i).W)}
  val ofs = UInt(ofs_width.W)
  val en = Bool()

  override def cloneType = (new R_XBar(ofs_width, bank_width)).asInstanceOf[this.type] // See chisel3 bug 358
}

class W_XBar(val ofs_width:Int, val bank_width:List[Int], val data_width:Int) extends Bundle {
  val banks = HVec.tabulate(bank_width.length){i => UInt(bank_width(i).W)}
  val ofs = UInt(ofs_width.W)
  val data = UInt(data_width.W)
  val reset = Bool() // For FF
  val init = UInt(data_width.W) // For FF
  val shiftEn = Bool() // For ShiftRegFile
  val en = Bool()

  override def cloneType = (new W_XBar(ofs_width, bank_width, data_width)).asInstanceOf[this.type] // See chisel3 bug 358
}

class R_Direct(val ofs_width:Int, val banks:List[Int]) extends Bundle {
  val ofs = UInt(ofs_width.W)
  val en = Bool()

  override def cloneType = (new R_Direct(ofs_width, banks)).asInstanceOf[this.type] // See chisel3 bug 358
}

class W_Direct(val ofs_width:Int, val banks:List[Int], val data_width:Int) extends Bundle {
  val ofs = UInt(ofs_width.W)
  val data = UInt(data_width.W)
  val en = Bool()

  override def cloneType = (new W_Direct(ofs_width, banks, data_width)).asInstanceOf[this.type] // See chisel3 bug 358
}

class Mem1D(val size: Int, bitWidth: Int, syncMem: Boolean = false) extends Module { // Unbanked, inner 1D mem
  def this(size: Int) = this(size, 32)

  val addrWidth = Utils.log2Up(size)

  val io = IO( new Bundle {
    val r = Input(new R_XBar(addrWidth, List(1)))
    val rMask = Input(Bool())
    val w = Input(new W_XBar(addrWidth, List(1), bitWidth))
    val wMask = Input(Bool())
    val flow = Input(Bool())
    val output = new Bundle {
      val data  = Output(UInt(bitWidth.W))
    }
    val debug = new Bundle {
      val invalidRAddr = Output(Bool())
      val invalidWAddr = Output(Bool())
      val rwOn = Output(Bool())
      val error = Output(Bool())
      // val addrProbe = Output(UInt(bitWidth.W))
    }
  })

  // We can do better than MaxJ by forcing mems to be single-ported since
  //   we know how to properly schedule reads and writes
  val wInBound = io.w.ofs < (size).U
  val rInBound = io.r.ofs < (size).U

  if (syncMem) {
    if (size <= Utils.SramThreshold) {
      val m = (0 until size).map{ i =>
        val reg = RegInit(0.U(bitWidth.W))
        reg := Mux(io.w.en & (io.w.ofs === i.U(addrWidth.W)), io.w.data, reg)
        (i.U(addrWidth.W) -> reg)
      }
      val radder = Utils.getRetimed(io.r.ofs,1)
      io.output.data := MuxLookup(radder, 0.U(bitWidth.W), m)
    } else {
      val m = Module(new fringe.SRAM(UInt(bitWidth.W), size, "BRAM"))
      m.io.raddr     := io.r.ofs
      m.io.waddr     := io.w.ofs
      m.io.wen       := io.w.en & wInBound & io.wMask
      m.io.wdata     := io.w.data
      m.io.flow      := io.flow
      io.output.data := m.io.rdata
    }
  } else {
    if (size <= Utils.SramThreshold) {
      val m = (0 until size).map{ i =>
        val reg = RegInit(0.U(bitWidth.W))
        reg := Mux(io.w.en & io.wMask & (io.w.ofs === i.U(addrWidth.W)), io.w.data, reg)
        (i.U(addrWidth.W) -> reg)
      }
      io.output.data := MuxLookup(io.r.ofs, 0.U(bitWidth.W), m)
    } else {
      val m = Mem(size, UInt(bitWidth.W) /*, seqRead = true deprecated? */)
      when (io.w.en & io.wMask & wInBound) {m(io.w.ofs) := io.w.data}
      io.output.data := m(io.r.ofs)
    }
  }

  if (scala.util.Properties.envOrElse("RUNNING_REGRESSION", "0") == "1") {
    io.debug.invalidRAddr := ~rInBound
    io.debug.invalidWAddr := ~wInBound
    io.debug.rwOn := io.w.en & io.r.en & io.wMask & io.rMask
    io.debug.error := ~rInBound | ~wInBound | (io.w.en & io.r.en & io.wMask & io.rMask)
    // io.debug.addrProbe := m(0.U)
  }

}

class SRAM(val logicalDims: List[Int], val bitWidth: Int, 
           val banks: List[Int], val strides: List[Int], 
           val xBarWMux: HashMap[Int, Int], val xBarRMux: HashMap[Int, Int], // muxPort -> accessPar
           val directWMux: HashMap[Int, List[List[Int]]], val directRMux: HashMap[Int, List[List[Int]]],  // muxPort -> List(banks, banks, ...)
           val bankingMode: BankingMode, val inits: Option[List[Double]] = None, val syncMem: Boolean = false, val fracBits: Int = 0) extends Module { 

  // Overloaded construters
  // Tuple unpacker
  def this(tuple: (List[Int], Int, List[Int], List[Int], HashMap[Int, Int], HashMap[Int, Int], 
    HashMap[Int, List[List[Int]]], HashMap[Int, List[List[Int]]], BankingMode)) = this(tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7, tuple._8, tuple._9)

  val depth = logicalDims.product // Size of memory
  val N = logicalDims.length // Number of dimensions
  val ofsWidth = Utils.log2Up(depth/banks.product)
  val banksWidths = banks.map(Utils.log2Up(_))

  // Compute info required to set up IO interface
  val hasXBarW = xBarWMux.values.sum > 0
  val hasXBarR = xBarRMux.values.sum > 0
  val numXBarW = if (hasXBarW) xBarWMux.values.sum else 1
  val numXBarR = if (hasXBarR) xBarRMux.values.sum else 1
  val hasDirectW = directWMux.values.flatten.toList.length > 0
  val hasDirectR = directRMux.values.flatten.toList.length > 0
  val numDirectW = if (hasDirectW) directWMux.values.flatten.toList.length else 1
  val numDirectR = if (hasDirectR) directRMux.values.flatten.toList.length else 1
  val totalOutputs = {if (hasXBarR) xBarRMux.values.max else 0} max {if (hasDirectR) directRMux.values.map(_.length).max else 0}
  val defaultDirect = List.fill(banks.length)(99)

  val io = IO( new Bundle {
    val xBarW = Vec(numXBarW, Input(new W_XBar(ofsWidth, banksWidths, bitWidth)))
    val xBarR = Vec(numXBarR, Input(new R_XBar(ofsWidth, banksWidths))) 
    val directW = HVec(Array.tabulate(numDirectW){i => Input(new W_Direct(ofsWidth, if (hasDirectW) directWMux.toSeq.sortBy(_._1).toMap.values.flatten.toList(i) else defaultDirect, bitWidth))})
    val directR = HVec(Array.tabulate(numDirectR){i => Input(new R_Direct(ofsWidth, if (hasDirectR) directRMux.toSeq.sortBy(_._1).toMap.values.flatten.toList(i) else defaultDirect))})
    val flow = Vec(xBarRMux.values.sum + directRMux.values.flatten.toList.length, Input(Bool()))
    val output = new Bundle {
      val data  = Vec(totalOutputs, Output(UInt(bitWidth.W)))
    }
  })

  // Get info on physical dims
  // TODO: Upcast dims to evenly bank
  val bankDim = bankingMode match {
    // case DiagonalMemory => logicalDims.zipWithIndex.map { case (dim, i) => if (i == N - 1) math.ceil(dim.toDouble/banks.head).toInt else dim}
    case BankedMemory => math.ceil(depth / banks.product).toInt
  }
  val numMems = bankingMode match {
    case DiagonalMemory => banks.head
    case BankedMemory => banks.product
  }

  // Create list of (mem: Mem1D, coords: List[Int] <coordinates of bank>)
  val m = (0 until numMems).map{ i => 
    val mem = Module(new Mem1D(bankDim, bitWidth, syncMem))
    val coords = banks.zipWithIndex.map{ case (b,j) => 
      i % (banks.drop(j).product) / banks.drop(j+1).product
    }
    (mem,coords)
  }

  // Handle Writes
  m.foreach{ mem => 
    // Check all xBar w ports against this bank's coords
    val xBarSelect = io.xBarW.map(_.banks).zip(io.xBarW.map(_.en)).map{ case(bids, en) => 
      bids.zip(mem._2).map{case (b,coord) => b === coord.U}.reduce{_&&_} & {if (hasXBarW) en else false.B}
    }
    // Check all direct W ports against this bank's coords
    val directSelect = io.directW.filter(_.banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_))

    // Unmask write port if any of the above match
    mem._1.io.wMask := xBarSelect.reduce{_|_} | {if (hasDirectW) directSelect.map(_.en).reduce(_|_) else false.B}
    // Connect matching W port to memory
    mem._1.io.w.ofs := Mux(if (hasDirectW) directSelect.map(_.en).reduce(_|_) else false.B, chisel3.util.PriorityMux(if (hasDirectW) directSelect.map(_.en) else List(false.B), directSelect).ofs, chisel3.util.PriorityMux(if (hasXBarW) xBarSelect else List(false.B), io.xBarW).ofs)
    mem._1.io.w.data := Mux(if (hasDirectW) directSelect.map(_.en).reduce(_|_) else false.B, chisel3.util.PriorityMux(if (hasDirectW) directSelect.map(_.en) else List(false.B), directSelect).data, chisel3.util.PriorityMux(if (hasXBarW) xBarSelect else List(false.B), io.xBarW).data)
    mem._1.io.w.en := Mux(if (hasDirectW) directSelect.map(_.en).reduce(_|_) else false.B, chisel3.util.PriorityMux(if (hasDirectW) directSelect.map(_.en) else List(false.B), directSelect).en, chisel3.util.PriorityMux(if (hasXBarW) xBarSelect else List(false.B), io.xBarW).en)
  }

  // Handle Reads
  m.foreach{ mem => 
    // Check all xBar r ports against this bank's coords
    val xBarSelect = io.xBarR.map(_.banks).zip(io.xBarR.map(_.en)).map{ case(bids, en) => 
      bids.zip(mem._2).map{case (b,coord) => b === coord.U}.reduce{_&&_} & en 
    }
    // Check all direct r ports against this bank's coords
    val directSelect = io.directR.filter(_.banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_))

    // Unmask write port if any of the above match
    mem._1.io.rMask := xBarSelect.reduce{_|_} & directSelect.map(_.en).reduce(_|_)
    // Connect matching R port to memory
    mem._1.io.r.ofs := Mux(if (hasDirectR) directSelect.map(_.en).reduce(_|_) else false.B, chisel3.util.PriorityMux(if (hasDirectR) directSelect.map(_.en) else List(false.B), directSelect).ofs, chisel3.util.PriorityMux(if (hasXBarR) xBarSelect else List(false.B), io.xBarR).ofs)
    mem._1.io.r.en := Mux(if (hasDirectR) directSelect.map(_.en).reduce(_|_) else false.B, chisel3.util.PriorityMux(if (hasDirectR) directSelect.map(_.en) else List(false.B), directSelect).en, chisel3.util.PriorityMux(if (hasXBarR) xBarSelect else List(false.B), io.xBarR).en)

    mem._1.io.flow := io.flow.reduce{_&_} // TODO: Dangerous but probably works
  }

  // Connect read data to output
  io.output.data.zipWithIndex.foreach { case (wire,i) => 
    // Figure out which read port was active in xBar
    val xBarIds = xBarRMux.toSeq.sortBy(_._1).toMap.values.zipWithIndex.map{case(x,ii) => xBarRMux.toSeq.sortBy(_._1).toMap.values.take(ii).sum + i }
    val xBarCandidates = xBarIds.map(io.xBarR(_))
    // Figure out which read port was active in direct
    val directIds = directRMux.toSeq.sortBy(_._1).toMap.values.zipWithIndex.map{case(x,ii) => directRMux.toSeq.sortBy(_._1).toMap.values.take(ii).toList.flatten.length + i }
    val directCandidates = directIds.map(io.directR(_))
    // Create bit vector to select which bank was activated by this i
    val sel = m.map{ mem => 
      val xBarWants = if (hasXBarR) xBarCandidates.map {x => 
        x.banks.zip(mem._2).map{case (b, coord) => Utils.getRetimed(b, Utils.sramload_latency) === coord.U}.reduce{_&&_} && x.en
      }.reduce{_||_} else false.B
      val directWants = if (hasDirectR) directCandidates.map {x => 
        x.banks.zip(mem._2).map{case (b, coord) => b == coord}.reduce{_&&_}.B && x.en
      }.reduce{_||_} else false.B
      xBarWants || directWants
    }
    val datas = m.map{ _._1.io.output.data }
    val d = chisel3.util.PriorityMux(sel, datas)
    wire := d
  }

  def connectXBarWPort(wBundle: W_XBar, muxPort: Int, vecId: Int) {
    val base = xBarWMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.sum + vecId
    io.xBarW(base) := wBundle
  }

  def connectXBarRPort(rBundle: R_XBar, muxPort: Int, vecId: Int): UInt = {connectXBarRPort(rBundle, muxPort, vecId, true.B)}

  def connectXBarRPort(rBundle: R_XBar, muxPort: Int, vecId: Int, flow: Bool): UInt = {
    val base = xBarRMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.sum + vecId
    io.xBarR(base) := rBundle    
    io.flow(base) := flow
    io.output.data(vecId)
  }

  def connectDirectWPort(wBundle: W_Direct, muxPort: Int, vecId: Int) {
    val base = directWMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.flatten.toList.length + vecId
    io.directW(base) := wBundle
  }

  def connectDirectRPort(rBundle: R_Direct, muxPort: Int, vecId: Int): UInt = {connectDirectRPort(rBundle, muxPort, vecId, true.B)}

  def connectDirectRPort(rBundle: R_Direct, muxPort: Int, vecId: Int, flow: Bool): UInt = {
    val base = directRMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.flatten.toList.length + vecId
    io.directR(base) := rBundle    
    io.flow(base) := flow
    io.output.data(vecId)
  }

}



class FF(val bitWidth: Int,
         val xBarWMux: HashMap[Int, Int] = HashMap(0 -> 1), // muxPort -> 1 bookkeeping
         val init: Option[List[Double]] = None,
         val fracBits: Int = 0
        ) extends Module {
  def this(tuple: (Int, HashMap[Int, Int])) = this(tuple._1,tuple._2,None,0)
  // Compatibility with standard mem codegen
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: List[Int], strides: List[Int], 
           xBarWMux: HashMap[Int, Int], xBarRMux: HashMap[Int, Int], // muxPort -> accessPar
           directWMux: HashMap[Int, List[List[Int]]], directRMux: HashMap[Int, List[List[Int]]],  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int) = this(bitWidth, xBarWMux, init, fracBits)

  val io = IO(new Bundle{
    val input = Vec(xBarWMux.toList.length, Input(new W_XBar(1, List(1), bitWidth)))
    val output = new Bundle {
      val data  = Output(UInt(bitWidth.W))
    }
  })

  val ff = if (init.isDefined) RegInit((init.get.head*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W)) else RegInit(io.input(0).init)
  val anyReset = io.input.map{_.reset}.reduce{_|_}
  val anyEnable = io.input.map{_.en}.reduce{_|_}
  val wr_data = chisel3.util.Mux1H(io.input.map{_.en}, io.input.map{_.data})
  ff := Mux(anyReset, io.input(0).init, Mux(anyEnable, wr_data, ff))
  io.output.data := Mux(anyReset, io.input(0).init, ff)

  def connectXBarWPort(wBundle: W_XBar, muxPort: Int, vecId: Int) {
    val base = xBarWMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.sum + vecId
    io.input(base) := wBundle
  }

  def connectXBarRPort(rBundle: R_XBar, muxPort: Int, vecId: Int): UInt = {io.output.data}

}

class FIFO(val logicalDims: List[Int], val bitWidth: Int, 
           val banks: List[Int], 
           val xBarWMux: HashMap[Int, Int], val xBarRMux: HashMap[Int, Int],
           val inits: Option[List[Double]] = None, val syncMem: Boolean = false, val fracBits: Int = 0) extends Module {

  def this(tuple: (List[Int], Int, List[Int], HashMap[Int, Int], HashMap[Int, Int])) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)

  val depth = logicalDims.product // Size of memory
  val N = logicalDims.length // Number of dimensions
  val ofsWidth = Utils.log2Up(depth/banks.product) + 2
  val elsWidth = Utils.log2Up(depth) + 2
  val banksWidths = banks.map(Utils.log2Up(_))

  // Compute info required to set up IO interface
  val hasXBarW = xBarWMux.values.sum > 0
  val hasXBarR = xBarRMux.values.sum > 0
  val numXBarW = if (hasXBarW) xBarWMux.values.sum else 0
  val numXBarR = if (hasXBarR) xBarRMux.values.sum else 0
  val totalOutputs = numXBarR
  val defaultDirect = List.fill(banks.length)(99)

  val io = IO( new Bundle {
    val xBarW = Vec(1 max numXBarW, Input(new W_XBar(ofsWidth, banksWidths, bitWidth)))
    val xBarR = Vec(1 max numXBarR, Input(new R_XBar(ofsWidth, banksWidths))) 
    val flow = Vec(xBarRMux.values.sum, Input(Bool()))
    val output = new Bundle {
      val data  = Vec(totalOutputs, Output(UInt(bitWidth.W)))
    }
    val full = Output(Bool())
    val almostFull = Output(Bool())
    val empty = Output(Bool())
    val almostEmpty = Output(Bool())
    val numel = Output(UInt(32.W))
  })

  // Create bank counters
  val headCtr = Module(new CompactingCounter(numXBarW, depth, elsWidth))
  val tailCtr = Module(new CompactingCounter(numXBarR, depth, elsWidth))
  (0 until numXBarW).foreach{i => headCtr.io.input.enables(i) := io.xBarW(i).en}
  (0 until numXBarR).foreach{i => tailCtr.io.input.enables(i) := io.xBarR(i).en}
  headCtr.io.input.reset := reset
  tailCtr.io.input.reset := reset
  headCtr.io.input.dir := true.B
  tailCtr.io.input.dir := true.B

  // Create numel counter
  val elements = Module(new CompactingIncDincCtr(numXBarW, numXBarR, depth, elsWidth))
  (0 until numXBarW).foreach{i => elements.io.input.inc_en(i)  := io.xBarW(i).en}
  (0 until numXBarR).foreach{i => elements.io.input.dinc_en(i) := io.xBarR(i).en}

  // Create physical mems
  val numBanks = banks.product
  val m = (0 until numBanks).map{ i => Module(new Mem1D(depth/numBanks, bitWidth))}

  // Create compacting network
  val enqCompactor = Module(new CompactingEnqNetwork(xBarWMux.toSeq.sortBy(_._1).toMap.values.toList, numBanks, ofsWidth, bitWidth))
  enqCompactor.io.headCnt := headCtr.io.output.count
  (0 until numXBarW).foreach{i => enqCompactor.io.in(i).data := io.xBarW(i).data; enqCompactor.io.in(i).en := io.xBarW(i).en}

  // Connect compacting network to banks
  val active_w_bank = Utils.singleCycleModulo(headCtr.io.output.count, numBanks.S(elsWidth.W))
  val active_w_addr = Utils.singleCycleDivide(headCtr.io.output.count, numBanks.S(elsWidth.W))
  (0 until numBanks).foreach{i => 
    val addr = Mux(i.S(elsWidth.W) < active_w_bank, active_w_addr + 1.S(elsWidth.W), active_w_addr)
    m(i).io.w.ofs := addr.asUInt
    m(i).io.w.data := enqCompactor.io.out(i).data
    m(i).io.w.en   := enqCompactor.io.out(i).en
    m(i).io.wMask  := enqCompactor.io.out(i).en
  }

  // Create dequeue compacting network
  val deqCompactor = Module(new CompactingDeqNetwork(xBarRMux.toSeq.sortBy(_._1).toMap.values.toList, numBanks, elsWidth, bitWidth))
  deqCompactor.io.tailCnt := tailCtr.io.output.count
  val active_r_bank = Utils.singleCycleModulo(tailCtr.io.output.count, numBanks.S(elsWidth.W))
  val active_r_addr = Utils.singleCycleDivide(tailCtr.io.output.count, numBanks.S(elsWidth.W))
  (0 until numBanks).foreach{i => 
    val addr = Mux(i.S(elsWidth.W) < active_r_bank, active_r_addr + 1.S(elsWidth.W), active_r_addr)
    m(i).io.r.ofs := addr.asUInt
    deqCompactor.io.input.data(i) := m(i).io.output.data
  }
  (0 until numXBarR).foreach{i =>
    deqCompactor.io.input.deq(i) := io.xBarR(i).en
  }
  (0 until xBarRMux.values.max).foreach{i =>
    io.output.data(i) := deqCompactor.io.output.data(i)
  }

  // Check if there is data
  io.empty := elements.io.output.empty
  io.full := elements.io.output.full
  io.almostEmpty := elements.io.output.almostEmpty
  io.almostFull := elements.io.output.almostFull
  io.numel := elements.io.output.numel.asUInt

  def connectXBarWPort(wBundle: W_XBar, muxPort: Int, vecId: Int) {
    val base = xBarWMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.sum + vecId
    io.xBarW(base) := wBundle
  }

  def connectXBarRPort(rBundle: R_XBar, muxPort: Int, vecId: Int): UInt = {connectXBarRPort(rBundle, muxPort, vecId, true.B)}

  def connectXBarRPort(rBundle: R_XBar, muxPort: Int, vecId: Int, flow: Bool): UInt = {
    val base = xBarRMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.sum + vecId
    io.xBarR(base) := rBundle    
    io.flow(base) := flow
    io.output.data(vecId)
  }



}


// class ShiftRegFile(val logicalDims: List[Int], val bitWidth: Int, 
//                    val banks: List[Int], val bankDepth: Int, val inits: Option[Map[List[Int], Double]], val stride: Int, 
//                    val xBarWMux: HashMap[Int, Int], val xBarRMux: HashMap[Int, Int], // muxPort -> accessPar
//                    val isBuf: Boolean, val fracBits: Int) extends Module {

//   def this(tuple: (List[Int], Int, List[Int], Int, Option[Map[List[Int], Double]], HashMap[Int, Int], HashMap[Int, Int], Boolean, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9, tuple._10)


//   /* FROM SRAM 
//   val depth = logicalDims.product // Size of memory
//   val N = logicalDims.length // Number of dimensions
//   val ofsWidth = Utils.log2Up(depth/banks.product)
//   val banksWidths = banks.map(Utils.log2Up(_))

//   // Compute info required to set up IO interface
//   val hasXBarW = xBarWMux.values.sum > 0
//   val hasXBarR = xBarRMux.values.sum > 0
//   val numXBarW = if (hasXBarW) xBarWMux.values.sum else 1
//   val numXBarR = if (hasXBarR) xBarRMux.values.sum else 1
//   val hasDirectW = directWMux.values.flatten.toList.length > 0
//   val hasDirectR = directRMux.values.flatten.toList.length > 0
//   val numDirectW = if (hasDirectW) directWMux.values.flatten.toList.length else 1
//   val numDirectR = if (hasDirectR) directRMux.values.flatten.toList.length else 1
//   val totalOutputs = {if (hasXBarR) xBarRMux.values.max else 0} max {if (hasDirectR) directRMux.values.map(_.length).max else 0}
//   val defaultDirect = List.fill(banks.length)(99)

//   val io = IO( new Bundle {
//     val xBarW = Vec(numXBarW, Input(new W_XBar(ofsWidth, banksWidths, bitWidth)))
//     val xBarR = Vec(numXBarR, Input(new R_XBar(ofsWidth, banksWidths))) 
//     val directW = HVec(Array.tabulate(numDirectW){i => Input(new W_Direct(ofsWidth, if (hasDirectW) directWMux.toSeq.sortBy(_._1).toMap.values.flatten.toList(i) else defaultDirect, bitWidth))})
//     val directR = HVec(Array.tabulate(numDirectR){i => Input(new R_Direct(ofsWidth, if (hasDirectR) directRMux.toSeq.sortBy(_._1).toMap.values.flatten.toList(i) else defaultDirect))})
//     val flow = Vec(xBarRMux.values.sum + directRMux.values.flatten.toList.length, Input(Bool()))
//     val output = new Bundle {
//       val data  = Vec(totalOutputs, Output(UInt(bitWidth.W)))
//     }
//   })
//   */

//   val muxWidth = Utils.log2Up(dims.reduce{_*_})
//   val portWidth = banks.length+1
//   val numMems = banks.product * bankDepth
//   assert(numMems == dims.product)

//   // Console.println(s"dims are $dims, banks $banks $bankDepth, num mmems $numMems, wparstride $wPar * $stride, readers $numReaders")

//   // Console.println(" " + dims.reduce{_*_} + " " + wPar + " " + dims.length)
//   val io = IO(new Bundle { 
//     // Signals for dumping data from one buffer to next
//     val dump_out = Vec(numMems, Output(UInt(bitWidth.W)))
//     val dump_data = Vec(numMems, Input(UInt(bitWidth.W)))
//     val dump_en = Input(Bool())

//     // Data connections
//     val xBarW = Vec(1 max (wPar * stride), Input(new RegW_Info(32, List.fill(banks.length)(32), bitWidth)))
//     val xBarR = Vec(1 max numReaders, Input(new RegR_Info(32, List.fill(banks.length)(32)))) 

//     val reset    = Input(Bool())
//     val data_out = Vec(1 max numReaders, Output(UInt(bitWidth.W)))

//   })

//   val registers = (0 until numMems).map{ i => 
//     val coords = (banks :+ bankDepth).zipWithIndex.map{ case (b,j) => 
//       i % ((banks :+ bankDepth).drop(j).product) / (banks :+ bankDepth).drop(j+1).product
//     }

//     val initval = if (inits.isDefined) (inits.get.apply(coords)*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W) else 0.U(bitWidth.W)
//     val mem = RegInit(initval)
//     io.dump_out(i) := mem
//     (mem,coords,i)
//   }


//   (0 until numReaders).map{ j => 
//     val bitmask = registers.map{mem => (0 until banks.length).map{k => io.r(j).banks(k) === mem._2(k).U}.reduce{_&&_} && io.r(j).ofs === mem._2.last.U}
//     io.data_out(j) := Mux1H(bitmask, registers.map(_._1))
//   }

//   if (wPar > 0) { // If it is not >0, then this should just be a pass-through in an nbuf
//     // Connect a w port to each reg
//     (numMems-1 to 0 by -1).foreach { i => 
//       // Construct n-D coords
//       val coords = registers(i)._2
//       when(io.reset) {
//         if (inits.isDefined) {
//           registers(i)._1 := (inits.get.apply(coords)*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W)
//         } else {
//           registers(i)._1 := 0.U(bitWidth.W)            
//         }
//       }.elsewhen(io.dump_en) {
//         registers(i)._1 := io.dump_data(i)
//       }.otherwise {
//         if (wPar * stride > 1) {
//           // Address flattening
//           val w_addrs_match = (0 until wPar*stride).map{ wnum => (0 until portWidth - 1).map{j => io.w(wnum).banks(j) === coords(j).U(32.W)}.reduce{_&&_} && io.w(wnum).ofs === coords.last.U(32.W)}

//           val write_here = (0 until wPar * stride).map{ wnum => io.w(wnum).en & w_addrs_match(wnum) }
//           val shift_entry_here =  (0 until wPar * stride).map{ wnum => io.w(wnum).shiftEn & w_addrs_match(wnum) }
//           val write_data = Mux1H(write_here.zip(shift_entry_here).map{case (a,b) => a|b}, io.w)
//           // val shift_data = Mux1H(shift_entry_here, io.w)
//           val has_writer = write_here.reduce{_|_}
//           val has_shifter = shift_entry_here.reduce{_|_}

//           // Assume no bozos will shift mid-axis
//           val shift_axis = (0 until wPar * stride).map{ wnum => io.w(wnum).shiftEn & {if (dims.length > 1) {(coords.last >= stride).B & io.w(wnum).banks.zip(coords.dropRight(1)).map{case(a,b) => a === b.U(32.W)}.reduce{_&_}} else {(coords.last >= stride).B} }}.reduce{_|_}
//           val producing_reg = coords.dropRight(1) :+ (0 max (coords.last - stride))
//           // Console.println(s"coords $coords receives shift from ${producing_reg}")
//           registers(i)._1 := Mux(shift_axis, registers.filter(_._2 == producing_reg).head._1, Mux(has_writer | has_shifter, write_data.data, registers(i)._1))
//         } else {
//           // Address flattening
//           val w_addr_match = (0 until portWidth - 1).map{j => io.w(0).banks(j) === coords(j).U(32.W)}.reduce{_&&_} && io.w(0).ofs === coords.last.U(32.W)

//           val write_here = io.w(0).en & w_addr_match
//           val shift_entry_here =  io.w(0).shiftEn & w_addr_match
//           val write_data = io.w(0).data
//           // val shift_data = Mux1H(shift_entry_here, io.w)
//           val has_writer = write_here
//           val has_shifter = shift_entry_here

//           // Assume no bozos will shift mid-axis
//           val shift_axis = io.w(0).shiftEn & {if (dims.length > 1) {(coords.last >= stride).B & io.w(0).banks.zip(coords.dropRight(1)).map{case(a,b) => a === b.U}.reduce{_&_} } else {(coords.last >= stride).B} }
//           val producing_reg = coords.dropRight(1) :+ (0 max (coords.last - stride))
//           registers(i)._1 := Mux(shift_axis, registers.filter(_._2 == producing_reg).head._1, Mux(has_writer | has_shifter, write_data.data, registers(i)._1))
//         }
//       }
//     }
//   } else {
//     when(io.reset) {
//       for (i <- 0 until numMems) {
//         val coords = registers(i)._2
//         if (inits.isDefined) {
//           registers(i)._1 := (inits.get.apply(coords)*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W)
//         } else {
//           registers(i)._1 := 0.U(bitWidth.W)            
//         }
//       }
//     }.elsewhen(io.dump_en) {
//       for (i <- 0 until dims.reduce{_*_}) {
//         registers(i)._1 := io.dump_data(i)
//       }
//     }.otherwise{
//       for (i <- 0 until dims.reduce{_*_}) {
//         registers(i)._1 := registers(i)._1
//       }      
//     }
//   }




//   var wId = 0
//   def connectWPort(wBundle: Vec[RegW_Info], ports: List[Int]) {
//     assert(ports.head == 0)
//     (0 until wBundle.length).foreach{ i => 
//       io.w(wId+i) := wBundle(i)
//     }
//     wId += wBundle.length
//   }

//   def connectShiftPort(wBundle: Vec[RegW_Info], ports: List[Int]) {
//     assert(ports.head == 0)
//     (0 until wBundle.length).foreach{ i => 
//       io.w(wId+i) := wBundle(i)
//     }
//     wId += wBundle.length
//   }

//   var rId = 0
//   def connectRPort(addrs: RegR_Info, port: Int): Int = {
//     io.r(rId) := addrs
//     rId = rId + 1
//     rId - 1
//   }
  
// }


// To be deprecated...

class SRAM_Old(val logicalDims: List[Int], val bitWidth: Int, 
           val banks: List[Int], val strides: List[Int], 
           val wPar: List[Int], val rPar: List[Int], val bankingMode: BankingMode, val syncMem: Boolean = false) extends Module { 

  // Overloaded construters
  // Tuple unpacker
  def this(tuple: (List[Int], Int, List[Int], List[Int], 
           List[Int], List[Int], BankingMode)) = this(tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7)
  // Bankmode-less
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: List[Int], strides: List[Int], 
           wPar: List[Int], rPar: List[Int]) = this(logicalDims, bitWidth, banks, strides, wPar, rPar, BankedMemory)
  // If 1D, spatial will make banks and strides scalars instead of lists
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: Int, strides: Int, 
           wPar: List[Int], rPar: List[Int]) = this(logicalDims, bitWidth, List(banks), List(strides), wPar, rPar, BankedMemory)

  val depth = logicalDims.reduce{_*_} // Size of memory
  val N = logicalDims.length // Number of dimensions
  val addrWidth = logicalDims.map{Utils.log2Up(_)}.max

  val io = IO( new Bundle {
    // TODO: w bundle gets forcefully generated as output in verilog
    //       so the only way to make it an input seems to flatten the
    //       Vec(numWriters, Vec(wPar, _)) to a 1D vector and then reconstruct it
    val w = Vec(wPar.reduce{_+_}, Input(new multidimW(N, logicalDims, bitWidth)))
    val r = Vec(rPar.reduce{_+_},Input(new multidimR(N, logicalDims, bitWidth))) // TODO: Spatial allows only one reader per mem
    val flow = Vec(rPar.length, Input(Bool()))
    val output = new Bundle {
      val data  = Vec(rPar.reduce{_+_}, Output(UInt(bitWidth.W)))
    }
    val debug = new Bundle {
      val invalidRAddr = Output(Bool())
      val invalidWAddr = Output(Bool())
      val rwOn = Output(Bool())
      val readCollision = Output(Bool())
      val writeCollision = Output(Bool())
      val error = Output(Bool())
    }
  })

  // Get info on physical dims
  // TODO: Upcast dims to evenly bank
  val physicalDims = bankingMode match {
    case DiagonalMemory => logicalDims.zipWithIndex.map { case (dim, i) => if (i == N - 1) math.ceil(dim.toDouble/banks.head).toInt else dim}
    case BankedMemory => logicalDims.zip(banks).map { case (dim, b) => math.ceil(dim.toDouble/b).toInt}
  }
  val numMems = bankingMode match {
    case DiagonalMemory => banks.head
    case BankedMemory => banks.reduce{_*_}
  }

  // Create physical mems
  val m = (0 until numMems).map{ i => Module(new MemND_Old(physicalDims, bitWidth, syncMem))}

  // Reconstruct io.w as 2d vector


  // TODO: Should connect multidimW's directly to their banks rather than all-to-all connections
  // Convert selectedWVec to translated physical addresses
  val wConversions = io.w.map{ wbundle => 
    // Writer conversion
    val convertedW = Wire(new multidimW(N,logicalDims,bitWidth))
    val physicalAddrs = bankingMode match {
      case DiagonalMemory => wbundle.addr.zipWithIndex.map {case (logical, i) => if (i == N - 1) logical./-/(banks.head.U,None) else logical}
      case BankedMemory => wbundle.addr.zip(banks).map{ case (logical, b) => logical./-/(b.U,None) }
    }
    physicalAddrs.zipWithIndex.foreach { case (calculatedAddr, i) => convertedW.addr(i) := calculatedAddr}
    convertedW.data := wbundle.data
    convertedW.en := wbundle.en
    val flatBankId = bankingMode match {
      case DiagonalMemory => wbundle.addr.reduce{_+_}.%-%(banks.head.U, None)
      case BankedMemory => 
        val bankCoords = wbundle.addr.zip(banks).map{ case (logical, b) => logical.%-%(b.U,None) }
       bankCoords.zipWithIndex.map{ case (c, i) => c.*-*((banks.drop(i).reduce{_*_}/banks(i)).U,None) }.reduce{_+_}
        // bankCoords.zipWithIndex.map{ case (c, i) => FringeGlobals.bigIP.multiply(c, (banks.drop(i).reduce{_.*-*(_,None)}/-/banks(i)).U, 0) }.reduce{_+_}
    }

    (convertedW, flatBankId)
  }
  val convertedWVec = wConversions.map{_._1}
  val bankIdW = wConversions.map{_._2}

  val rConversions = io.r.map{ rbundle => 
    // Reader conversion
    val convertedR = Wire(new multidimR(N,logicalDims,bitWidth))
    val physicalAddrs = bankingMode match {
      case DiagonalMemory => rbundle.addr.zipWithIndex.map {case (logical, i) => if (i == N - 1) logical./-/(banks.head.U,None) else logical}
      case BankedMemory => rbundle.addr.zip(banks).map{ case (logical, b) => logical./-/(b.U,None) }
    }
    physicalAddrs.zipWithIndex.foreach { case (calculatedAddr, i) => convertedR.addr(i) := calculatedAddr}
    convertedR.en := rbundle.en
    val syncDelay = 0//if (syncMem) 1 else 0
    val flatBankId = bankingMode match {
      case DiagonalMemory => Utils.getRetimed(rbundle.addr.reduce{_+_}, syncDelay).%-%(banks.head.U, None)
      case BankedMemory => 
        val bankCoords = rbundle.addr.zip(banks).map{ case (logical, b) => Utils.getRetimed(logical, syncDelay).%-%(b.U,None) }
       bankCoords.zipWithIndex.map{ case (c, i) => c.*-*((banks.drop(i).reduce{_*_}/banks(i)).U,None) }.reduce{_+_}
        // bankCoords.zipWithIndex.map{ case (c, i) => FringeGlobals.bigIP.multiply(c, (banks.drop(i).reduce{_.*-*(_,None)}/-/banks(i)).U, 0) }.reduce{_+_}
    }
    (convertedR, flatBankId)
  }
  val convertedRVec = rConversions.map{_._1}
  val bankIdR = rConversions.map{_._2}

  // TODO: Doing inefficient thing here of all-to-all connection between bundlesNDs and MemNDs
  // Convert bankCoords for each bundle to a bit vector
  // TODO: Probably need to have a dummy multidimW port to default to for unused banks so we don't overwrite anything
  m.zipWithIndex.foreach{ case (mem, i) => 
    val bundleSelect = bankIdW.zip(convertedWVec).map{ case(bid, wvec) => bid === i.U & wvec.en }
    mem.io.wMask := bundleSelect.reduce{_|_}
    mem.io.w := chisel3.util.PriorityMux(bundleSelect, convertedWVec)
  }

  // TODO: Doing inefficient thing here of all-to-all connection between bundlesNDs and MemNDs
  // Convert bankCoords for each bundle to a bit vector
  m.zipWithIndex.foreach{ case (mem, i) => 
    val bundleSelect = bankIdR.zip(convertedRVec).map{ case(bid, rvec) => (bid === i.U) & rvec.en }
    mem.io.rMask := bundleSelect.reduce{_|_}
    mem.io.r := chisel3.util.PriorityMux(bundleSelect, convertedRVec)
    mem.io.flow := io.flow.reduce{_&_} // TODO: Dangerous but probably works
  }

  // Connect read data to output
  io.output.data.zip(bankIdR).foreach { case (wire, id) => 
    val sel = (0 until numMems).map{ i => (Utils.getRetimed(id, Utils.sramload_latency) === i.U)}
    val datas = m.map{ _.io.output.data }
    val d = chisel3.util.PriorityMux(sel, datas)
    wire := d
  }

  var wInUse = Array.fill(wPar.length) {false} // Array for tracking which wPar sections are in use
  def connectWPort(wBundle: Vec[multidimW], ports: List[Int]) {
    // Figure out which wPar section this wBundle fits in by finding first false index with same wPar
    val potentialFits = wPar.zipWithIndex.filter(_._1 == wBundle.length).map(_._2)
    val wId = potentialFits(potentialFits.map(wInUse(_)).indexWhere(_ == false))
    val port = ports(0) // Should never have more than 1 for SRAM
    // Get start index of this section
    val base = if (wId > 0) {wPar.take(wId).reduce{_+_}} else 0
    // Connect to wPar(wId) elements from base
    (0 until wBundle.length).foreach{ i => 
      io.w(base + i) := wBundle(i) 
    }
    // Set this section in use
    wInUse(wId) = true
  }

  var rId = 0
  var flowId = 0
  def connectRPort(rBundle: Vec[multidimR], port: Int): Int = {
    // Get start index of this section
    val base = rId
    // Connect to rPar(rId) elements from base
    (0 until rBundle.length).foreach{ i => 
      io.r(base + i) := rBundle(i) 
    }
    io.flow(flowId) := true.B
    flowId = flowId + 1
    rId = rId + rBundle.length
    base
  }

  def connectRPort(rBundle: Vec[multidimR], port: Int, flow: Bool): Int = {
    // Get start index of this section
    val base = rId
    // Connect to rPar(rId) elements from base
    (0 until rBundle.length).foreach{ i => 
      io.r(base + i) := rBundle(i) 
    }
    io.flow(flowId) := flow
    flowId = flowId + 1
    rId = rId + rBundle.length
    base
  }

  if (scala.util.Properties.envOrElse("RUNNING_REGRESSION", "0") == "1") { // Major hack until someone helps me include the sv file in Driver (https://groups.google.com/forum/#!topic/chisel-users/_wawG_guQgE)
    // Connect debug signals
    val wInBound = io.w.map{ v => v.addr.zip(logicalDims).map { case (addr, bound) => addr < bound.U }.reduce{_&_}}.reduce{_&_}
    val rInBound = io.r.map{ v => v.addr.zip(logicalDims).map { case (addr, bound) => addr < bound.U }.reduce{_&_}}.reduce{_&_}
    val writeOn = io.w.map{ v => v.en }
    val readOn = io.r.map{ v => v.en }
    val rwOn = writeOn.zip(readOn).map{ case(a,b) => a&b}.reduce{_|_}
    val rCollide = bankIdR.zip( readOn).map{ case(id1,en1) => bankIdR.zip( readOn).map{ case(id2,en2) => Mux((id1 === id2) & en1 & en2, 1.U, 0.U)}.reduce{_+_} }.reduce{_+_} !=  readOn.map{Mux(_, 1.U, 0.U)}.reduce{_+_}
    val wCollide = bankIdW.zip(writeOn).map{ case(id1,en1) => bankIdW.zip(writeOn).map{ case(id2,en2) => Mux((id1 === id2) & en1 & en2, 1.U, 0.U)}.reduce{_+_} }.reduce{_+_} != writeOn.map{Mux(_, 1.U, 0.U)}.reduce{_+_}
    io.debug.invalidWAddr := ~wInBound
    io.debug.invalidRAddr := ~rInBound
    io.debug.rwOn := rwOn
    io.debug.readCollision := rCollide
    io.debug.writeCollision := wCollide
    io.debug.error := ~wInBound | ~rInBound | rwOn | rCollide | wCollide
  }

}

class MemND_Old(val dims: List[Int], bitWidth: Int = 32, syncMem: Boolean = false) extends Module { 
  val depth = dims.reduce{_*_} // Size of memory
  val N = dims.length // Number of dimensions
  val addrWidth = dims.map{Utils.log2Up(_)}.max

  val io = IO( new Bundle {
    val w = Input(new multidimW(N, dims, bitWidth))
    val wMask = Input(Bool())
    val r = Input(new multidimR(N, dims, bitWidth))
    val rMask = Input(Bool())
    val flow = Input(Bool())
    val output = new Bundle {
      val data  = Output(UInt(bitWidth.W))
    }
    val debug = new Bundle {
      val invalidRAddr = Output(Bool())
      val invalidWAddr = Output(Bool())
      val rwOn = Output(Bool())
      val error = Output(Bool())
    }
  })

  // Instantiate 1D mem
  val m = Module(new Mem1D_Old(depth, bitWidth, syncMem))

  // Address flattening
  m.io.w.addr := Utils.getRetimed(io.w.addr.zipWithIndex.map{ case (addr, i) =>
    // FringeGlobals.bigIP.multiply(addr, (banks.drop(i).reduce{_.*-*(_,None)}/-/banks(i)).U, 0)
   addr.*-*((dims.drop(i).reduce{_*_}/dims(i)).U, None)
  }.reduce{_+_}, 0 max Utils.sramstore_latency - 1)
  m.io.r.addr := Utils.getRetimed(io.r.addr.zipWithIndex.map{ case (addr, i) =>
    // FringeGlobals.bigIP.multiply(addr, (dims.drop(i).reduce{_.*-*(_,None)}/dims(i)).U, 0)
   addr.*-*((dims.drop(i).reduce{_*_}/dims(i)).U, None)
  }.reduce{_+_}, 0 max {Utils.sramload_latency - 1}, io.flow) // Latency set to 2, give 1 cycle for bank to resolve

  // Connect the other ports
  m.io.w.data := Utils.getRetimed(io.w.data, 0 max Utils.sramstore_latency - 1)
  m.io.w.en := Utils.getRetimed(io.w.en & io.wMask, 0 max Utils.sramstore_latency - 1)
  m.io.r.en := Utils.getRetimed(io.r.en & io.rMask, 0 max {Utils.sramload_latency - 1}, io.flow) // Latency set to 2, give 1 cycle for bank to resolve
  m.io.flow := io.flow
  io.output.data := Utils.getRetimed(m.io.output.data, if (syncMem) 0 else {if (Utils.retime) 1 else 0}, io.flow)
  if (scala.util.Properties.envOrElse("RUNNING_REGRESSION", "0") == "1") {
    // Check if read/write is in bounds
    val rInBound = io.r.addr.zip(dims).map { case (addr, bound) => addr < bound.U }.reduce{_&_}
    val wInBound = io.w.addr.zip(dims).map { case (addr, bound) => addr < bound.U }.reduce{_&_}
    io.debug.invalidWAddr := ~wInBound
    io.debug.invalidRAddr := ~rInBound
    io.debug.rwOn := io.w.en & io.wMask & io.r.en & io.rMask
    io.debug.error := ~wInBound | ~rInBound | (io.w.en & io.r.en)
  }
}


class Mem1D_Old(val size: Int, bitWidth: Int, syncMem: Boolean = false) extends Module { // Unbanked, inner 1D mem
  def this(size: Int) = this(size, 32)

  val addrWidth = Utils.log2Up(size)

  val io = IO( new Bundle {
    val w = Input(new flatW(addrWidth, bitWidth))
    val r = Input(new flatR(addrWidth, bitWidth))
    val flow = Input(Bool())
    val output = new Bundle {
      val data  = Output(UInt(bitWidth.W))
    }
    val debug = new Bundle {
      val invalidRAddr = Output(Bool())
      val invalidWAddr = Output(Bool())
      val rwOn = Output(Bool())
      val error = Output(Bool())
      // val addrProbe = Output(UInt(bitWidth.W))
    }
  })

  // We can do better than MaxJ by forcing mems to be single-ported since
  //   we know how to properly schedule reads and writes
  val wInBound = io.w.addr < (size).U
  val rInBound = io.r.addr < (size).U

  if (syncMem) {
    if (size <= Utils.SramThreshold) {
      val m = (0 until size).map{ i =>
        val reg = RegInit(0.U(bitWidth.W))
        reg := Mux(io.w.en & (io.w.addr === i.U(addrWidth.W)), io.w.data, reg)
        (i.U(addrWidth.W) -> reg)
      }
      val radder = Utils.getRetimed(io.r.addr,1)
      io.output.data := MuxLookup(radder, 0.U(bitWidth.W), m)
    } else {
      val m = Module(new fringe.SRAM(UInt(bitWidth.W), size, "BRAM"))
      m.io.raddr     := io.r.addr
      m.io.waddr     := io.w.addr
      m.io.wen       := io.w.en & wInBound
      m.io.wdata     := io.w.data
      m.io.flow      := io.flow
      io.output.data := m.io.rdata
    }
  } else {
    if (size <= Utils.SramThreshold) {
      val m = (0 until size).map{ i =>
        val reg = RegInit(0.U(bitWidth.W))
        reg := Mux(io.w.en & (io.w.addr === i.U(addrWidth.W)), io.w.data, reg)
        (i.U(addrWidth.W) -> reg)
      }
      io.output.data := MuxLookup(io.r.addr, 0.U(bitWidth.W), m)
    } else {
      val m = Mem(size, UInt(bitWidth.W) /*, seqRead = true deprecated? */)
      when (io.w.en & wInBound) {m(io.w.addr) := io.w.data}
      io.output.data := m(io.r.addr)
    }
  }

  if (scala.util.Properties.envOrElse("RUNNING_REGRESSION", "0") == "1") {
    io.debug.invalidRAddr := ~rInBound
    io.debug.invalidWAddr := ~wInBound
    io.debug.rwOn := io.w.en & io.r.en
    io.debug.error := ~rInBound | ~wInBound | (io.w.en & io.r.en)
    // io.debug.addrProbe := m(0.U)
  }

}


class flatW(val a: Int, val w: Int) extends Bundle {
  val addr = UInt(a.W)
  val data = UInt(w.W)
  val en = Bool()

  override def cloneType = (new flatW(a, w)).asInstanceOf[this.type] // See chisel3 bug 358
}
class flatR(val a:Int, val w: Int) extends Bundle {
  val addr = UInt(a.W)
  val en = Bool()

  override def cloneType = (new flatR(a, w)).asInstanceOf[this.type] // See chisel3 bug 358
}
class multidimW(val N: Int, val dims: List[Int], val w: Int) extends Bundle {
  assert(N == dims.length)
  // val addr = Vec(N, UInt(32.W))
  val addr = HVec.tabulate(N){i => UInt((Utils.log2Up(dims(i))).W)}
  // val addr = dims.map{d => UInt((Utils.log2Up(d)).W)}
  val data = UInt(w.W)
  val en = Bool()

  override def cloneType = (new multidimW(N, dims, w)).asInstanceOf[this.type] // See chisel3 bug 358
}
class multidimR(val N: Int, val dims: List[Int], val w: Int) extends Bundle {
  assert(N == dims.length)
  // val addr = Vec(N, UInt(32.W))
  val addr = HVec.tabulate(N){i => UInt((Utils.log2Up(dims(i))).W)}
  // val addr = dims.map{d => UInt((Utils.log2Up(d)).W)}
  val en = Bool()
  
  override def cloneType = (new multidimR(N, dims, w)).asInstanceOf[this.type] // See chisel3 bug 358
}
