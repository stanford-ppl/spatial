package fringe.templates.memory

import chisel3._
import chisel3.util._
import fringe._
import fringe.Ledger._
import fringe.templates.counters.{CompactingCounter, CompactingIncDincCtr, IncDincCtr, SingleSCounterCheap}
import fringe.templates.math.Math
import fringe.utils._
import fringe.utils.XMap._
import fringe.utils.DMap._
import fringe.utils.HVec
import fringe.utils.{getRetimed, log2Up}
import fringe.utils.implicits._
import emul.ResidualGenerator._


abstract class MemPrimitive(val p: MemParams) extends Module {
  val io = p.iface match {
    case StandardInterfaceType => IO(new StandardInterface(p))
    case ShiftRegFileInterfaceType => IO(new ShiftRegFileInterface(p))
    case FIFOInterfaceType => IO(new FIFOInterface(p))
  } 

  p.iface match {
    case StandardInterfaceType => io.asInstanceOf[StandardInterface] <> DontCare
    case ShiftRegFileInterfaceType => io.asInstanceOf[ShiftRegFileInterface] <> DontCare
    case FIFOInterfaceType => io.asInstanceOf[FIFOInterface] <> DontCare    
  }

  def connectBufW(p: W_Port, lane: Int, mask: Bool): Unit = {
    io.wPort(lane).banks    :=  p.banks
    io.wPort(lane).ofs      :=  p.ofs
    io.wPort(lane).data     :=  p.data
    io.wPort(lane).reset    :=  p.reset
    io.wPort(lane).init     :=  p.init
    io.wPort(lane).shiftEn  :=  p.shiftEn.map(_ && mask)
    io.wPort(lane).en       :=  p.en.map(_ && mask)
  }

  def connectBufR(p: R_Port, lane: Int, mask: Bool): Unit = {
    io.rPort(lane).banks        := p.banks
    io.rPort(lane).ofs          := p.ofs
    io.rPort(lane).en           := p.en.map(_ & mask)
    io.rPort(lane).backpressure := p.backpressure
  }

  override def desiredName = p.myName
}


class BankedSRAM(p: MemParams) extends MemPrimitive(p) {

  def this(logicalDims: List[Int], bitWidth: Int, banks: List[Int], strides: List[Int],
           WMapping: List[Access], RMapping: List[Access],
           bankingMode: BankingMode, inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int, myName: String = "sram") = this(MemParams(StandardInterfaceType, logicalDims,bitWidth,banks,strides,WMapping,RMapping,bankingMode,inits,syncMem,fracBits, numActives = numActives, myName = myName))
  def this(tuple: (List[Int], Int, List[Int], List[Int], List[Access], List[Access],
    BankingMode)) = this(MemParams(StandardInterfaceType,tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7))

  // Get info on physical dims
  // TODO: Upcast dims to evenly bank
  val numMems = p.banks.product
  val bankDim = math.ceil(p.depth.toDouble / numMems.toDouble).toInt

  // Create list of (mem: Mem1D, coords: List[Int] <coordinates of bank>)
  val m = (0 until numMems).map{ i =>
    val mem = Module(new Mem1D(bankDim, p.bitWidth, p.syncMem))
    mem.io <> DontCare
    val coords = p.banks.zipWithIndex.map{ case (b,j) =>
      i % (p.banks.drop(j).product) / p.banks.drop(j+1).product
    }
    (mem,coords)
  }

  // Handle Writes
  m.foreach{ mem =>
    // See which W ports can see this mem
    val connected: Seq[(W_Port, Seq[Int])] = io.wPort.collect{case x if (canSee(x.visibleBanks, mem._2, p.banks)) => (x, lanesThatCanSee(x.visibleBanks, mem._2, p.banks))}

    if (connected.size > 0) {
      val (ens, datas, ofs) = connected.map{case (port, lanes) => 
        val lane_enables:    Seq[Bool]          = lanes.map(port.en)
        val visible_in_lane: Seq[Seq[Seq[Int]]] = lanes.map(port.visibleBanks).map(_.zipWithIndex.map{case(r,j) => r.expand(p.banks(j))})
        val banks_for_lane:  Seq[Seq[UInt]]     = lanes.map(port.banks.grouped(p.banks.size).toSeq)
        val bank_matches:    Seq[Bool]          = banks_for_lane.zip(visible_in_lane).map{case (wireBanks, visBanks) => (wireBanks, mem._2, visBanks).zipped.map{case (a,b,c) => if (c.size == 1) true.B else {a === b.U}}.reduce{_&&_}}
        val ens:             Seq[Bool]          = lane_enables.zip(bank_matches).map{case (a,b) => a && b}
        val datas:           Seq[UInt]          = lanes.map(port.data)
        val ofs:             Seq[UInt]          = lanes.map(port.ofs)
        (ens,datas,ofs)
      }.reduce[(Seq[Bool], Seq[UInt], Seq[UInt])]{case 
        (
         a: (Seq[Bool], Seq[UInt], Seq[UInt]),
         b: (Seq[Bool], Seq[UInt], Seq[UInt])
        ) => (a._1 ++ b._1, a._2 ++ b._2, a._3 ++ b._3)}

      val finalChoice = fatMux("PriorityMux", ens, ens, datas, ofs)
      mem._1.io.w.ofs.head := finalChoice(2)
      mem._1.io.w.data.head := finalChoice(1)
      mem._1.io.w.en.head := finalChoice(0)
    }
  }


  // Handle Reads
  m.foreach{ mem =>
    val connected: Seq[(R_Port, Seq[Int])] = io.rPort.collect{case x if (canSee(x.visibleBanks, mem._2, p.banks)) => (x, lanesThatCanSee(x.visibleBanks, mem._2, p.banks))}

    if (connected.size > 0) {
      val (rawEns, ofs, backpressures) = connected.map{case (port, lanes) => 
        val lane_enables:    Seq[Bool]          = lanes.map(port.en)
        val visible_in_lane: Seq[Seq[Seq[Int]]] = lanes.map(port.visibleBanks).map(_.zipWithIndex.map{case(r,j) => r.expand(p.banks(j))})
        val banks_for_lane:  Seq[Seq[UInt]]     = lanes.map(port.banks.grouped(p.banks.size).toSeq)
        val bank_matches:    Seq[Bool]          = banks_for_lane.zip(visible_in_lane).map{case (wireBanks, visBanks) => (wireBanks, mem._2, visBanks).zipped.map{case (a,b,c) => if (c.size == 1) true.B else {a === b.U}}.reduce{_&&_}}
        val ens:             Seq[Bool]          = lane_enables.zip(bank_matches).map{case (a,b) => a && b}
        val ofs:             Seq[UInt]          = lanes.map(port.ofs)
        val backpressure:    Seq[Bool]          = Seq.fill(lanes.size){port.backpressure}
        (ens,ofs,backpressure)
      }.reduce[(Seq[Bool], Seq[UInt], Seq[Bool])]{case 
        (
         a: (Seq[Bool], Seq[UInt], Seq[Bool]),
         b: (Seq[Bool], Seq[UInt], Seq[Bool])
        ) => (a._1 ++ b._1, a._2 ++ b._2, a._3 ++ b._3)}

      // val stickyEns = Module(new StickySelects(rawEns.size))
      // stickyEns.io.ins.zip(rawEns).foreach{case (a,b) => a := b}
      val ens = rawEns //stickyEns.io.outs.map(_.toBool)

      // Unmask write port if any of the above match
      val finalChoice = fatMux("PriorityMux", ens, ens, backpressures, ofs)
      mem._1.io.r.ofs.head := finalChoice(2)
      mem._1.io.r.backpressure := finalChoice(1)
      mem._1.io.r.en.head := finalChoice(0)
    }
  }

  // Connect read data to output
  io.rPort.foreach{ port => 
    port.output.zipWithIndex.foreach{case (out, lane) => 
      val visBanksForLane = port.visibleBanks(lane).zipWithIndex.map{case(r,j) => r.expand(p.banks(j))}
      val visibleMems = m.collect{case (m, ba) if (ba.zip(visBanksForLane).forall{case (real, possible) => possible.contains(real)}) => (m, ba)}
      val datas = visibleMems.map(_._1.io.output)
      val bankMatches = if (visibleMems.size == 1) Seq(true.B) else visibleMems.map(_._2).map{ba => port.banks.grouped(p.banks.size).toSeq(lane).zip(ba).map{case (a,b) => a === b.U}.reduce{_&&_} }
      val en = port.en(lane)
      val sel = bankMatches.map{be => getRetimed(be & en, globals.target.sramload_latency, port.backpressure)}
      out := chisel3.util.PriorityMux(sel, datas)
    }
  }

}  

class FF(p: MemParams) extends MemPrimitive(p) {
  def this(logicalDims: List[Int], bitWidth: Int, banks: List[Int], strides: List[Int],
           WMapping: List[Access], RMapping: List[Access],
           bankingMode: BankingMode, inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int, myName: String = "FF") = this(MemParams(StandardInterfaceType, logicalDims,bitWidth,banks,strides,WMapping,RMapping,bankingMode,inits,syncMem,fracBits, numActives = numActives, myName = myName))
  def this(tuple: (List[Int], Int, List[Int], List[Int], List[Access], List[Access],
    BankingMode)) = this(MemParams(StandardInterfaceType,tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7))

  def this(tuple: (Int, List[Access])) = this(List(1), tuple._1,List(1), List(1), tuple._2, List(AccessHelper.singular(32)), BankedMemory, None, false, 0, 1)
  def this(bitWidth: Int) = this(List(1), bitWidth,List(1), List(1), List(AccessHelper.singular(bitWidth)), List(AccessHelper.singular(bitWidth)), BankedMemory, None, false, 0, 1)
  def this(bitWidth: Int, WMapping: List[Access], RMapping: List[Access], inits: Option[List[Double]], fracBits: Int, numActives: Int, myName: String) = this(List(1), bitWidth,List(1), List(1), WMapping, RMapping, BankedMemory, inits, false, fracBits, numActives = numActives, myName)

  val init = 
    if (p.inits.isDefined) {
      if (p.bitWidth == 1) {if (p.inits.get.head == 0.0) false.B else true.B}
      else                 (p.inits.get.head*scala.math.pow(2,p.fracBits)).toLong.S(p.bitWidth.W).asUInt
    }
    else io.wPort(0).init

  val ff = RegInit(init)
  val anyReset: Bool = io.wPort.map{_.reset}.toList.reduce{_|_} | io.reset
  val anyEnable: Bool = io.wPort.flatMap{_.en}.toList.reduce{_|_}
  val wr_data: UInt = chisel3.util.PriorityMux(io.wPort.flatMap{_.en}.toList, io.wPort.flatMap{_.data}.toList)
  ff := Mux(anyReset, init, Mux(anyEnable, wr_data, ff))
  io.rPort.foreach(_.output.head := ff)
}

// class FIFOReg(p: MemParams) extends MemPrimitive(p) {
//   // Compatibility with standard mem codegen
//   def this(logicalDims: List[Int], bitWidth: Int, 
//            banks: List[Int], strides: List[Int], 
//            xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
//            directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
//            bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int, myName: String = "FIFOReg") = this(MemParams(FIFOInterfaceType, logicalDims, bitWidth, banks, strides, xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, init, syncMem, fracBits, numActives = numActives, myName = myName))
//   // def this(logicalDims: List[Int], bitWidth: Int, 
//   //          banks: List[Int], strides: List[Int], 
//   //          xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
//   //          directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
//   //          bankingMode: BankingMode, init: => Option[List[Int]], syncMem: Boolean, fracBits: Int) = this(MemParams(logicalDimsType, bitWidth, banks, strides, xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, {if (init.isDefined) Some(init.get.map(_.toDouble)) else None}, syncMem, fracBits))
//   def this(tuple: (Int, XMap)) = this(List(1), tuple._1,List(1), List(1), tuple._2, XMap((0,0,0) -> (1, None)), DMap(), DMap(), BankedMemory, None, false, 0, 2)
//   def this(bitWidth: Int) = this(List(1), bitWidth,List(1), List(1), XMap((0,0,0) -> (1, None)), XMap((0,0,0) -> (1, None)), DMap(), DMap(), BankedMemory, None, false, 0, 2)
//   def this(bitWidth: Int, xBarWMux: XMap, xBarRMux: XMap, inits: Option[List[Double]], fracBits: Int, numActives: Int) = this(List(1), bitWidth,List(1), List(1), xBarWMux, xBarRMux, DMap(), DMap(), BankedMemory, inits, false, fracBits, numActives)

//   val init = 
//     if (p.inits.isDefined) {
//       if (p.bitWidth == 1) {if (p.inits.get.head == 0.0) false.B else true.B}
//       else                 (p.inits.get.head*scala.math.pow(2,p.fracBits)).toLong.S(p.bitWidth.W).asUInt
//     }
//     else io.xBarW(0).init.head

//   val ff = RegInit(init)

//   val anyReset: Bool = io.xBarW.map{_.reset}.flatten.toList.reduce{_|_} | io.reset
//   val anyWrite: Bool = io.xBarW.map{_.en}.flatten.toList.reduce{_|_}
//   val anyRead: Bool = io.xBarR.map{_.en}.flatten.toList.reduce{_|_}
//   val wr_data: UInt = chisel3.util.PriorityMux(io.xBarW.map{_.en}.flatten.toList, io.xBarW.map{_.data}.flatten.toList)
//   ff := Mux(anyReset, init, Mux(anyWrite, wr_data, ff))
//   io.output.foreach(_ := ff)

//   val isValid = Module(new SRFF())
//   isValid.io.input.set := anyWrite
//   isValid.io.input.reset := anyRead
//   isValid.io.input.asyn_reset := false.B

//   // Check if there is data
//   io.asInstanceOf[FIFOInterface].accessActivesOut.zip(io.asInstanceOf[FIFOInterface].accessActivesIn).foreach{case (o,i) => o := i}
//   io.asInstanceOf[FIFOInterface].empty := ~isValid.io.output
//   io.asInstanceOf[FIFOInterface].full := isValid.io.output
//   io.asInstanceOf[FIFOInterface].almostEmpty := false.B
//   io.asInstanceOf[FIFOInterface].almostFull := false.B
//   io.asInstanceOf[FIFOInterface].numel := Mux(isValid.io.output, 1.U, 0.U)

// }

class FIFO(p: MemParams) extends MemPrimitive(p) {
  def this(logicalDims: List[Int], bitWidth: Int,
           banks: List[Int], WMapping: List[Access], RMapping: List[Access],
           inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int) = this(MemParams(FIFOInterfaceType,logicalDims, bitWidth, banks, List(1), WMapping, RMapping, BankedMemory, inits, syncMem, fracBits, numActives = numActives, myName = "FIFO"))

  def this(tuple: (List[Int], Int, List[Int], List[Access], List[Access], Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, None, false, 0, tuple._6)
  def this(logicalDims: List[Int], bitWidth: Int,
           banks: List[Int], strides: List[Int],
           WMapping: List[Access], RMapping: List[Access],
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int, myName: String = "FIFO") = this(MemParams(FIFOInterfaceType,logicalDims, bitWidth, banks, List(1), WMapping, RMapping, bankingMode, init, syncMem, fracBits, numActives = numActives, myName = myName))

  // Create bank counters
  val headCtr = Module(new CompactingCounter(p.WMapping.size, p.depth, p.elsWidth)); headCtr.io <> DontCare
  val tailCtr = Module(new CompactingCounter(p.RMapping.size, p.depth, p.elsWidth)); tailCtr.io <> DontCare
  (0 until p.WMapping.size).foreach{i => headCtr.io.input.enables.zip(io.wPort.flatMap(_.en)).foreach{case (l,r) => l := r}}
  (0 until p.RMapping.size).foreach{i => tailCtr.io.input.enables.zip(io.rPort.flatMap(_.en)).foreach{case (l,r) => l := r}}
  headCtr.io.input.reset := reset
  tailCtr.io.input.reset := reset
  headCtr.io.input.dir := true.B
  tailCtr.io.input.dir := true.B

  // Create numel counter
  val elements = Module(new CompactingIncDincCtr(p.WMapping.size, p.RMapping.size, p.widestW, p.widestR, p.depth, p.elsWidth))
  elements.io <> DontCare
  elements.io.input.inc_en.zip(io.wPort.flatMap(_.en)).foreach{case(l,r) => l := r}
  elements.io.input.dinc_en.zip(io.rPort.flatMap(_.en)).foreach{case(l,r) => l := r}

  // Create physical mems
  val m = (0 until p.numBanks).map{ i => val x = Module(new Mem1D(p.depth/p.numBanks, p.bitWidth)); x.io <> DontCare; x}

  // Create compacting network

  val enqCompactor = Module(new CompactingEnqNetwork(p.WMapping.map(_.par).toList, p.numBanks, p.elsWidth, p.bitWidth))
  enqCompactor.io <> DontCare
  enqCompactor.io.headCnt := headCtr.io.output.count
  (0 until p.WMapping.size).foreach{i =>
    enqCompactor.io.in.map(_.data).zip(io.wPort.flatMap(_.data)).foreach{case (l,r) => l := r}
    enqCompactor.io.in.map(_.en).zip(io.wPort.flatMap(_.en)).foreach{case(l,r) => l := r}
  }

  // Connect compacting network to banks
  val active_w_bank = Math.singleCycleModulo(headCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  val active_w_addr = Math.singleCycleDivide(headCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  (0 until p.numBanks).foreach{i =>
    val addr = Mux(i.S(p.elsWidth.W) < active_w_bank, active_w_addr + 1.S(p.elsWidth.W), active_w_addr)
    m(i).io.w.ofs.head := addr.asUInt
    m(i).io.w.data.head := enqCompactor.io.out(i).data
    m(i).io.w.en.head   := enqCompactor.io.out(i).en
  }

  // Create dequeue compacting network
  val deqCompactor = Module(new CompactingDeqNetwork(p.RMapping.map(_.par).toList, p.numBanks, p.elsWidth, p.bitWidth))
  deqCompactor.io <> DontCare
  deqCompactor.io.tailCnt := tailCtr.io.output.count
  val active_r_bank = Math.singleCycleModulo(tailCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  val active_r_addr = Math.singleCycleDivide(tailCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  (0 until p.numBanks).foreach{i =>
    val addr = Mux(i.S(p.elsWidth.W) < active_r_bank, active_r_addr + 1.S(p.elsWidth.W), active_r_addr)
    m(i).io.r.ofs.head := addr.asUInt
    deqCompactor.io.input.data(i) := m(i).io.output
  }
  (0 until p.RMapping.size).foreach{i =>
    deqCompactor.io.input.deq.zip(io.rPort.flatMap(_.en)).foreach{case (l,r) => l := r}
  }
  io.rPort.foreach{p => 
    p.output.zip(deqCompactor.io.output).foreach{case (a,b) => a := b}
  }

  // Check if there is data
  io.asInstanceOf[FIFOInterface].accessActivesOut.zip(io.asInstanceOf[FIFOInterface].accessActivesIn).foreach{case (o,i) => o := i}
  io.asInstanceOf[FIFOInterface].empty := elements.io.output.empty
  io.asInstanceOf[FIFOInterface].full := elements.io.output.full
  io.asInstanceOf[FIFOInterface].almostEmpty := elements.io.output.almostEmpty
  io.asInstanceOf[FIFOInterface].almostFull := elements.io.output.almostFull
  io.asInstanceOf[FIFOInterface].numel := elements.io.output.numel.asUInt


}

// class LIFO(p: MemParams) extends MemPrimitive(p) {

//   def this(logicalDims: List[Int], bitWidth: Int,
//            banks: List[Int],
//            xBarWMux: XMap, xBarRMux: XMap,
//            inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int) = this(MemParams(FIFOInterfaceType,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, DMap(), DMap(), BankedMemory, inits, syncMem, fracBits, numActives = numActives, myName = "LIFO"))
//   def this(tuple: (List[Int], Int, List[Int], XMap, XMap, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, None, false, 0, tuple._6)
//   def this(logicalDims: List[Int], bitWidth: Int,
//            banks: List[Int], strides: List[Int],
//            xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
//            directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
//            bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int, myName: String = "LIFO") = this(MemParams(FIFOInterfaceType,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, init, syncMem, fracBits, numActives = numActives, myName = myName))

//   val pW = p.xBarWMux.accessPars.max
//   val pR = p.xBarRMux.accessPars.max
//   val par = scala.math.max(pW, pR) // TODO: Update this template because this was from old style

//   // Register for tracking number of elements in FILO
//   val elements = Module(new IncDincCtr(pW,pR, p.depth))
//   elements.io.input.inc_en := io.wPort.flatMap(_.en).toList.reduce{_|_}
//   elements.io.input.dinc_en := io.rPort.flatMap(_.en).toList.reduce{_|_}

//   // Create physical mems
//   val m = (0 until par).map{ i => val x = Module(new Mem1D(p.depth/par, p.bitWidth)); x.io <> DontCare; x}

//   // Create head and reader sub counters
//   val sa_width = 2 + log2Up(par)
//   val subAccessor = Module(new SingleSCounterCheap(1,0,par,pW,-pR,sa_width))
//   subAccessor.io <> DontCare
//   subAccessor.io.input.enable := io.wPort.flatMap(_.en).toList.reduce{_|_} | io.rPort.flatMap(_.en).toList.reduce{_|_}
//   subAccessor.io.input.dir := io.wPort.flatMap(_.en).toList.reduce{_|_}
//   subAccessor.io.input.reset := reset
//   subAccessor.io.setup.saturate := false.B
//   val subAccessor_prev = Mux(subAccessor.io.output.count(0) - pR.S(sa_width.W) < 0.S(sa_width.W), (par-pR).S(sa_width.W), subAccessor.io.output.count(0) - pR.S(sa_width.W))

//   // Create head and reader counters
//   val a_width = 2 + log2Up(p.depth/par)
//   val accessor = Module(new SingleSCounterCheap(1, 0, (p.depth/par), 1, -1, a_width))
//   accessor.io <> DontCare
//   accessor.io.input.enable := (io.wPort.flatMap(_.en).toList.reduce{_|_} & subAccessor.io.output.done) | (io.rPort.flatMap(_.en).toList.reduce{_|_} & subAccessor_prev === 0.S(sa_width.W))
//   accessor.io.input.dir := io.wPort.flatMap(_.en).toList.reduce{_|_}
//   accessor.io.input.reset := reset
//   accessor.io.setup.saturate := false.B


//   // Connect pusher
//   if (pW == pR) {
//     m.zipWithIndex.foreach { case (mem, i) =>
//       // Figure out which write port was active in xBar
//       val xBarIds = p.xBarWMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarWMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }.toList
//       val xBarCandidatesEns = xBarIds.map{n => io.wPort.flatMap(_.en).toList(n+i)}
//       val xBarCandidatesDatas = xBarIds.map{n => io.wPort.flatMap(_.data).toList(n+i)}

//       // Make connections to memory
//       mem.io.w.ofs.head := accessor.io.output.count(0).asUInt
//       mem.io.w.data.head := PriorityMux(xBarCandidatesEns, xBarCandidatesDatas)
//       mem.io.w.en.head := xBarCandidatesEns.or
//     }
//   } else {
//     (0 until pW).foreach { w_i =>
//       (0 until (par / pW)).foreach { i =>
//         // Figure out which write port was active in xBar
//         val xBarIds = p.xBarWMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarWMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }.toList
//         val xBarCandidatesEns = xBarIds.map{n => io.wPort.flatMap(_.en).toList(n+(i*pW+w_i))}
//         val xBarCandidatesDatas = xBarIds.map{n => io.wPort.flatMap(_.data).toList(n+(i*pW+w_i))}

//         // Make connections to memory
//         m(w_i + i*pW).io.w.ofs.head := accessor.io.output.count(0).asUInt
//         m(w_i + i*pW).io.w.data.head := PriorityMux(xBarCandidatesEns, xBarCandidatesDatas)
//         m(w_i + i*pW).io.w.en.head := xBarCandidatesEns.or & (subAccessor.io.output.count(0) === (i*pW).S(sa_width.W))
//       }
//     }
//   }

//   // Connect popper
//   if (pW == pR) {
//     m.zipWithIndex.foreach { case (mem, i) =>
//       mem.io.r.ofs.head := (accessor.io.output.count(0) - 1.S(a_width.W)).asUInt
//       mem.io.r.en.head := io.rPort.flatMap(_.en).toList.reduce{_|_}
//       io.output(i) := mem.io.output
//     }
//   } else {
//     (0 until pR).foreach { r_i =>
//       val rSel = Wire(Vec( (par/pR), Bool()))
//       val rData = Wire(Vec( (par/pR), UInt(p.bitWidth.W)))
//       (0 until (par / pR)).foreach { i =>
//         m(r_i + i*pR).io.r.ofs.head := (accessor.io.output.count(0) - 1.S(sa_width.W)).asUInt
//         m(r_i + i*pR).io.r.en.head := io.rPort.flatMap(_.en).toList.reduce{_|_} & (subAccessor_prev === (i*pR).S(sa_width.W))
//         rSel(i) := subAccessor_prev === i.S
//         rData(i) := m(r_i + i*pR).io.output
//       }
//       io.output(pR - 1 - r_i) := chisel3.util.PriorityMux(rSel, rData)
//     }
//   }

//   // Check if there is data
//   io.asInstanceOf[FIFOInterface].accessActivesOut.zip(io.asInstanceOf[FIFOInterface].accessActivesIn).foreach{case (o,i) => o := i}
//   io.asInstanceOf[FIFOInterface].empty := elements.io.output.empty
//   io.asInstanceOf[FIFOInterface].full := elements.io.output.full
//   io.asInstanceOf[FIFOInterface].almostEmpty := elements.io.output.almostEmpty
//   io.asInstanceOf[FIFOInterface].almostFull := elements.io.output.almostFull
//   io.asInstanceOf[FIFOInterface].numel := elements.io.output.numel.asUInt

// }

class ShiftRegFile(p: MemParams) extends MemPrimitive(p) {
  def this(logicalDims: List[Int], bitWidth: Int, banks: List[Int], strides: List[Int],
           WMapping: List[Access], RMapping: List[Access],
           inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int, myName: String) = this(MemParams(ShiftRegFileInterfaceType, logicalDims,bitWidth,banks,strides,WMapping,RMapping,BankedMemory,inits,syncMem,fracBits, false, numActives, myName))
  def this(logicalDims: List[Int], bitWidth: Int, banks: List[Int], strides: List[Int],
           WMapping: List[Access], RMapping: List[Access],
           bankingMode: BankingMode, inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int, myName: String) = this(MemParams(ShiftRegFileInterfaceType, logicalDims,bitWidth,banks,strides,WMapping,RMapping,bankingMode,inits,syncMem,fracBits, false, numActives, myName))


  // Create list of (mem: Mem1D, coords: List[Int] <coordinates of bank>)
  val m = (0 until p.depth).map{ i =>
    val coords = p.logicalDims.zipWithIndex.map{ case (b,j) =>
      i % (p.logicalDims.drop(j).product) / p.logicalDims.drop(j+1).product
    }
    val initval = if (p.inits.isDefined) (p.inits.get.apply(i)*scala.math.pow(2,p.fracBits)).toLong.U(p.bitWidth.W) else 0.U(p.bitWidth.W)
    val mem = RegInit(initval)
    io.asInstanceOf[ShiftRegFileInterface].dump_out(i) := mem
    (mem,coords,initval)
  }

  def stripCoord(l: List[Int], x: Int): List[Int] = {l.take(x) ++ l.drop(x+1)}
  def stripCoord(l: HVec[UInt], x: Int): HVec[UInt] = {HVec(l.take(x) ++ l.drop(x+1))}
  def decrementAxisCoord(l: List[Int], x: Int): List[Int] = {l.take(x) ++ List(l(x) - 1) ++ l.drop(x+1)}

  // Handle Writes
  m.foreach{ mem =>
    val coords = mem._2
    val initval = mem._3
    // See which W ports can see this mem
    val connected: Seq[(W_Port, Seq[Int])] = io.wPort.collect{case x if (canSee(x.visibleBanks, mem._2, p.banks)) => (x, lanesThatCanSee(x.visibleBanks, mem._2, p.banks))}

    if (connected.size > 0) {
      val (ens, datas) = connected.map{case (port, lanes) => 
        val lane_enables:    Seq[Bool]          = lanes.map(port.en)
        val visible_in_lane: Seq[Seq[Seq[Int]]] = lanes.map(port.visibleBanks).map(_.zipWithIndex.map{case(r,j) => r.expand(j)})
        val banks_for_lane:  Seq[Seq[UInt]]     = lanes.map(port.banks.grouped(p.banks.size).toSeq)
        val bank_matches:    Seq[Bool]          = banks_for_lane.zip(visible_in_lane).map{case (wireBanks, visBanks) => (wireBanks, mem._2, visBanks).zipped.map{case (a,b,c) => if (c.size == 1) true.B else {a === b.U}}.reduce{_&&_}}
        val ens:             Seq[Bool]          = lane_enables.zip(bank_matches).map{case (a,b) => a && b}
        val datas:           Seq[UInt]          = lanes.map(port.data)
        (ens,datas)
      }.reduce[(Seq[Bool], Seq[UInt])]{case 
        (
         a: (Seq[Bool], Seq[UInt]),
         b: (Seq[Bool], Seq[UInt])
        ) => (a._1 ++ b._1, a._2 ++ b._2)}

      // TODO: FIGURE OUT SHIFT CONNECTIONS AND THINGS BELOW
  //   // Check if shiftEn is turned on for this line, guaranteed false on entry plane
  //   val shiftMask = if (p.axis >= 0 && coords(p.axis) != 0) {
  //     val axisShiftXBar = io.wPort.flatMap(_.banks).toList.grouped(p.banks.length).toList.zip(io.wPort.flatMap(_.shiftEn).toList).map{ case(bids, en) =>
  //       bids.zip(coords).zipWithIndex.map{case ((b, coord),id) => if (id == p.axis) true.B else b === coord.U}.reduce{_&&_} & en
  //     }
  //     // Unmask shift if any of the above match
  //     axisShiftXBar.reduce{_|_}
  //   } else false.B

    // mem._1 := Mux(io.reset, initval, Mux(shiftEnable, shiftSource, Mux(enable, data, mem)))
    }
  }


  

  // // Handle Writes
  // m.foreach{ case(mem, coords, flatCoord, initval) =>
  //   // Check all xBar w ports against this bank's coords
  //   val xBarSelect = (io.xBarW.flatMap(_.banks).toList.grouped(p.banks.length).toList, io.xBarW.flatMap(_.en).toList, io.xBarW.flatMap(_.shiftEn).toList).zipped.map{ case(bids, en, shiftEn) =>
  //     bids.zip(coords).map{case (b,coord) => b === coord.U}.reduce{_&&_} & {if (p.hasXBarW) en | shiftEn else false.B}
  //   }
  //   // Check all direct W ports against this bank's coords
  //   val directSelectEns = io.directW.flatMap(_.en).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
  //   val directSelectDatas = io.directW.flatMap(_.data).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
  //   val directSelectOffsets = io.directW.flatMap(_.ofs).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
  //   val directSelectShiftEns = io.directW.flatMap(_.shiftEn).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}

  //   // Unmask write port if any of the above match
  //   val wMask = xBarSelect.reduce{_|_} | directSelectEns.or | directSelectShiftEns.or

  //   // Check if shiftEn is turned on for this line, guaranteed false on entry plane
  //   val shiftMask = if (p.axis >= 0 && coords(p.axis) != 0) {
  //     // XBarW requests shift
  //     val axisShiftXBar = io.xBarW.flatMap(_.banks).toList.grouped(p.banks.length).toList.zip(io.xBarW.flatMap(_.shiftEn).toList).map{ case(bids, en) =>
  //       bids.zip(coords).zipWithIndex.map{case ((b, coord),id) => if (id == p.axis) true.B else b === coord.U}.reduce{_&&_} & {if (p.hasXBarW) en else false.B}
  //     }
  //     // // DirectW requests shift
  //     // val axisShiftDirect = io.directW.map(_.banks).flatten.filter{case banks => stripCoord(banks, p.axis).zip(stripCoord(coords, p.axis)).map{case (b,coord) => b == coord}.reduce(_&_)}

  //     // Unmask shift if any of the above match
  //     axisShiftXBar.reduce{_|_} | directSelectShiftEns.or
  //   } else false.B

  //   // Connect matching W port to memory
  //   val shiftSource = if (p.axis >= 0 && coords(p.axis) != 0) m.filter{case (_,c,_,_) => decrementAxisCoord(coords,p.axis) == c}.head._1 else mem
  //   val shiftEnable = if (p.axis >= 0 && coords(p.axis) != 0) shiftMask else false.B
  //   val (data, enable) =
  //     if (directSelectEns.nonEmpty & p.hasXBarW) {           // Has direct and x
  //       val liveDirectWireSelects = directSelectEns.zip(directSelectShiftEns).map{case (e,se) => e | se}
  //       val liveDirectWireData = chisel3.util.PriorityMux(liveDirectWireSelects, directSelectDatas)
  //       val liveDirectWireEn = chisel3.util.PriorityMux(liveDirectWireSelects, liveDirectWireSelects)
  //       val liveXBarWireEn = chisel3.util.PriorityMux(xBarSelect, io.xBarW.flatMap(_.en).zip(io.xBarW.flatMap(_.shiftEn)).map{case(e,se) => e | se})
  //       val liveXBarWireData = chisel3.util.PriorityMux(xBarSelect, io.xBarW.flatMap(_.data))
  //       val enable = Mux(liveDirectWireSelects.or, liveDirectWireEn, liveXBarWireEn) & wMask
  //       val data = Mux(liveDirectWireSelects.or, liveDirectWireData, liveXBarWireData)
  //       (data, enable)
  //     }
  //     else if (p.hasXBarW && directSelectEns.isEmpty) {  // Has x only
  //       val liveXBarWireEn = chisel3.util.PriorityMux(xBarSelect, io.xBarW.flatMap(_.en).zip(io.xBarW.flatMap(_.shiftEn)).map{case(e,se) => e | se})
  //       val liveXBarWireData = chisel3.util.PriorityMux(xBarSelect, io.xBarW.flatMap(_.data))
  //       val enable = (liveXBarWireEn) & wMask
  //       val data = liveXBarWireData
  //       (data, enable)
  //     }
  //     else if (directSelectEns.nonEmpty) {                                            // Has direct only
  //       val liveDirectWireSelects = directSelectEns.zip(directSelectShiftEns).map{case (e,se) => e | se}
  //       val liveDirectWireData = chisel3.util.PriorityMux(liveDirectWireSelects, directSelectDatas)
  //       val liveDirectWireEn = chisel3.util.PriorityMux(liveDirectWireSelects, liveDirectWireSelects)
  //       val enable = (liveDirectWireEn) & wMask
  //       val data = liveDirectWireData
  //       (data, enable)
  //     } else (0.U, false.B)
  //   if (p.isBuf) mem := Mux(io.asInstanceOf[ShiftRegFileInterface].dump_en, io.asInstanceOf[ShiftRegFileInterface].dump_in(flatCoord), Mux(shiftEnable, shiftSource, Mux(enable, data, mem)))
  //   else mem := Mux(io.reset, initval, Mux(shiftEnable, shiftSource, Mux(enable, data, mem)))
  // }

//   // Connect read data to output
//   io.output.zipWithIndex.foreach { case (wire,i) =>
//     if (p.xBarRMux.nonEmpty && i < p.xBarOutputs) {
//       // Figure out which read port was active in xBar
//       val xBarIds = p.xBarRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }
//       val xBarCandidatesEns = xBarIds.map(io.rPort.flatMap(_.en).toList(_))
//       val xBarCandidatesBanks = xBarIds.map(io.rPort.flatMap(_.banks).toList.grouped(p.banks.length).toList(_))
//       val xBarCandidatesOffsets = xBarIds.map(io.rPort.flatMap(_.ofs).toList(_))
//       val sel = m.map{ mem =>
//         if (xBarCandidatesEns.nonEmpty) (xBarCandidatesEns, xBarCandidatesBanks, xBarCandidatesOffsets).zipped.map {case(en,banks,ofs) =>
//           banks.zip(mem._2).map{case (b, coord) => b === coord.U}.reduce{_&&_} && en
//         }.reduce{_||_} else false.B
//       }
//       val datas = m.map{ _._1 }
//       val d = chisel3.util.PriorityMux(sel, datas)
//       wire := d
//     } else {
//       // Figure out which read port was active in direct
//       val directIds = p.directRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i - p.xBarOutputs < entry._1.length) => p.directRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i - p.xBarOutputs}
//       val directCandidatesEns = directIds.map(io.directR.flatMap(_.en).toList(_))
//       val directCandidatesBanks = directIds.map(io.directR.flatMap(_.banks).toList(_))
//       val directCandidatesOffsets = directIds.map(io.directR.flatMap(_.ofs).toList(_))
//       // Create bit vector to select which bank was activated by this io
//       val sel = m.map{ mem =>
//         if (directCandidatesEns.nonEmpty) (directCandidatesEns, directCandidatesBanks, directCandidatesOffsets).zipped.map {case(en,banks,ofs) =>
//           banks.zip(mem._2).map{case (b, coord) => b == coord}.reduce{_&&_}.B && en
//         }.reduce{_||_} else false.B
//       }
//       val datas = m.map{ _._1 }
//       val d = chisel3.util.PriorityMux(sel, datas)
//       wire := d
//     }
//   }

}

class LUT(p: MemParams) extends MemPrimitive(p) {
  def this(logicalDims: List[Int], bitWidth: Int, banks: List[Int], strides: List[Int],
           WMapping: List[Access], RMapping: List[Access],
           inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int, myName: String) = this(MemParams(StandardInterfaceType, logicalDims,bitWidth,banks,strides,WMapping,RMapping,BankedMemory,inits,syncMem,fracBits, false, numActives, myName))
  def this(logicalDims: List[Int], bitWidth: Int, banks: List[Int], strides: List[Int],
           WMapping: List[Access], RMapping: List[Access],
           bankingMode: BankingMode, inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, numActives: Int, myName: String) = this(MemParams(StandardInterfaceType, logicalDims,bitWidth,banks,strides,WMapping,RMapping,bankingMode,inits,syncMem,fracBits, false, numActives, myName))
  // def this(tuple: (List[Int], Int, List[Access], List[Access])) = this(tuple._1,tuple._2,tuple._3,tuple._4,None, false, 0, false, 1, "LUT")
  // def this(tuple: (List[Int], Int, List[Access], List[Access], Option[List[Double]], Boolean, Int)) = this(tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6, tuple._7, false, 1, "LUT")


  // Create list of (mem: Mem1D, coords: List[Int] <coordinates of bank>)
  val m = (0 until p.depth).map{ i =>
    val coords = p.logicalDims.zipWithIndex.map{ case (b,j) =>
      i % (p.logicalDims.drop(j).product) / p.logicalDims.drop(j+1).product
    }
    val initval = if (p.inits.isDefined) (p.inits.get.apply(i)*scala.math.pow(2,p.fracBits)).toLong.S((p.bitWidth+1).W).asUInt.apply(p.bitWidth,0) else 0.U(p.bitWidth.W)
    val mem = RegInit(initval)
    (mem,coords)
  }

  // Connect read data to output
  io.rPort.foreach{ port => 
    port.output.zipWithIndex.foreach{case (out, lane) => 
      val visBanksForLane = port.visibleBanks(lane).zipWithIndex.map{case(r,j) => r.expand(p.banks(j))}
      val visibleMems = m.collect{case (m, ba) if (ba.zip(visBanksForLane).forall{case (real, possible) => possible.contains(real)}) => (m, ba)}
      val datas = visibleMems.map(_._1)
      val bankMatches = if (visibleMems.size == 1) Seq(true.B) else visibleMems.map(_._2).map{ba => port.banks.grouped(p.banks.size).toSeq(lane).zip(ba).map{case (a,b) => a === b.U}.reduce{_&&_} }
      val en = port.en(lane)
      val sel = bankMatches.map{be => be & en}
      out := chisel3.util.PriorityMux(sel, datas)
    }
  }
}


// Backing memory for SRAM
class Mem1D(val size: Int, bitWidth: Int, syncMem: Boolean = false) extends Module { // Unbanked, inner 1D mem
  def this(size: Int) = this(size, 32)

  val addrWidth = log2Up(size)

  val io = IO( new Bundle {
    val r = Input(new R_Port(1, addrWidth, List(1), bitWidth, List(List(ResidualGenerator(1,0,1)))))
    val w = Input(new W_Port(1, addrWidth, List(1), bitWidth, List(List(ResidualGenerator(1,0,1)))))
    val output = Output(UInt(bitWidth.W))
  })

  // We can do better than MaxJ by forcing mems to be single-ported since
  //   we know how to properly schedule reads and writes
  val wInBound = io.w.ofs.head <= (size).U
  val rInBound = io.r.ofs.head <= (size).U

  if (syncMem) {
    if (size <= globals.target.SramThreshold) {
      val m = (0 until size).map{ i =>
        val reg = RegInit(0.U(bitWidth.W))
        reg := Mux(io.w.en.head & wInBound & (io.w.ofs.head === i.U(addrWidth.W)), io.w.data.head, reg)
        (i.U(addrWidth.W) -> reg)
      }
      val radder = getRetimed(io.r.ofs.head,1,io.r.backpressure)
      io.output := getRetimed(MuxLookup(radder, 0.U(bitWidth.W), m), 1, io.r.backpressure)
    } else {
      val m = Module(new SRAM(UInt(bitWidth.W), size, "Generic")) // TODO: Change to BRAM or URAM once we get SRAMVerilogAWS_BRAM/URAM.v
      if (size >= 2) m.io.raddr     := getRetimed(io.r.ofs.head, 1, io.r.backpressure)
      else           m.io.raddr     := 0.U
      m.io.waddr     := io.w.ofs.head
      m.io.wen       := io.w.en.head & wInBound
      m.io.wdata     := io.w.data.head
      m.io.backpressure      := io.r.backpressure
      io.output := m.io.rdata
    }
  } else {
    if (size <= globals.target.SramThreshold) {
      val m = (0 until size).map{ i =>
        val reg = RegInit(0.U(bitWidth.W))
        reg := Mux(io.w.en.head & (io.w.ofs.head === i.U(addrWidth.W)), io.w.data.head, reg)
        (i.U(addrWidth.W) -> reg)
      }
      io.output := MuxLookup(io.r.ofs.head, 0.U(bitWidth.W), m)
    } else {
      val m = Mem(size, UInt(bitWidth.W) /*, seqRead = true deprecated? */)
      when (io.w.en.head & wInBound) {m(io.w.ofs.head) := io.w.data.head}
      io.output := m(io.r.ofs.head)
    }
  }
}


class enqPort(val w: Int) extends Bundle {
  val data = UInt(w.W)
  val en = Bool()

  override def cloneType = (new enqPort(w)).asInstanceOf[this.type] // See chisel3 bug 358
}

class Compactor(val ports: List[Int], val banks: Int, val width: Int, val bitWidth: Int = 32) extends Module {
  val num_compactors = if (ports.size == 0) 1 else ports.max
  val io = IO( new Bundle {
      val numEnabled =Input(UInt(width.W))
      val in = Vec(1 max ports.sum, Input(new enqPort(bitWidth)))
      val out = Vec(1 max num_compactors, Output(new enqPort(bitWidth)))
    })

    val compacted = (0 until num_compactors).map{i => 
      val num_inputs_per_bundle = ports.map{p => if (i < p) p-i else 0}
      val mux_selects = num_inputs_per_bundle.zipWithIndex.flatMap{case(j, id) =>
        val in_start_id = ports.take(id).sum
        val connect_start_id = num_inputs_per_bundle.take(id).sum
        val num_holes = if ((ports(id)-j) > 0) {
          (0 until (ports(id)-j)).map{ k => Mux(!io.in(in_start_id + k).en, 1.U(width.W), 0.U(width.W)) }.reduce{_+_} // number of 0's in this bundle that precede current
        } else {
          0.U(width.W)          
        }
        (0 until j).map{k => 
          val ens_below = if (k > 0) {(0 until k).map{l => Mux(io.in(in_start_id + (ports(id) - j + l)).en, 1.U(width.W), 0.U(width.W)) }.reduce{_+_}} else {0.U(width.W)}
          io.in(in_start_id + (ports(id) - j + k)).en & ens_below >= num_holes
        }
      }
      val mux_datas = num_inputs_per_bundle.zipWithIndex.map{case(j, id) => 
        val in_start_id = ports.take(id).sum
        val connect_start_id = num_inputs_per_bundle.take(id).sum
        (0 until j).map{k => io.in(in_start_id + (ports(id) - j + k)).data}
      }.flatten
      if (mux_selects.size == 0) io.out(i).data := 0.U else io.out(i).data := chisel3.util.PriorityMux(mux_selects, mux_datas)
      io.out(i).en := i.U(width.W) < io.numEnabled
    }
}

/* This consists of an innermost compactor, surrounded by a router.  The compactor
   takes all of the enq ports in, has as many priority muxes as required for the largest
   enq port bundle, and outputs the compacted enq port bundle.  The shifter takes this 
   compacted bundle and shifts it so that they get connected to the correct fifo banks
   outside of the module
*/
class CompactingEnqNetwork(val ports: List[Int], val banks: Int, val width: Int, val bitWidth: Int = 32) extends Module {
  val io = IO( new Bundle {
      val headCnt = Input(SInt(width.W))
      val in = Vec(1 max ports.sum, Input(new enqPort(bitWidth)))
      val out = Vec(1 max banks, Output(new enqPort(bitWidth)))
    })

  val numEnabled = io.in.map{i => Mux(i.en, 1.U(width.W), 0.U(width.W))}.reduce{_+_}
  val num_compactors = if (ports.size == 0) 1 else ports.max

  // Compactor
  val compactor = Module(new Compactor(ports, banks, width, bitWidth))
  compactor.io.in := io.in
  compactor.io.numEnabled := numEnabled

  // Router
  val current_base_bank = Math.singleCycleModulo(io.headCnt, banks.S(width.W))
  val upper = current_base_bank + numEnabled.asSInt - banks.S(width.W)
  val num_straddling = Mux(upper < 0.S(width.W), 0.S(width.W), upper)
  val num_straight = (numEnabled.asSInt) - num_straddling
  val outs = (0 until banks).map{ i =>
    val lane_enable = Mux(i.S(width.W) < num_straddling | (i.S(width.W) >= current_base_bank & i.S(width.W) < current_base_bank + numEnabled.asSInt), true.B, false.B)
    val id_from_base = Mux(i.S(width.W) < num_straddling, i.S(width.W) + num_straight, i.S(width.W) - current_base_bank)
    val port_vals = (0 until num_compactors).map{ i => 
      (i.U(width.W) -> compactor.io.out(i).data)
    }
    val lane_data = chisel3.util.MuxLookup(id_from_base.asUInt, 0.U(bitWidth.W), port_vals)
    (lane_data,lane_enable)
  }

  (0 until banks).foreach{i => 
    if (banks == 0) io.out(i).data := 0.U else io.out(i).data := outs(i)._1
    if (banks == 0) io.out(i).en := false.B else io.out(i).en := outs(i)._2
  }
}

class CompactingDeqNetwork(val ports: List[Int], val banks: Int, val width: Int, val bitWidth: Int = 32) extends Module {
  val io = IO( new Bundle {
      val tailCnt = Input(SInt(width.W))
      val input = new Bundle{
        val data = Vec(1 max banks, Input(UInt(bitWidth.W)))
        val deq = Vec(1 max ports.sum, Input(Bool()))
      }
      val output = Vec({if (ports.size == 0) 1 else ports.max}, Output(UInt(bitWidth.W)))
    })
  io.output := DontCare
  // Compactor
  val num_compactors = if (ports.size == 0) 1 else ports.max
  // val numPort_width = 1 + Utils.log2Up(ports.max)
  val numEnabled = io.input.deq.map{i => Mux(i, 1.U(width.W), 0.U(width.W))}.reduce{_+_}

  // Router
  val current_base_bank = Math.singleCycleModulo(io.tailCnt, banks.S(width.W))
  val upper = current_base_bank + numEnabled.asSInt - banks.S(width.W)
  val num_straddling = Mux(upper < 0.S(width.W), 0.S(width.W), upper)
  val num_straight = (numEnabled.asSInt) - num_straddling
  // TODO: Probably has a bug if you have more than one dequeuer
  (0 until {if (ports.size == 0) 1 else ports.max}).foreach{ i =>
    val id_from_base = Mux(i.S(width.W) < num_straddling, i.S(width.W) + num_straight, Math.singleCycleModulo((i.S(width.W) + current_base_bank), banks.S(width.W)))
    val ens_below = if (i>0) (0 until i).map{j => Mux(io.input.deq(j), 1.U(width.W), 0.U(width.W)) }.reduce{_+_} else 0.U(width.W)
    val proper_bank = Math.singleCycleModulo((current_base_bank.asUInt + ens_below), banks.U(width.W))
    val port_vals = (0 until banks).map{ j => 
      (j.U(width.W) -> io.input.data(j)) 
    }
    io.output(i) := chisel3.util.MuxLookup(proper_bank.asUInt, 0.U(bitWidth.W), port_vals)
  }

}
