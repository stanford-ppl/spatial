package fringe.templates.memory

import chisel3._
import chisel3.util._
import fringe._
import fringe.templates.counters.{CompactingCounter, CompactingIncDincCtr, IncDincCtr, SingleSCounterCheap}
import fringe.templates.math.Math
import fringe.utils._
import fringe.utils.XMap._
import fringe.utils.DMap._
import fringe.utils.HVec
import fringe.utils.{getRetimed, log2Up}
import fringe.utils.implicits._

class R_XBar(val port_width: Int, val ofs_width:Int, val bank_width:List[Int]) extends Bundle {
  val banks = HVec.tabulate(port_width*bank_width.length){i => UInt(bank_width(i%bank_width.length).W)}
  val ofs = Vec(port_width, UInt(ofs_width.W))
  val en = Vec(port_width, Bool())
  val flow = Vec(port_width, Bool())

  def connectLane(lhs_lane: Int, rhs_lane: Int, rhs_port: R_XBar, f: Bool): Unit = {
    ofs(lhs_lane) := rhs_port.ofs(rhs_lane)
    bank_width.length.indices[Unit]{i => banks(i + lhs_lane*bank_width.length) := rhs_port.banks(i + rhs_lane*bank_width.length)}
    en(lhs_lane) := rhs_port.en(rhs_lane)
    flow(lhs_lane) := f
  }

  override def cloneType = (new R_XBar(port_width, ofs_width, bank_width)).asInstanceOf[this.type] // See chisel3 bug 358
}

class W_XBar(val port_width: Int, val ofs_width:Int, val bank_width:List[Int], val data_width:Int) extends Bundle {
  val banks = HVec.tabulate(port_width*bank_width.length){i => UInt(bank_width(i%bank_width.length).W)}
  val ofs = Vec(port_width, UInt(ofs_width.W))
  val data = Vec(port_width, UInt(data_width.W))
  val reset = Vec(port_width, Bool()) // For FF
  val init = Vec(port_width, UInt(data_width.W)) // For FF
  val shiftEn = Vec(port_width, Bool()) // For ShiftRegFile
  val en = Vec(port_width, Bool())

  override def cloneType = (new W_XBar(port_width, ofs_width, bank_width, data_width)).asInstanceOf[this.type] // See chisel3 bug 358
}

class R_Direct(val port_width:Int, val ofs_width:Int, val banks:List[List[Int]]) extends Bundle {
  val ofs = Vec(port_width, UInt(ofs_width.W))
  val en = Vec(port_width, Bool())
  val flow = Vec(port_width, Bool())

  def connectLane(lhs_lane: Int, rhs_lane: Int, rhs_port: R_Direct, f: Bool): Unit = {
    ofs(lhs_lane) := rhs_port.ofs(rhs_lane)
    en(lhs_lane) := rhs_port.en(rhs_lane)
    flow(lhs_lane) := f
  }

  override def cloneType = (new R_Direct(port_width, ofs_width, banks)).asInstanceOf[this.type] // See chisel3 bug 358
}

class W_Direct(val port_width: Int, val ofs_width:Int, val banks:List[List[Int]], val data_width:Int) extends Bundle {
  val ofs = Vec(port_width, UInt(ofs_width.W))
  val data = Vec(port_width, UInt(data_width.W))
  val shiftEn = Vec(port_width, Bool()) // For ShiftRegFile
  val en = Vec(port_width, Bool())

  override def cloneType = (new W_Direct(port_width, ofs_width, banks, data_width)).asInstanceOf[this.type] // See chisel3 bug 358
}


abstract class MemPrimitive(val p: MemParams) extends Module {
  val io = p.iface match {
    case StandardInterface => IO(new StandardInterface(p))
    case ShiftRegFileInterface => IO(new ShiftRegFileInterface(p))
    case FIFOInterface => IO(new FIFOInterface(p))
  } 

  override def desiredName = p.myName

  var usedMuxPorts = List[(String,(Int,Int,Int,Int))]() // Check if the muxPort, muxAddr, lane, castgrp is taken for this connection style (xBar or direct)
  def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxAddr: (Int, Int)): Unit = {
    assert(p.hasXBarW)
    assert(p.xBarWMux.contains((muxAddr._1,muxAddr._2,0)))
    assert(!usedMuxPorts.contains(("XBarW", (muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to XBarW port $muxAddr twice!")
    usedMuxPorts ::= ("XBarW", (muxAddr._1,muxAddr._2,0,0))
    val base = p.xBarWMux.accessParsBelowMuxPort(muxAddr._1,muxAddr._2,0).size
    io.xBarW(base) := wBundle
  }

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, flow: Bool): Seq[UInt] = {
    assert(p.hasXBarR)
    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
      val castgrp = if (ignoreCastInfo) 0 else cg
      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
      val base = p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,effectiveOfs,castgrp).size
      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
      val outBase = p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,effectiveOfs,castgrp).sum - p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum
      if (bid == 0) {
        if (ignoreCastInfo && i == 0) {
          assert(p.xBarRMux.contains((muxAddr._1, effectiveOfs, 0)))
          assert(!usedMuxPorts.contains(("XBarR", (muxAddr._1,effectiveOfs, i, 0))), s"Attempted to connect to XBarR port $muxAddr, castgrp $castgrp on lane $i twice!")
          usedMuxPorts ::= ("XBarR", (muxAddr._1,effectiveOfs, i, 0))
        } else if (!ignoreCastInfo) {
          assert(p.xBarRMux.contains((muxAddr._1, effectiveOfs, castgrp)))
          assert(!usedMuxPorts.contains(("XBarR", (muxAddr._1,effectiveOfs, i, castgrp))), s"Attempted to connect to XBarR port $muxAddr, castgrp $castgrp on lane $i twice!")
          usedMuxPorts ::= ("XBarR", (muxAddr._1,effectiveOfs, i, castgrp))
        }
        io.xBarR(base).connectLane(vecId, i, rBundle, flow)
      }
      // Temp fix for merged readers not recomputing port info
      io.output.data(outBase + vecId)
    }
    
  }

  def connectDirectWPort(wBundle: W_Direct, bufferPort: Int, muxAddr: (Int, Int)): Unit = {
    assert(p.hasDirectW)
    assert(p.directWMux.contains((muxAddr._1,muxAddr._2,0)))
    assert(!usedMuxPorts.contains(("DirectW", (muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to DirectW port $muxAddr twice!")
    usedMuxPorts ::= ("DirectW", (muxAddr._1,muxAddr._2,0,0))
    val base = p.directWMux.accessParsBelowMuxPort(muxAddr._1,muxAddr._2,0).size
    io.directW(base) := wBundle
  }

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean): Seq[UInt] = {connectDirectRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, flow: Bool): Seq[UInt] = {
    assert(p.hasDirectR)
    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
      val castgrp = if (ignoreCastInfo) 0 else cg
      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
      val base = p.directRMux.accessParsBelowMuxPort(muxAddr._1,effectiveOfs, castgrp).size
      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
      val outBase = p.directRMux.accessParsBelowMuxPort(muxAddr._1,effectiveOfs,castgrp).sum - p.directRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum
      if (bid == 0) {
        if (ignoreCastInfo && i == 0) {
          assert(p.directRMux.contains((muxAddr._1, effectiveOfs, 0)))
          assert(!usedMuxPorts.contains(("DirectR", (muxAddr._1,effectiveOfs, i, 0))), s"Attempted to connect to DirectR port $muxAddr, castgrp $castgrp on lane $i twice!")
          usedMuxPorts ::= ("DirectR", (muxAddr._1,effectiveOfs, i, 0))
        } else if (!ignoreCastInfo) {
          assert(p.directRMux.contains((muxAddr._1, effectiveOfs, castgrp)))
          assert(!usedMuxPorts.contains(("DirectR", (muxAddr._1,effectiveOfs, i, castgrp))), s"Attempted to connect to DirectR port $muxAddr, castgrp $castgrp on lane $i twice!")
          usedMuxPorts ::= ("DirectR", (muxAddr._1,effectiveOfs, i, castgrp))
        }
        io.directR(base).connectLane(vecId, i, rBundle, flow)
      }
      // Temp fix for merged readers not recomputing port info
      io.output.data(p.xBarOutputs + outBase + vecId)
    }
  }
}


class BankedSRAM(p: MemParams) extends MemPrimitive(p) {

  def this(logicalDims: List[Int], bitWidth: Int, banks: List[Int], strides: List[Int],
           xBarWMux: XMap, xBarRMux: XMap, directWMux: DMap, directRMux: DMap,
           bankingMode: BankingMode, inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, myName: String = "sram") = this(MemParams(StandardInterface, logicalDims,bitWidth,banks,strides,xBarWMux,xBarRMux,directWMux,directRMux,bankingMode,inits,syncMem,fracBits, myName = myName))
  def this(tuple: (List[Int], Int, List[Int], List[Int], XMap, XMap,
    DMap, DMap, BankingMode)) = this(MemParams(StandardInterface,tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7, tuple._8, tuple._9))

  // Get info on physical dims
  // TODO: Upcast dims to evenly bank
  val numMems = p.bankingMode match {
    case DiagonalMemory => p.banks.head
    case BankedMemory => p.banks.product
  }
  val bankDim = p.bankingMode match {
    case DiagonalMemory => math.ceil(p.depth.toDouble / numMems.toDouble).toInt //logicalDims.zipWithIndex.map { case (dim, i) => if (i == N - 1) math.ceil(dim.toDouble/banks.head).toInt else dim}
    case BankedMemory => math.ceil(p.depth.toDouble / numMems.toDouble).toInt
  }

  // Create list of (mem: Mem1D, coords: List[Int] <coordinates of bank>)
  val m = (0 until numMems).map{ i =>
    val mem = Module(new Mem1D(bankDim, p.bitWidth, p.syncMem))
    val coords = p.banks.zipWithIndex.map{ case (b,j) =>
      i % (p.banks.drop(j).product) / p.banks.drop(j+1).product
    }
    (mem,coords)
  }

  // Handle Writes
  m.foreach{ mem =>
    // Check all xBar w ports against this bank's coords
    val xBarSelect = io.xBarW.flatMap(_.banks).grouped(p.banks.length).toList.zip(io.xBarW.flatMap(_.en)).map{ case(bids, en) =>
      bids.zip(mem._2).map{case (b,coord) => b === coord.U}.reduce{_&&_} & {if (p.hasXBarW) en else false.B}
    }
    // Check all direct W ports against this bank's coords
    val directSelectEns = io.directW.flatMap(_.en).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectDatas = io.directW.flatMap(_.data).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectOffsets = io.directW.flatMap(_.ofs).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}

    // Unmask write port if any of the above match
    mem._1.io.wMask := xBarSelect.reduce{_|_} | {directSelectEns.or}

    // Connect matching W port to memory
    if (directSelectEns.nonEmpty & p.hasXBarW) {           // Has direct and x
      val directChoice = fatMux("PriorityMux", directSelectEns, directSelectEns, directSelectDatas, directSelectOffsets)
      val xBarChoice = fatMux("PriorityMux", xBarSelect, io.xBarW.flatMap(_.en).toList, io.xBarW.flatMap(_.data).toList, io.xBarW.flatMap(_.ofs).toList)
      val finalChoice = fatMux("PriorityMux", Seq(directSelectEns.or), Seq(directChoice(0), xBarChoice(0)), Seq(directChoice(1), xBarChoice(1)), Seq(directChoice(2), xBarChoice(2))) 
      mem._1.io.w.ofs.head  := finalChoice(2)
      mem._1.io.w.data.head := finalChoice(1)
      mem._1.io.w.en.head   := finalChoice(0)
    }
    else if (p.hasXBarW && directSelectEns.isEmpty) {  // Has x only
      val xBarChoice = fatMux("PriorityMux", xBarSelect, io.xBarW.flatMap(_.en).toList, io.xBarW.flatMap(_.data).toList, io.xBarW.flatMap(_.ofs).toList)
      mem._1.io.w.ofs.head  := xBarChoice(2)
      mem._1.io.w.data.head := xBarChoice(1)
      mem._1.io.w.en.head   := xBarChoice(0)
    }
    else if (directSelectEns.nonEmpty) {               // Has direct only
      val directChoice = fatMux("PriorityMux", directSelectEns, directSelectEns, directSelectDatas, directSelectOffsets)
      mem._1.io.w.ofs.head  := directChoice(2)
      mem._1.io.w.data.head := directChoice(1)
      mem._1.io.w.en.head   := directChoice(0)
    }
  }


  // Handle Reads
  m.zipWithIndex.foreach{ case(mem,ii) =>
    // Check all xBar r ports against this bank's coords
    val xBarSelectEnsRaw = io.xBarR.flatMap(_.banks).grouped(p.banks.length).toList.zip(io.xBarR.flatMap(_.en)).map{ case(bids, en) =>
      bids.zip(mem._2).map{case (b,coord) => b === coord.U}.reduce{_&&_} & {if (p.hasXBarR) en else false.B}
    }
    val xBarSelectEnsModule = Module(new StickySelects(xBarSelectEnsRaw.size))
    xBarSelectEnsModule.io.ins.zip(xBarSelectEnsRaw).foreach{case (a,b) => a := b}
    val xBarSelect = xBarSelectEnsRaw //xBarSelectEnsModule.io.outs.map(_.toBool)
    val directSelectEnsRaw = io.directR.flatMap(_.en).zip(io.directR.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectEnsModule = Module(new StickySelects(directSelectEnsRaw.size))
    directSelectEnsModule.io.ins.zip(directSelectEnsRaw).foreach{case (a,b) => a := b}
    val directSelectEns = directSelectEnsModule.io.outs.map(_.toBool)
    val directSelectOffsets = io.directR.flatMap(_.ofs).zip(io.directR.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectFlows = io.directR.flatMap(_.flow).zip(io.directR.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}

    // Unmask write port if any of the above match
    mem._1.io.rMask := {if (p.hasXBarR) xBarSelect.reduce{_|_} else true.B} & directSelectEns.or
    // Connect matching R port to memory
    if (directSelectEns.nonEmpty & p.hasXBarR) {          // Has direct and x
      val directChoice = fatMux("PriorityMux", directSelectEns, directSelectOffsets, directSelectEns, directSelectFlows)
      val xBarChoice = fatMux("PriorityMux", xBarSelect, io.xBarR.flatMap(_.ofs), io.xBarR.flatMap(_.en), io.xBarR.flatMap(_.flow))
      val finalChoice = fatMux("PriorityMux", Seq(directSelectEns.or), Seq(directChoice(0), xBarChoice(0)), Seq(directChoice(1), xBarChoice(1)), Seq(directChoice(2), xBarChoice(2))) 
      mem._1.io.r.ofs.head  := finalChoice(0)
      mem._1.io.r.en.head   := finalChoice(1)
      mem._1.io.flow        := finalChoice(2)
    }
    else if (p.hasXBarR && directSelectEns.isEmpty) { // Has x only
      val xBarChoice = fatMux("PriorityMux", xBarSelect, io.xBarR.flatMap(_.ofs), io.xBarR.flatMap(_.en), io.xBarR.flatMap(_.flow))
      mem._1.io.r.ofs.head  :=  xBarChoice(0)
      mem._1.io.r.en.head   :=  xBarChoice(1)
      mem._1.io.flow         :=  xBarChoice(2)
    }
    else if (directSelectEns.nonEmpty) {                                           // Has direct only
      val directChoice = fatMux("PriorityMux", directSelectEns, directSelectOffsets, directSelectEns, directSelectFlows)
      mem._1.io.r.ofs.head  := directChoice(0)
      mem._1.io.r.en.head   := directChoice(1)
      mem._1.io.flow         := directChoice(2)
    }
  }

  // Connect read data to output
  io.output.data.zipWithIndex.foreach { case (wire,i) =>
    if (p.xBarRMux.nonEmpty && i < p.xBarOutputs) {
      val xBarIds = p.xBarRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if i < entry._1 => p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }
      val xBarCandidatesEns = xBarIds.map(io.xBarR.flatMap(_.en).toList(_)).toList
      val xBarCandidatesFlows = xBarIds.map(io.xBarR.flatMap(_.flow).toList(_)).toList
      val xBarCandidatesBanks = xBarIds.map(io.xBarR.flatMap(_.banks).toList.grouped(p.banks.length).toList(_)).toList
      val xBarCandidatesOffsets = xBarIds.map(io.xBarR.flatMap(_.ofs).toList(_)).toList
      val sel = m.map{ mem =>
        if (xBarCandidatesEns.nonEmpty) xBarCandidatesEns.size.indices{j => 
          val en = xBarCandidatesEns(j)
          val banks = xBarCandidatesBanks(j)
          val ofs = xBarCandidatesOffsets(j)
          val flow = xBarCandidatesFlows(j)
          getRetimed(banks.zip(mem._2).map{case (b, coord) => b === coord.U}.reduce{_&&_} && en, globals.target.sramload_latency, flow)
          // banks.zip(mem._2).map{case (b, coord) => getRetimed(b, globals.target.sramload_latency, flow) === coord.U}.reduce{_&&_} &&
          //   getRetimed(en, globals.target.sramload_latency, flow)
        }.reduce{_||_} else false.B
      }
      val datas = m.map{ _._1.io.output.data }
      val d = chisel3.util.PriorityMux(sel, datas) // Not sure how to connect flows
      wire := d
    } else {
      // Figure out which read port was active in direct
      val directIds = p.directRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i - p.xBarOutputs < entry._1.length) => p.directRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i - p.xBarOutputs }
      val directCandidatesEns = directIds.map(io.directR.flatMap(_.en).toList(_)).toList
      val directCandidatesBanks = directIds.map(io.directR.flatMap(_.banks).toList(_)).toList
      val directCandidatesOffsets = directIds.map(io.directR.flatMap(_.ofs).toList(_)).toList
      val directCandidatesFlows = directIds.map(io.directR.flatMap(_.flow).toList(_)).toList
      // Create bit vector to select which bank was activated by this io
      val sel = m.map{ mem =>
        if (directCandidatesEns.nonEmpty) directCandidatesEns.size.indices{j => 
          val en = directCandidatesEns(j)
          val banks = directCandidatesBanks(j)
          val ofs = directCandidatesOffsets(j)
          val flow = directCandidatesFlows(j)
          banks.zip(mem._2).map{case (b, coord) => b == coord}.reduce{_&&_}.B && getRetimed(en, globals.target.sramload_latency, flow)
        }.reduce{_||_} else false.B
      }
      val datas = m.map{ _._1.io.output.data }
      val d = chisel3.util.PriorityMux(sel, datas)
      wire := d
    }
  }

}  

class FF(p: MemParams) extends MemPrimitive(p) {
  // Compatibility with standard mem codegen
  def this(logicalDims: List[Int], bitWidth: Int,
           banks: List[Int], strides: List[Int],
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int, myName: String = "FF") = this(MemParams(StandardInterface, logicalDims, bitWidth, banks, strides, xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, init, syncMem, fracBits, myName = myName))
  // def this(logicalDims: List[Int], bitWidth: Int,
  //          banks: List[Int], strides: List[Int],
  //          xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
  //          directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
  //          bankingMode: BankingMode, init: => Option[List[Int]], syncMem: Boolean, fracBits: Int) = this(MemParams(logicalDims, bitWidth, banks, strides, xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, {if (init.isDefined) Some(init.get.map(_.toDouble)) else None}, syncMem, fracBits))
  def this(tuple: (Int, XMap)) = this(List(1), tuple._1,List(1), List(1), tuple._2, XMap((0,0,0) -> (1, None)), DMap(), DMap(), BankedMemory, None, false, 0)
  def this(bitWidth: Int) = this(List(1), bitWidth,List(1), List(1), XMap((0,0,0) -> (1, None)), XMap((0,0,0) -> (1, None)), DMap(), DMap(), BankedMemory, None, false, 0)
  def this(bitWidth: Int, xBarWMux: XMap, xBarRMux: XMap, inits: Option[List[Double]], fracBits: Int, myName: String) = this(List(1), bitWidth,List(1), List(1), xBarWMux, xBarRMux, DMap(), DMap(), BankedMemory, inits, false, fracBits, myName)

  val init = 
    if (p.inits.isDefined) {
      if (p.bitWidth == 1) {if (p.inits.get.head == 0.0) false.B else true.B}
      else                 (p.inits.get.head*scala.math.pow(2,p.fracBits)).toLong.S(p.bitWidth.W).asUInt
    }
    else io.xBarW(0).init.head

  val ff = RegInit(init)
  val anyReset: Bool = io.xBarW.flatMap{_.reset}.toList.reduce{_|_} | io.reset
  val anyEnable: Bool = io.xBarW.flatMap{_.en}.toList.reduce{_|_}
  val wr_data: UInt = chisel3.util.PriorityMux(io.xBarW.flatMap{_.en}.toList, io.xBarW.flatMap{_.data}.toList)
  ff := Mux(anyReset, init, Mux(anyEnable, wr_data, ff))
  io.output.data.foreach(_ := ff)
}

class FIFOReg(p: MemParams) extends MemPrimitive(p) {
  // Compatibility with standard mem codegen
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: List[Int], strides: List[Int], 
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int, myName: String = "FIFOReg") = this(MemParams(FIFOInterface, logicalDims, bitWidth, banks, strides, xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, init, syncMem, fracBits, myName = myName))
  // def this(logicalDims: List[Int], bitWidth: Int, 
  //          banks: List[Int], strides: List[Int], 
  //          xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
  //          directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
  //          bankingMode: BankingMode, init: => Option[List[Int]], syncMem: Boolean, fracBits: Int) = this(MemParams(logicalDims, bitWidth, banks, strides, xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, {if (init.isDefined) Some(init.get.map(_.toDouble)) else None}, syncMem, fracBits))
  def this(tuple: (Int, XMap)) = this(List(1), tuple._1,List(1), List(1), tuple._2, XMap((0,0,0) -> (1, None)), DMap(), DMap(), BankedMemory, None, false, 0)
  def this(bitWidth: Int) = this(List(1), bitWidth,List(1), List(1), XMap((0,0,0) -> (1, None)), XMap((0,0,0) -> (1, None)), DMap(), DMap(), BankedMemory, None, false, 0)
  def this(bitWidth: Int, xBarWMux: XMap, xBarRMux: XMap, inits: Option[List[Double]], fracBits: Int) = this(List(1), bitWidth,List(1), List(1), xBarWMux, xBarRMux, DMap(), DMap(), BankedMemory, inits, false, fracBits)

  val init = 
    if (p.inits.isDefined) {
      if (p.bitWidth == 1) {if (p.inits.get.head == 0.0) false.B else true.B}
      else                 (p.inits.get.head*scala.math.pow(2,p.fracBits)).toLong.S(p.bitWidth.W).asUInt
    }
    else io.xBarW(0).init.head

  val ff = RegInit(init)

  val anyReset: Bool = io.xBarW.map{_.reset}.flatten.toList.reduce{_|_} | io.reset
  val anyWrite: Bool = io.xBarW.map{_.en}.flatten.toList.reduce{_|_}
  val anyRead: Bool = io.xBarR.map{_.en}.flatten.toList.reduce{_|_}
  val wr_data: UInt = chisel3.util.PriorityMux(io.xBarW.map{_.en}.flatten.toList, io.xBarW.map{_.data}.flatten.toList)
  ff := Mux(anyReset, init, Mux(anyWrite, wr_data, ff))
  io.output.data.foreach(_ := ff)

  val isValid = Module(new SRFF())
  isValid.io.input.set := anyWrite
  isValid.io.input.reset := anyRead
  isValid.io.input.asyn_reset := false.B

  // Check if there is data
  io.asInstanceOf[FIFOInterface].empty := ~isValid.io.output.data
  io.asInstanceOf[FIFOInterface].full := isValid.io.output.data
  io.asInstanceOf[FIFOInterface].almostEmpty := false.B
  io.asInstanceOf[FIFOInterface].almostFull := false.B
  io.asInstanceOf[FIFOInterface].numel := Mux(isValid.io.output.data, 1.U, 0.U)

}

class FIFO(p: MemParams) extends MemPrimitive(p) {
  def this(logicalDims: List[Int], bitWidth: Int,
           banks: List[Int], xBarWMux: XMap, xBarRMux: XMap,
           inits: Option[List[Double]], syncMem: Boolean, fracBits: Int) = this(MemParams(FIFOInterface,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, DMap(), DMap(), BankedMemory, inits, syncMem, fracBits, myName = "FIFO"))

  def this(tuple: (List[Int], Int, List[Int], XMap, XMap)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, None, false, 0)
  def this(logicalDims: List[Int], bitWidth: Int,
           banks: List[Int], strides: List[Int],
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int, myName: String = "FIFO") = this(MemParams(FIFOInterface,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, init, syncMem, fracBits, myName = myName))

  // Create bank counters
  val headCtr = Module(new CompactingCounter(p.numXBarW, p.depth, p.elsWidth))
  val tailCtr = Module(new CompactingCounter(p.numXBarR, p.depth, p.elsWidth))
  (0 until p.numXBarW).foreach{i => headCtr.io.input.enables.zip(io.xBarW.flatMap(_.en)).foreach{case (l,r) => l := r}}
  (0 until p.numXBarR).foreach{i => tailCtr.io.input.enables.zip(io.xBarR.flatMap(_.en)).foreach{case (l,r) => l := r}}
  headCtr.io.input.reset := reset
  tailCtr.io.input.reset := reset
  headCtr.io.input.dir := true.B
  tailCtr.io.input.dir := true.B

  // Create numel counter
  val elements = Module(new CompactingIncDincCtr(p.numXBarW, p.numXBarR, p.depth, p.elsWidth))
  elements.io.input.inc_en.zip(io.xBarW.flatMap(_.en)).foreach{case(l,r) => l := r}
  elements.io.input.dinc_en.zip(io.xBarR.flatMap(_.en)).foreach{case(l,r) => l := r}

  // Create physical mems
  val m = (0 until p.numBanks).map{ i => Module(new Mem1D(p.depth/p.numBanks, p.bitWidth))}

  // Create compacting network

  val enqCompactor = Module(new CompactingEnqNetwork(p.xBarWMux.sortByMuxPortAndCombine.values.map(_._1).toList, p.numBanks, p.elsWidth, p.bitWidth))
  enqCompactor.io.headCnt := headCtr.io.output.count
  (0 until p.numXBarW).foreach{i =>
    enqCompactor.io.in.map(_.data).zip(io.xBarW.flatMap(_.data)).foreach{case (l,r) => l := r}
    enqCompactor.io.in.map(_.en).zip(io.xBarW.flatMap(_.en)).foreach{case(l,r) => l := r}
  }

  // Connect compacting network to banks
  val active_w_bank = Math.singleCycleModulo(headCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  val active_w_addr = Math.singleCycleDivide(headCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  (0 until p.numBanks).foreach{i =>
    val addr = Mux(i.S(p.elsWidth.W) < active_w_bank, active_w_addr + 1.S(p.elsWidth.W), active_w_addr)
    m(i).io.w.ofs.head := addr.asUInt
    m(i).io.w.data.head := enqCompactor.io.out(i).data
    m(i).io.w.en.head   := enqCompactor.io.out(i).en
    m(i).io.wMask  := enqCompactor.io.out(i).en
  }

  // Create dequeue compacting network
  val deqCompactor = Module(new CompactingDeqNetwork(p.xBarRMux.sortByMuxPortAndCombine.values.map(_._1).toList, p.numBanks, p.elsWidth, p.bitWidth))
  deqCompactor.io.tailCnt := tailCtr.io.output.count
  val active_r_bank = Math.singleCycleModulo(tailCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  val active_r_addr = Math.singleCycleDivide(tailCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  (0 until p.numBanks).foreach{i =>
    val addr = Mux(i.S(p.elsWidth.W) < active_r_bank, active_r_addr + 1.S(p.elsWidth.W), active_r_addr)
    m(i).io.r.ofs.head := addr.asUInt
    deqCompactor.io.input.data(i) := m(i).io.output.data
  }
  (0 until p.numXBarR).foreach{i =>
    deqCompactor.io.input.deq.zip(io.xBarR.flatMap(_.en)).foreach{case (l,r) => l := r}
  }
  (0 until p.xBarRMux.accessPars.max).foreach{i =>
    io.output.data(i) := deqCompactor.io.output.data(i)
  }

  // Check if there is data
  io.asInstanceOf[FIFOInterface].empty := elements.io.output.empty
  io.asInstanceOf[FIFOInterface].full := elements.io.output.full
  io.asInstanceOf[FIFOInterface].almostEmpty := elements.io.output.almostEmpty
  io.asInstanceOf[FIFOInterface].almostFull := elements.io.output.almostFull
  io.asInstanceOf[FIFOInterface].numel := elements.io.output.numel.asUInt


}

class LIFO(p: MemParams) extends MemPrimitive(p) {

  def this(logicalDims: List[Int], bitWidth: Int,
           banks: List[Int],
           xBarWMux: XMap, xBarRMux: XMap,
           inits: Option[List[Double]], syncMem: Boolean, fracBits: Int) = this(MemParams(FIFOInterface,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, DMap(), DMap(), BankedMemory, inits, syncMem, fracBits, myName = "LIFO"))
  def this(tuple: (List[Int], Int, List[Int], XMap, XMap)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, None, false, 0)
  def this(logicalDims: List[Int], bitWidth: Int,
           banks: List[Int], strides: List[Int],
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int, myName: String = "LIFO") = this(MemParams(FIFOInterface,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, init, syncMem, fracBits, myName = myName))

  val pW = p.xBarWMux.accessPars.max
  val pR = p.xBarRMux.accessPars.max
  val par = scala.math.max(pW, pR) // TODO: Update this template because this was from old style

  // Register for tracking number of elements in FILO
  val elements = Module(new IncDincCtr(pW,pR, p.depth))
  elements.io.input.inc_en := io.xBarW.flatMap(_.en).toList.reduce{_|_}
  elements.io.input.dinc_en := io.xBarR.flatMap(_.en).toList.reduce{_|_}

  // Create physical mems
  val m = (0 until par).map{ i => Module(new Mem1D(p.depth/par, p.bitWidth))}

  // Create head and reader sub counters
  val sa_width = 2 + log2Up(par)
  val subAccessor = Module(new SingleSCounterCheap(1,0,par,pW,-pR,0,sa_width))
  subAccessor.io.input.enable := io.xBarW.flatMap(_.en).toList.reduce{_|_} | io.xBarR.flatMap(_.en).toList.reduce{_|_}
  subAccessor.io.input.dir := io.xBarW.flatMap(_.en).toList.reduce{_|_}
  subAccessor.io.input.reset := reset
  subAccessor.io.input.saturate := false.B
  val subAccessor_prev = Mux(subAccessor.io.output.count(0) - pR.S(sa_width.W) < 0.S(sa_width.W), (par-pR).S(sa_width.W), subAccessor.io.output.count(0) - pR.S(sa_width.W))

  // Create head and reader counters
  val a_width = 2 + log2Up(p.depth/par)
  val accessor = Module(new SingleSCounterCheap(1, 0, (p.depth/par), 1, -1, 0, a_width))
  accessor.io.input.enable := (io.xBarW.flatMap(_.en).toList.reduce{_|_} & subAccessor.io.output.done) | (io.xBarR.flatMap(_.en).toList.reduce{_|_} & subAccessor_prev === 0.S(sa_width.W))
  accessor.io.input.dir := io.xBarW.flatMap(_.en).toList.reduce{_|_}
  accessor.io.input.reset := reset
  accessor.io.input.saturate := false.B


  // Connect pusher
  if (pW == pR) {
    m.zipWithIndex.foreach { case (mem, i) =>
      // Figure out which write port was active in xBar
      val xBarIds = p.xBarWMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarWMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }.toList
      val xBarCandidatesEns = xBarIds.map{n => io.xBarW.flatMap(_.en).toList(n+i)}
      val xBarCandidatesDatas = xBarIds.map{n => io.xBarW.flatMap(_.data).toList(n+i)}

      // Make connections to memory
      mem.io.w.ofs.head := accessor.io.output.count(0).asUInt
      mem.io.w.data.head := PriorityMux(xBarCandidatesEns, xBarCandidatesDatas)
      mem.io.w.en.head := xBarCandidatesEns.or
      mem.io.wMask := xBarCandidatesEns.or
    }
  } else {
    (0 until pW).foreach { w_i =>
      (0 until (par / pW)).foreach { i =>
        // Figure out which write port was active in xBar
        val xBarIds = p.xBarWMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarWMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }.toList
        val xBarCandidatesEns = xBarIds.map{n => io.xBarW.flatMap(_.en).toList(n+(i*pW+w_i))}
        val xBarCandidatesDatas = xBarIds.map{n => io.xBarW.flatMap(_.data).toList(n+(i*pW+w_i))}

        // Make connections to memory
        m(w_i + i*pW).io.w.ofs.head := accessor.io.output.count(0).asUInt
        m(w_i + i*pW).io.w.data.head := PriorityMux(xBarCandidatesEns, xBarCandidatesDatas)
        m(w_i + i*pW).io.w.en.head := xBarCandidatesEns.or & (subAccessor.io.output.count(0) === (i*pW).S(sa_width.W))
        m(w_i + i*pW).io.wMask := xBarCandidatesEns.or & (subAccessor.io.output.count(0) === (i*pW).S(sa_width.W))
      }
    }
  }

  // Connect popper
  if (pW == pR) {
    m.zipWithIndex.foreach { case (mem, i) =>
      mem.io.r.ofs.head := (accessor.io.output.count(0) - 1.S(a_width.W)).asUInt
      mem.io.r.en.head := io.xBarR.flatMap(_.en).toList.reduce{_|_}
      mem.io.rMask := io.xBarR.flatMap(_.en).toList.reduce{_|_}
      io.output.data(i) := mem.io.output.data
    }
  } else {
    (0 until pR).foreach { r_i =>
      val rSel = Wire(Vec( (par/pR), Bool()))
      val rData = Wire(Vec( (par/pR), UInt(p.bitWidth.W)))
      (0 until (par / pR)).foreach { i =>
        m(r_i + i*pR).io.r.ofs.head := (accessor.io.output.count(0) - 1.S(sa_width.W)).asUInt
        m(r_i + i*pR).io.r.en.head := io.xBarR.flatMap(_.en).toList.reduce{_|_} & (subAccessor_prev === (i*pR).S(sa_width.W))
        m(r_i + i*pR).io.rMask := io.xBarR.flatMap(_.en).toList.reduce{_|_} & (subAccessor_prev === (i*pR).S(sa_width.W))
        rSel(i) := subAccessor_prev === i.S
        rData(i) := m(r_i + i*pR).io.output.data
      }
      io.output.data(pR - 1 - r_i) := chisel3.util.PriorityMux(rSel, rData)
    }
  }

  // Check if there is data
  io.asInstanceOf[FIFOInterface].empty := elements.io.output.empty
  io.asInstanceOf[FIFOInterface].full := elements.io.output.full
  io.asInstanceOf[FIFOInterface].almostEmpty := elements.io.output.almostEmpty
  io.asInstanceOf[FIFOInterface].almostFull := elements.io.output.almostFull
  io.asInstanceOf[FIFOInterface].numel := elements.io.output.numel.asUInt

}

class ShiftRegFile(p: MemParams) extends MemPrimitive(p) {
  def this(logicalDims: List[Int], bitWidth: Int,
            xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
            directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
            inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, isBuf: Boolean, myName: String) = this(MemParams(ShiftRegFileInterface,logicalDims, bitWidth, logicalDims, List(1), xBarWMux, xBarRMux, directWMux, directRMux, BankedMemory, inits, syncMem, fracBits, isBuf, myName = myName))

  def this(tuple: (List[Int], Int, XMap, XMap, DMap, DMap, Option[List[Double]], Boolean, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9, false, "SR")
  def this(tuple: (List[Int], Int, XMap, XMap, DMap, DMap)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, None, false, 0, false, "SR")
  def this(logicalDims: List[Int], bitWidth: Int,
           banks: List[Int], strides: List[Int],
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int, myName: String = "SR") = this(logicalDims, bitWidth, xBarWMux, xBarRMux, directWMux, directRMux, init, syncMem, fracBits, false, myName)


  // Create list of (mem: Mem1D, coords: List[Int] <coordinates of bank>)
  val m = (0 until p.depth).map{ i =>
    val coords = p.logicalDims.zipWithIndex.map{ case (b,j) =>
      i % (p.logicalDims.drop(j).product) / p.logicalDims.drop(j+1).product
    }
    val initval = if (p.inits.isDefined) (p.inits.get.apply(i)*scala.math.pow(2,p.fracBits)).toLong.U(p.bitWidth.W) else 0.U(p.bitWidth.W)
    val mem = RegInit(initval)
    io.asInstanceOf[ShiftRegFileInterface].dump_out(i) := mem
    (mem,coords,i,initval)
  }

  def stripCoord(l: List[Int], x: Int): List[Int] = {l.take(x) ++ l.drop(x+1)}
  def stripCoord(l: HVec[UInt], x: Int): HVec[UInt] = {HVec(l.take(x) ++ l.drop(x+1))}
  def decrementAxisCoord(l: List[Int], x: Int): List[Int] = {l.take(x) ++ List(l(x) - 1) ++ l.drop(x+1)}
  // Handle Writes
  m.foreach{ case(mem, coords, flatCoord, initval) =>
    // Check all xBar w ports against this bank's coords
    val xBarSelect = (io.xBarW.flatMap(_.banks).toList.grouped(p.banks.length).toList, io.xBarW.flatMap(_.en).toList, io.xBarW.flatMap(_.shiftEn).toList).zipped.map{ case(bids, en, shiftEn) =>
      bids.zip(coords).map{case (b,coord) => b === coord.U}.reduce{_&&_} & {if (p.hasXBarW) en | shiftEn else false.B}
    }
    // Check all direct W ports against this bank's coords
    val directSelectEns = io.directW.flatMap(_.en).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectDatas = io.directW.flatMap(_.data).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectOffsets = io.directW.flatMap(_.ofs).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectShiftEns = io.directW.flatMap(_.shiftEn).zip(io.directW.flatMap(_.banks.flatten).grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}

    // Unmask write port if any of the above match
    val wMask = xBarSelect.reduce{_|_} | directSelectEns.or | directSelectShiftEns.or

    // Check if shiftEn is turned on for this line, guaranteed false on entry plane
    val shiftMask = if (p.axis >= 0 && coords(p.axis) != 0) {
      // XBarW requests shift
      val axisShiftXBar = io.xBarW.flatMap(_.banks).toList.grouped(p.banks.length).toList.zip(io.xBarW.flatMap(_.shiftEn).toList).map{ case(bids, en) =>
        bids.zip(coords).zipWithIndex.map{case ((b, coord),id) => if (id == p.axis) true.B else b === coord.U}.reduce{_&&_} & {if (p.hasXBarW) en else false.B}
      }
      // // DirectW requests shift
      // val axisShiftDirect = io.directW.map(_.banks).flatten.filter{case banks => stripCoord(banks, p.axis).zip(stripCoord(coords, p.axis)).map{case (b,coord) => b == coord}.reduce(_&_)}

      // Unmask shift if any of the above match
      axisShiftXBar.reduce{_|_} | directSelectShiftEns.or
    } else false.B

    // Connect matching W port to memory
    val shiftSource = if (p.axis >= 0 && coords(p.axis) != 0) m.filter{case (_,c,_,_) => decrementAxisCoord(coords,p.axis) == c}.head._1 else mem
    val shiftEnable = if (p.axis >= 0 && coords(p.axis) != 0) shiftMask else false.B
    val (data, enable) =
      if (directSelectEns.nonEmpty & p.hasXBarW) {           // Has direct and x
        val liveDirectWireSelects = directSelectEns.zip(directSelectShiftEns).map{case (e,se) => e | se}
        val liveDirectWireData = chisel3.util.PriorityMux(liveDirectWireSelects, directSelectDatas)
        val liveDirectWireEn = chisel3.util.PriorityMux(liveDirectWireSelects, liveDirectWireSelects)
        val liveXBarWireEn = chisel3.util.PriorityMux(xBarSelect, io.xBarW.flatMap(_.en).zip(io.xBarW.flatMap(_.shiftEn)).map{case(e,se) => e | se})
        val liveXBarWireData = chisel3.util.PriorityMux(xBarSelect, io.xBarW.flatMap(_.data))
        val enable = Mux(liveDirectWireSelects.or, liveDirectWireEn, liveXBarWireEn) & wMask
        val data = Mux(liveDirectWireSelects.or, liveDirectWireData, liveXBarWireData)
        (data, enable)
      }
      else if (p.hasXBarW && directSelectEns.isEmpty) {  // Has x only
        val liveXBarWireEn = chisel3.util.PriorityMux(xBarSelect, io.xBarW.flatMap(_.en).zip(io.xBarW.flatMap(_.shiftEn)).map{case(e,se) => e | se})
        val liveXBarWireData = chisel3.util.PriorityMux(xBarSelect, io.xBarW.flatMap(_.data))
        val enable = (liveXBarWireEn) & wMask
        val data = liveXBarWireData
        (data, enable)
      }
      else if (directSelectEns.nonEmpty) {                                            // Has direct only
        val liveDirectWireSelects = directSelectEns.zip(directSelectShiftEns).map{case (e,se) => e | se}
        val liveDirectWireData = chisel3.util.PriorityMux(liveDirectWireSelects, directSelectDatas)
        val liveDirectWireEn = chisel3.util.PriorityMux(liveDirectWireSelects, liveDirectWireSelects)
        val enable = (liveDirectWireEn) & wMask
        val data = liveDirectWireData
        (data, enable)
      } else (0.U, false.B)
    if (p.isBuf) mem := Mux(io.asInstanceOf[ShiftRegFileInterface].dump_en, io.asInstanceOf[ShiftRegFileInterface].dump_in(flatCoord), Mux(shiftEnable, shiftSource, Mux(enable, data, mem)))
    else mem := Mux(io.reset, initval, Mux(shiftEnable, shiftSource, Mux(enable, data, mem)))
  }

  // Connect read data to output
  io.output.data.zipWithIndex.foreach { case (wire,i) =>
    if (p.xBarRMux.nonEmpty && i < p.xBarOutputs) {
      // Figure out which read port was active in xBar
      val xBarIds = p.xBarRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }
      val xBarCandidatesEns = xBarIds.map(io.xBarR.flatMap(_.en).toList(_))
      val xBarCandidatesBanks = xBarIds.map(io.xBarR.flatMap(_.banks).toList.grouped(p.banks.length).toList(_))
      val xBarCandidatesOffsets = xBarIds.map(io.xBarR.flatMap(_.ofs).toList(_))
      val sel = m.map{ mem =>
        if (xBarCandidatesEns.nonEmpty) (xBarCandidatesEns, xBarCandidatesBanks, xBarCandidatesOffsets).zipped.map {case(en,banks,ofs) =>
          banks.zip(mem._2).map{case (b, coord) => b === coord.U}.reduce{_&&_} && en
        }.reduce{_||_} else false.B
      }
      val datas = m.map{ _._1 }
      val d = chisel3.util.PriorityMux(sel, datas)
      wire := d
    } else {
      // Figure out which read port was active in direct
      val directIds = p.directRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i - p.xBarOutputs < entry._1.length) => p.directRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i - p.xBarOutputs}
      val directCandidatesEns = directIds.map(io.directR.flatMap(_.en).toList(_))
      val directCandidatesBanks = directIds.map(io.directR.flatMap(_.banks).toList(_))
      val directCandidatesOffsets = directIds.map(io.directR.flatMap(_.ofs).toList(_))
      // Create bit vector to select which bank was activated by this io
      val sel = m.map{ mem =>
        if (directCandidatesEns.nonEmpty) (directCandidatesEns, directCandidatesBanks, directCandidatesOffsets).zipped.map {case(en,banks,ofs) =>
          banks.zip(mem._2).map{case (b, coord) => b == coord}.reduce{_&&_}.B && en
        }.reduce{_||_} else false.B
      }
      val datas = m.map{ _._1 }
      val d = chisel3.util.PriorityMux(sel, datas)
      wire := d
    }
  }

}

class LUT(p: MemParams) extends MemPrimitive(p) {
  def this(logicalDims: List[Int], bitWidth: Int,
            xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
            directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
            inits: Option[List[Double]], syncMem: Boolean, fracBits: Int, isBuf: Boolean, myName: String) = this(MemParams(StandardInterface,logicalDims, bitWidth, logicalDims, List(1), xBarWMux, xBarRMux, directWMux, directRMux, BankedMemory, inits, syncMem, fracBits, isBuf, myName = myName))

  def this(tuple: (List[Int], Int, XMap, XMap, DMap, DMap, Option[List[Double]], Boolean, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9, false, "LUT")
  def this(tuple: (List[Int], Int, XMap, XMap, DMap, DMap)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, None, false, 0, false, "LUT")
  def this(logicalDims: List[Int], bitWidth: Int,
           banks: List[Int], strides: List[Int],
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int, myName: String = "LUT") = this(logicalDims, bitWidth, xBarWMux, xBarRMux, directWMux, directRMux, init, syncMem, fracBits, false, myName)


  // Create list of (mem: Mem1D, coords: List[Int] <coordinates of bank>)
  val m = (0 until p.depth).map{ i =>
    val coords = p.logicalDims.zipWithIndex.map{ case (b,j) =>
      i % (p.logicalDims.drop(j).product) / p.logicalDims.drop(j+1).product
    }
    val initval = if (p.inits.isDefined) (p.inits.get.apply(i)*scala.math.pow(2,p.fracBits)).toLong.S((p.bitWidth+1).W).asUInt.apply(p.bitWidth,0) else 0.U(p.bitWidth.W)
    val mem = RegInit(initval)
    (mem,coords,i)
  }

  // Connect read data to output
  io.output.data.zipWithIndex.foreach { case (wire,i) =>
    if (p.xBarRMux.nonEmpty && i < p.xBarOutputs) {
      // Figure out which read port was active in xBar
      val xBarIds = p.xBarRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }
      val xBarCandidatesEns = xBarIds.map(io.xBarR.flatMap(_.en).toList(_))
      val xBarCandidatesBanks = xBarIds.map(io.xBarR.flatMap(_.banks).toList.grouped(p.banks.length).toList(_))
      val xBarCandidatesOffsets = xBarIds.map(io.xBarR.flatMap(_.ofs).toList(_))
      val sel = m.map{ mem =>
        if (xBarCandidatesEns.nonEmpty) (xBarCandidatesEns, xBarCandidatesBanks, xBarCandidatesOffsets).zipped.map {case(en,banks,ofs) =>
          banks.zip(mem._2).map{case (b, coord) => b === coord.U}.reduce{_&&_} && en
        }.reduce{_||_} else false.B
      }
      val datas = m.map{ _._1 }
      val d = chisel3.util.PriorityMux(sel, datas)
      wire := d
    } else {
      // Figure out which read port was active in direct
      val directIds = p.directRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i - p.xBarOutputs < entry._1.length) => p.directRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i - p.xBarOutputs}
      val directCandidatesEns = directIds.map(io.directR.flatMap(_.en).toList(_))
      val directCandidatesBanks = directIds.map(io.directR.flatMap(_.banks).toList(_))
      val directCandidatesOffsets = directIds.map(io.directR.flatMap(_.ofs).toList(_))
      // Create bit vector to select which bank was activated by this io
      val sel = m.map{ mem =>
        if (directCandidatesEns.nonEmpty) (directCandidatesEns, directCandidatesBanks, directCandidatesOffsets).zipped.map {case(en,banks,ofs) =>
          banks.zip(mem._2).map{case (b, coord) => b == coord}.reduce{_&&_}.B && en
        }.reduce{_||_} else false.B
      }
      val datas = m.map{ _._1 }
      val d = chisel3.util.PriorityMux(sel, datas)
      wire := d
    }
  }

}


// Backing memory for SRAM
class Mem1D(val size: Int, bitWidth: Int, syncMem: Boolean = false) extends Module { // Unbanked, inner 1D mem
  def this(size: Int) = this(size, 32)

  val addrWidth = log2Up(size)

  val io = IO( new Bundle {
    val r = Input(new R_XBar(1, addrWidth, List(1)))
    val rMask = Input(Bool())
    val w = Input(new W_XBar(1, addrWidth, List(1), bitWidth))
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
  val wInBound = io.w.ofs.head <= (size).U
  val rInBound = io.r.ofs.head <= (size).U

  if (syncMem) {
    if (size <= globals.target.SramThreshold) {
      val m = (0 until size).map{ i =>
        val reg = RegInit(0.U(bitWidth.W))
        reg := Mux(io.w.en.head & wInBound & (io.w.ofs.head === i.U(addrWidth.W)) & io.wMask, io.w.data.head, reg)
        (i.U(addrWidth.W) -> reg)
      }
      val radder = getRetimed(io.r.ofs.head,1,io.flow)
      io.output.data := getRetimed(MuxLookup(radder, 0.U(bitWidth.W), m), 1, io.flow)
    } else {
      val m = Module(new SRAM(UInt(bitWidth.W), size, "Generic")) // TODO: Change to BRAM or URAM once we get SRAMVerilogAWS_BRAM/URAM.v
      if (size >= 2) m.io.raddr     := getRetimed(io.r.ofs.head, 1, io.flow)
      else           m.io.raddr     := 0.U
      m.io.waddr     := io.w.ofs.head
      m.io.wen       := io.w.en.head & wInBound & io.wMask
      m.io.wdata     := io.w.data.head
      m.io.flow      := io.flow
      io.output.data := m.io.rdata
    }
  } else {
    if (size <= globals.target.SramThreshold) {
      val m = (0 until size).map{ i =>
        val reg = RegInit(0.U(bitWidth.W))
        reg := Mux(io.w.en.head & io.wMask & (io.w.ofs.head === i.U(addrWidth.W)), io.w.data.head, reg)
        (i.U(addrWidth.W) -> reg)
      }
      io.output.data := MuxLookup(io.r.ofs.head, 0.U(bitWidth.W), m)
    } else {
      val m = Mem(size, UInt(bitWidth.W) /*, seqRead = true deprecated? */)
      when (io.w.en.head & io.wMask & wInBound) {m(io.w.ofs.head) := io.w.data.head}
      io.output.data := m(io.r.ofs.head)
    }
  }

  // if (globals.regression_testing == "1") {
  //   io.debug.invalidRAddr := ~rInBound
  //   io.debug.invalidWAddr := ~wInBound
  //   io.debug.rwOn := io.w.en.head & io.r.en.head & io.wMask & io.rMask
  //   io.debug.error := !rInBound | !wInBound | (io.w.en.head & io.r.en.head & io.wMask & io.rMask)
  //   // io.debug.addrProbe := m(0.U)
  // }
}


class enqPort(val w: Int) extends Bundle {
  val data = UInt(w.W)
  val en = Bool()

  override def cloneType = (new enqPort(w)).asInstanceOf[this.type] // See chisel3 bug 358
}

class Compactor(val ports: List[Int], val banks: Int, val width: Int, val bitWidth: Int = 32) extends Module {
  val num_compactors = ports.max
  val io = IO( new Bundle {
      val numEnabled =Input(UInt(width.W))
      val in = Vec(ports.sum, Input(new enqPort(bitWidth)))
      val out = Vec(num_compactors, Output(new enqPort(bitWidth)))
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
      io.out(i).data := chisel3.util.PriorityMux(mux_selects, mux_datas)
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
      val in = Vec(ports.sum, Input(new enqPort(bitWidth)))
      val out = Vec(banks, Output(new enqPort(bitWidth)))
      val debug1 = Output(Bool())
      val debug2 = Output(Bool())
    })

  val numEnabled = io.in.map{i => Mux(i.en, 1.U(width.W), 0.U(width.W))}.reduce{_+_}
  val num_compactors = ports.max

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
    io.out(i).data := outs(i)._1
    io.out(i).en := outs(i)._2
  }
}

class CompactingDeqNetwork(val ports: List[Int], val banks: Int, val width: Int, val bitWidth: Int = 32) extends Module {
  val io = IO( new Bundle {
      val tailCnt = Input(SInt(width.W))
      val input = new Bundle{
        val data = Vec(banks, Input(UInt(bitWidth.W)))
        val deq = Vec(ports.sum, Input(Bool()))
      }
      val output = new Bundle{
        val data = Vec(ports.max, Output(UInt(bitWidth.W)))
      }
    })

  // Compactor
  val num_compactors = ports.max
  // val numPort_width = 1 + Utils.log2Up(ports.max)
  val numEnabled = io.input.deq.map{i => Mux(i, 1.U(width.W), 0.U(width.W))}.reduce{_+_}

  // Router
  val current_base_bank = Math.singleCycleModulo(io.tailCnt, banks.S(width.W))
  val upper = current_base_bank + numEnabled.asSInt - banks.S(width.W)
  val num_straddling = Mux(upper < 0.S(width.W), 0.S(width.W), upper)
  val num_straight = (numEnabled.asSInt) - num_straddling
  // TODO: Probably has a bug if you have more than one dequeuer
  (0 until ports.max).foreach{ i =>
    val id_from_base = Mux(i.S(width.W) < num_straddling, i.S(width.W) + num_straight, Math.singleCycleModulo((i.S(width.W) + current_base_bank), banks.S(width.W)))
    val ens_below = if (i>0) (0 until i).map{j => Mux(io.input.deq(j), 1.U(width.W), 0.U(width.W)) }.reduce{_+_} else 0.U(width.W)
    val proper_bank = Math.singleCycleModulo((current_base_bank.asUInt + ens_below), banks.U(width.W))
    val port_vals = (0 until banks).map{ j => 
      (j.U(width.W) -> io.input.data(j)) 
    }
    io.output.data(i) := chisel3.util.MuxLookup(proper_bank.asUInt, 0.U(bitWidth.W), port_vals)
  }

}
