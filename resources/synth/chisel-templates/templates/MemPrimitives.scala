package templates

import util._
import chisel3._
import chisel3.util._
import ops._
import fringe._
import chisel3.util.MuxLookup
import Utils._
import scala.collection.immutable.HashMap

sealed trait BankingMode
object DiagonalMemory extends BankingMode
object BankedMemory extends BankingMode

sealed trait MemType
object SRAMType extends MemType
object FFType extends MemType
object FIFOType extends MemType
object LIFOType extends MemType
object ShiftRegFileType extends MemType
object LineBufferType extends MemType

sealed trait MemInterfaceType
object StandardInterface extends MemInterfaceType
object ShiftRegFileInterface extends MemInterfaceType
object FIFOInterface extends MemInterfaceType


case class MemParams(
  val iface: MemInterfaceType, // Required so the abstract MemPrimitive class can instantiate correct interface
  val logicalDims: List[Int], 
  val bitWidth: Int, 
  val banks: List[Int], 
  val strides: List[Int], 
  val xBarWMux: XMap, 
  val xBarRMux: XMap, // muxPort -> accessPar
  val directWMux: DMap, 
  val directRMux: DMap,  // muxPort -> List(banks, banks, ...)
  val bankingMode: BankingMode, 
  val inits: Option[List[Double]] = None, 
  val syncMem: Boolean = false, 
  val fracBits: Int = 0,
  val isBuf: Boolean = false
) {
  def depth: Int = logicalDims.product
  def hasXBarW: Boolean = xBarWMux.accessPars.sum > 0
  def hasXBarR: Boolean = xBarRMux.accessPars.sum > 0
  def numXBarW: Int = xBarWMux.accessPars.sum
  def numXBarR: Int = xBarRMux.accessPars.sum
  def numXBarWPorts: Int = xBarWMux.accessPars.size
  def numXBarRPorts: Int = xBarRMux.accessPars.size
  def hasDirectW: Boolean = directWMux.accessPars.sum > 0
  def hasDirectR: Boolean = directRMux.accessPars.sum > 0
  def numDirectW: Int = directWMux.accessPars.sum
  def numDirectR: Int = directRMux.accessPars.sum
  def numDirectWPorts: Int = directWMux.accessPars.size
  def numDirectRPorts: Int = directRMux.accessPars.size
  def xBarOutputs: Int = if (xBarRMux.toList.length > 0) xBarRMux.sortByMuxPortAndCombine.accessPars.max else 0
  def directOutputs: Int = if (directRMux.toList.length > 0) directRMux.sortByMuxPortAndCombine.accessPars.max else 0
  def totalOutputs: Int = xBarOutputs + directOutputs
  def numBanks: Int = banks.product
  def defaultDirect: List[List[Int]] = List(List.fill(banks.length)(99)) // Dummy bank address when ~hasDirectR
  def ofsWidth: Int = Utils.log2Up(depth/banks.product)
  def banksWidths: List[Int] = banks.map(Utils.log2Up(_))
  def elsWidth: Int = Utils.log2Up(depth) + 2
  def axes = xBarWMux.values.map(_._2).filter(_.isDefined)
  def axis: Int = if (axes.toList.length > 0) axes.toList.head.get else -1 // Assume all shifters are in the same axis


}


class R_XBar(val port_width: Int, val ofs_width:Int, val bank_width:List[Int]) extends Bundle {
  val banks = HVec.tabulate(port_width*bank_width.length){i => UInt(bank_width(i%bank_width.length).W)}
  val ofs = Vec(port_width, UInt(ofs_width.W))
  val en = Vec(port_width, Bool())

  def connectLane(lhs_lane: Int, rhs_lane: Int, rhs_port: R_XBar): Unit = {
    bank_width.length.indices[Unit]{i => banks(i + lhs_lane*bank_width.length) := rhs_port.banks(i + rhs_lane*bank_width.length)}
    ofs(lhs_lane) := rhs_port.ofs(rhs_lane)
    en(lhs_lane) := rhs_port.en(rhs_lane)
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

  def connectLane(lhs_lane: Int, rhs_lane: Int, rhs_port: R_Direct): Unit = {
    ofs(lhs_lane) := rhs_port.ofs(rhs_lane)
    en(lhs_lane) := rhs_port.en(rhs_lane)
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

abstract class MemInterface(p: MemParams) extends Bundle {
  var xBarW = HVec(Array.tabulate(1 max p.numXBarWPorts){i => Input(new W_XBar(p.xBarWMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths, p.bitWidth))})
  var xBarR = HVec(Array.tabulate(1 max p.numXBarRPorts){i => Input(new R_XBar(p.xBarRMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths))}) 
  var directW = HVec(Array.tabulate(1 max p.numDirectWPorts){i => 
    Input(new W_Direct(p.directWMux.accessPars.getOr1(i), p.ofsWidth, if (p.hasDirectW) p.directWMux.sortByMuxPortAndOfs.values.map(_._1).flatten.flatten.toList.grouped(p.banks.length).toList else p.defaultDirect, p.bitWidth))
  })
  var directR = HVec(Array.tabulate(1 max p.numDirectRPorts){i => 
    Input(new R_Direct(p.directRMux.accessPars.getOr1(i), p.ofsWidth, if (p.hasDirectR) p.directRMux.sortByMuxPortAndOfs.values.map(_._1).flatten.flatten.toList.grouped(p.banks.length).toList else p.defaultDirect))
  })
  var flow = Vec(1 max p.totalOutputs, Input(Bool()))
  var output = new Bundle {
    var data  = Vec(1 max p.totalOutputs, Output(UInt(p.bitWidth.W)))
  }
}

class StandardInterface(p: MemParams) extends MemInterface(p) {}  
class ShiftRegFileInterface(p: MemParams) extends MemInterface(p) {
  var dump_out = Vec(p.depth, Output(UInt(p.bitWidth.W)))
  var dump_in = Vec(p.depth, Input(UInt(p.bitWidth.W)))
  var dump_en = Input(Bool())
}
class FIFOInterface(p: MemParams) extends MemInterface(p) {
  var full = Output(Bool())
  var almostFull = Output(Bool())
  var empty = Output(Bool())
  var almostEmpty = Output(Bool())
  var numel = Output(UInt(32.W))
}

abstract class MemPrimitive(val p: MemParams) extends Module {
  val io = p.iface match {
    case StandardInterface => IO(new StandardInterface(p))
    case ShiftRegFileInterface => IO(new ShiftRegFileInterface(p))
    case FIFOInterface => IO(new FIFOInterface(p))
  } 

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
        io.xBarR(base).connectLane(vecId, i, rBundle)
        io.flow(outBase + vecId) := flow
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
        io.directR(base).connectLane(vecId, i, rBundle)
        io.flow(p.xBarOutputs + outBase + vecId) := flow
      }
      // Temp fix for merged readers not recomputing port info
      io.output.data(p.xBarOutputs + outBase + vecId)
    }
  }
}


class SRAM(p: MemParams) extends MemPrimitive(p) { 
  def this(logicalDims: List[Int], bitWidth: Int, banks: List[Int], strides: List[Int], 
           xBarWMux: XMap, xBarRMux: XMap, directWMux: DMap, directRMux: DMap,
           bankingMode: BankingMode, inits: Option[List[Double]], syncMem: Boolean, fracBits: Int) = this(MemParams(StandardInterface, logicalDims,bitWidth,banks,strides,xBarWMux,xBarRMux,directWMux,directRMux,bankingMode,inits,syncMem,fracBits))
  def this(tuple: (List[Int], Int, List[Int], List[Int], XMap, XMap, 
    DMap, DMap, BankingMode)) = this(MemParams(StandardInterface,tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7, tuple._8, tuple._9))

  // Get info on physical dims
  // TODO: Upcast dims to evenly bank
  val bankDim = p.bankingMode match {
    case DiagonalMemory => math.ceil(p.depth.toDouble / p.banks.product.toDouble).toInt //logicalDims.zipWithIndex.map { case (dim, i) => if (i == N - 1) math.ceil(dim.toDouble/banks.head).toInt else dim}
    case BankedMemory => math.ceil(p.depth.toDouble / p.banks.product.toDouble).toInt
  }
  val numMems = p.bankingMode match {
    case DiagonalMemory => p.banks.head
    case BankedMemory => p.banks.product
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
    val xBarSelect = io.xBarW.map(_.banks).flatten.grouped(p.banks.length).toList.zip(io.xBarW.map(_.en).flatten).map{ case(bids, en) => 
      bids.zip(mem._2).map{case (b,coord) => b === coord.U}.reduce{_&&_} & {if (p.hasXBarW) en else false.B}
    }
    // Check all direct W ports against this bank's coords
    val directSelectEns = io.directW.map(_.en).flatten.zip(io.directW.map(_.banks.flatten).flatten.grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectDatas = io.directW.map(_.data).flatten.zip(io.directW.map(_.banks.flatten).flatten.grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectOffsets = io.directW.map(_.ofs).flatten.zip(io.directW.map(_.banks.flatten).flatten.grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}

    // Unmask write port if any of the above match
    mem._1.io.wMask := xBarSelect.reduce{_|_} | {directSelectEns.or}

    // Connect matching W port to memory
    if (directSelectEns.length > 0 & p.hasXBarW) {           // Has direct and x
      val directChoiceEn = chisel3.util.PriorityMux(directSelectEns, directSelectEns)
      val directChoiceData = chisel3.util.PriorityMux(directSelectEns, directSelectDatas)
      val directChoiceOffset = chisel3.util.PriorityMux(directSelectEns, directSelectOffsets)
      val xBarChoiceEn = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.en).flatten.toList)
      val xBarChoiceData = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.data).flatten.toList)
      val xBarChoiceOffset = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.ofs).flatten.toList)
      mem._1.io.w.ofs.head  := Mux(directSelectEns.or, directChoiceOffset, xBarChoiceOffset)
      mem._1.io.w.data.head := Mux(directSelectEns.or, directChoiceData, xBarChoiceData)
      mem._1.io.w.en.head   := Mux(directSelectEns.or, directChoiceEn, xBarChoiceEn)
    } else if (p.hasXBarW && directSelectEns.length == 0) {  // Has x only
      val xBarChoiceEn = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.en).flatten.toList)
      val xBarChoiceData = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.data).flatten.toList)
      val xBarChoiceOffset = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.ofs).flatten.toList)
      mem._1.io.w.ofs.head  := xBarChoiceOffset
      mem._1.io.w.data.head := xBarChoiceData
      mem._1.io.w.en.head   := xBarChoiceEn 
    } else if (directSelectEns.length > 0) {               // Has direct only
      val directChoiceEn = chisel3.util.PriorityMux(directSelectEns, directSelectEns)
      val directChoiceData = chisel3.util.PriorityMux(directSelectEns, directSelectDatas)
      val directChoiceOffset = chisel3.util.PriorityMux(directSelectEns, directSelectOffsets)
      mem._1.io.w.ofs.head  := directChoiceOffset
      mem._1.io.w.data.head := directChoiceData
      mem._1.io.w.en.head   := directChoiceEn
    }
  }

  // Handle Reads
  m.foreach{ mem => 
    // Check all xBar r ports against this bank's coords
    val xBarSelect = io.xBarR.map(_.banks).flatten.grouped(p.banks.length).toList.zip(io.xBarR.map(_.en).flatten).map{ case(bids, en) => 
      bids.zip(mem._2).map{case (b,coord) => b === coord.U}.reduce{_&&_} & {if (p.hasXBarR) en else false.B}
    }
    // Check all direct r ports against this bank's coords
    val directSelectEns = io.directR.map(_.en).flatten.zip(io.directR.map(_.banks.flatten).flatten.grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectOffsets = io.directR.map(_.ofs).flatten.zip(io.directR.map(_.banks.flatten).flatten.grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(mem._2).map{case (b,coord) => b == coord}.reduce(_&_)) => en}

    // Unmask write port if any of the above match
    mem._1.io.rMask := {if (p.hasXBarR) xBarSelect.reduce{_|_} else true.B} & directSelectEns.or
    // Connect matching R port to memory
    if (directSelectEns.length > 0 & p.hasXBarR) {          // Has direct and x
      mem._1.io.r.ofs.head  := Mux(directSelectEns.or, chisel3.util.PriorityMux(directSelectEns, directSelectOffsets), chisel3.util.PriorityMux(xBarSelect, io.xBarR.map(_.ofs).flatten))
      mem._1.io.r.en.head   := Mux(directSelectEns.or, chisel3.util.PriorityMux(directSelectEns, directSelectEns), chisel3.util.PriorityMux(xBarSelect, io.xBarR.map(_.en).flatten))
    } else if (p.hasXBarR && directSelectEns.length == 0) { // Has x only
      mem._1.io.r.ofs.head  := chisel3.util.PriorityMux(xBarSelect, io.xBarR.map(_.ofs).flatten)
      mem._1.io.r.en.head   := chisel3.util.PriorityMux(xBarSelect, io.xBarR.map(_.ofs).flatten)
    } else if (directSelectEns.length > 0) {                                           // Has direct only
      mem._1.io.r.ofs.head  := chisel3.util.PriorityMux(directSelectEns, directSelectOffsets)
      mem._1.io.r.en.head   := chisel3.util.PriorityMux(directSelectEns, directSelectEns) 
    }
    // Use flow for last active r port
    val sels = Array.tabulate(directSelectEns.size + xBarSelect.size){i => 
      val r = Module(new SRFF())
      if (i < xBarSelect.size) r.io.input.set := xBarSelect(i)
      else r.io.input.set := directSelectEns(i - xBarSelect.size)
      r.io.input.reset := false.B
      if (i < xBarSelect.size) r.io.input.asyn_reset :=  (xBarSelect.patch(i,Nil,1) ++ directSelectEns).foldLeft(false.B)(_|_) 
      else r.io.input.asyn_reset := (xBarSelect ++ directSelectEns.patch(i-xBarSelect.size, Nil, 1)).foldLeft(false.B)(_|_)
      r.io.output.data
    }
    mem._1.io.flow   := chisel3.util.PriorityMux(sels, io.flow)
  }

  // Connect read data to output
  io.output.data.zipWithIndex.foreach { case (wire,i) => 
    if (p.xBarRMux.toList.length > 0 && i < p.xBarOutputs) {
      // Figure out which read port was active in xBar
      val xBarIds = p.xBarRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }
      val xBarCandidatesEns = xBarIds.map(io.xBarR.map(_.en).flatten.toList(_))
      val xBarCandidatesBanks = xBarIds.map(io.xBarR.map(_.banks).flatten.toList.grouped(p.banks.length).toList(_))
      val xBarCandidatesOffsets = xBarIds.map(io.xBarR.map(_.ofs).flatten.toList(_))
      val sel = m.map{ mem => 
        if (xBarCandidatesEns.toList.length > 0) (xBarCandidatesEns, xBarCandidatesBanks, xBarCandidatesOffsets).zipped.map {case(en,banks,ofs) => 
          banks.zip(mem._2).map{case (b, coord) => Utils.getRetimed(b, Utils.sramload_latency) === coord.U}.reduce{_&&_} && Utils.getRetimed(en, Utils.sramload_latency)
        }.reduce{_||_} else false.B
      }
      val datas = m.map{ _._1.io.output.data }
      val d = chisel3.util.PriorityMux(sel, datas)
      wire := d
    } else {
      // Figure out which read port was active in direct
      val directIds = p.directRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i - p.xBarOutputs < entry._1.length) => p.directRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i - p.xBarOutputs }
      val directCandidatesEns = directIds.map(io.directR.map(_.en).flatten.toList(_))
      val directCandidatesBanks = directIds.map(io.directR.map(_.banks).flatten.toList(_))
      val directCandidatesOffsets = directIds.map(io.directR.map(_.ofs).flatten.toList(_))
      // Create bit vector to select which bank was activated by this io
      val sel = m.map{ mem => 
        if (directCandidatesEns.toList.length > 0) (directCandidatesEns, directCandidatesBanks, directCandidatesOffsets).zipped.map {case(en,banks,ofs) => 
          banks.zip(mem._2).map{case (b, coord) => b == coord}.reduce{_&&_}.B && Utils.getRetimed(en, Utils.sramload_latency)
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
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int) = this(MemParams(StandardInterface, logicalDims, bitWidth, banks, strides, xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, init, syncMem, fracBits))
  // def this(logicalDims: List[Int], bitWidth: Int, 
  //          banks: List[Int], strides: List[Int], 
  //          xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
  //          directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
  //          bankingMode: BankingMode, init: => Option[List[Int]], syncMem: Boolean, fracBits: Int) = this(MemParams(logicalDims, bitWidth, banks, strides, xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, {if (init.isDefined) Some(init.get.map(_.toDouble)) else None}, syncMem, fracBits))
  def this(tuple: (Int, XMap)) = this(List(1), tuple._1,List(1), List(1), tuple._2, XMap((0,0,0) -> (1, None)), DMap(), DMap(), BankedMemory, None, false, 0)
  def this(bitWidth: Int) = this(List(1), bitWidth,List(1), List(1), XMap((0,0,0) -> (1, None)), XMap((0,0,0) -> (1, None)), DMap(), DMap(), BankedMemory, None, false, 0)
  def this(bitWidth: Int, xBarWMux: XMap, xBarRMux: XMap, inits: Option[List[Double]], fracBits: Int) = this(List(1), bitWidth,List(1), List(1), xBarWMux, xBarRMux, DMap(), DMap(), BankedMemory, inits, false, fracBits)

  val ff = if (p.inits.isDefined) RegInit((p.inits.get.head*scala.math.pow(2,p.fracBits)).toLong.S(p.bitWidth.W).asUInt) else RegInit(io.xBarW(0).init.head)
  val anyReset: Bool = io.xBarW.map{_.reset}.flatten.toList.reduce{_|_}
  val anyEnable: Bool = io.xBarW.map{_.en}.flatten.toList.reduce{_|_}
  val wr_data: UInt = chisel3.util.Mux1H(io.xBarW.map{_.en}.flatten.toList, io.xBarW.map{_.data}.flatten.toList)
  ff := Mux(anyReset, io.xBarW(0).init.head, Mux(anyEnable, wr_data, ff))
  io.output.data.foreach(_ := ff)

}

class FIFO(p: MemParams) extends MemPrimitive(p) {
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: List[Int], xBarWMux: XMap, xBarRMux: XMap,
           inits: Option[List[Double]] = None, syncMem: Boolean = false, fracBits: Int = 0) = this(MemParams(FIFOInterface,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, DMap(), DMap(), BankedMemory, inits, syncMem, fracBits))

  def this(tuple: (List[Int], Int, List[Int], XMap, XMap)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: List[Int], strides: List[Int], 
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int) = this(MemParams(FIFOInterface,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, init, syncMem, fracBits))

  // Create bank counters
  val headCtr = Module(new CompactingCounter(p.numXBarW, p.depth, p.elsWidth))
  val tailCtr = Module(new CompactingCounter(p.numXBarR, p.depth, p.elsWidth))
  (0 until p.numXBarW).foreach{i => headCtr.io.input.enables.zip(io.xBarW.map(_.en).flatten).foreach{case (l,r) => l := r}}
  (0 until p.numXBarR).foreach{i => tailCtr.io.input.enables.zip(io.xBarR.map(_.en).flatten).foreach{case (l,r) => l := r}}
  headCtr.io.input.reset := reset
  tailCtr.io.input.reset := reset
  headCtr.io.input.dir := true.B
  tailCtr.io.input.dir := true.B

  // Create numel counter
  val elements = Module(new CompactingIncDincCtr(p.numXBarW, p.numXBarR, p.depth, p.elsWidth))
  elements.io.input.inc_en.zip(io.xBarW.map(_.en).flatten).foreach{case(l,r) => l := r}
  elements.io.input.dinc_en.zip(io.xBarR.map(_.en).flatten).foreach{case(l,r) => l := r}

  // Create physical mems
  val m = (0 until p.numBanks).map{ i => Module(new Mem1D(p.depth/p.numBanks, p.bitWidth))}

  // Create compacting network

  val enqCompactor = Module(new CompactingEnqNetwork(p.xBarWMux.sortByMuxPortAndCombine.values.map(_._1).toList, p.numBanks, p.elsWidth, p.bitWidth))
  enqCompactor.io.headCnt := headCtr.io.output.count
  (0 until p.numXBarW).foreach{i => 
    enqCompactor.io.in.map(_.data).zip(io.xBarW.map(_.data).flatten).foreach{case (l,r) => l := r}; 
    enqCompactor.io.in.map(_.en).zip(io.xBarW.map(_.en).flatten).foreach{case(l,r) => l := r}
  }

  // Connect compacting network to banks
  val active_w_bank = Utils.singleCycleModulo(headCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  val active_w_addr = Utils.singleCycleDivide(headCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
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
  val active_r_bank = Utils.singleCycleModulo(tailCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  val active_r_addr = Utils.singleCycleDivide(tailCtr.io.output.count, p.numBanks.S(p.elsWidth.W))
  (0 until p.numBanks).foreach{i => 
    val addr = Mux(i.S(p.elsWidth.W) < active_r_bank, active_r_addr + 1.S(p.elsWidth.W), active_r_addr)
    m(i).io.r.ofs.head := addr.asUInt
    deqCompactor.io.input.data(i) := m(i).io.output.data
  }
  (0 until p.numXBarR).foreach{i =>
    deqCompactor.io.input.deq.zip(io.xBarR.map(_.en).flatten).foreach{case (l,r) => l := r}
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
           inits: Option[List[Double]] = None, syncMem: Boolean = false, fracBits: Int = 0) = this(MemParams(FIFOInterface,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, DMap(), DMap(), BankedMemory, inits, syncMem, fracBits))
  def this(tuple: (List[Int], Int, List[Int], XMap, XMap)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: List[Int], strides: List[Int], 
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int) = this(MemParams(FIFOInterface,logicalDims, bitWidth, banks, List(1), xBarWMux, xBarRMux, directWMux, directRMux, bankingMode, init, syncMem, fracBits))

  val pW = p.xBarWMux.accessPars.max
  val pR = p.xBarRMux.accessPars.max
  val par = scala.math.max(pW, pR) // TODO: Update this template because this was from old style

  // Register for tracking number of elements in FILO
  val elements = Module(new IncDincCtr(pW,pR, p.depth))
  elements.io.input.inc_en := io.xBarW.map(_.en).flatten.toList.reduce{_|_}
  elements.io.input.dinc_en := io.xBarR.map(_.en).flatten.toList.reduce{_|_}

  // Create physical mems
  val m = (0 until par).map{ i => Module(new Mem1D(p.depth/par, p.bitWidth))}

  // Create head and reader sub counters
  val sa_width = 2 + Utils.log2Up(par)
  val subAccessor = Module(new SingleSCounterCheap(1,0,par,pW,-pR,0,sa_width))
  subAccessor.io.input.enable := io.xBarW.map(_.en).flatten.toList.reduce{_|_} | io.xBarR.map(_.en).flatten.toList.reduce{_|_}
  subAccessor.io.input.dir := io.xBarW.map(_.en).flatten.toList.reduce{_|_}
  subAccessor.io.input.reset := reset
  subAccessor.io.input.saturate := false.B
  val subAccessor_prev = Mux(subAccessor.io.output.count(0) - pR.S(sa_width.W) < 0.S(sa_width.W), (par-pR).S(sa_width.W), subAccessor.io.output.count(0) - pR.S(sa_width.W))

  // Create head and reader counters
  val a_width = 2 + Utils.log2Up(p.depth/par)
  val accessor = Module(new SingleSCounterCheap(1, 0, (p.depth/par), 1, -1, 0, a_width))
  accessor.io.input.enable := (io.xBarW.map(_.en).flatten.toList.reduce{_|_} & subAccessor.io.output.done) | (io.xBarR.map(_.en).flatten.toList.reduce{_|_} & subAccessor_prev === 0.S(sa_width.W))
  accessor.io.input.dir := io.xBarW.map(_.en).flatten.toList.reduce{_|_}
  accessor.io.input.reset := reset
  accessor.io.input.saturate := false.B


  // Connect pusher
  if (pW == pR) {
    m.zipWithIndex.foreach { case (mem, i) => 
      // Figure out which write port was active in xBar
      val xBarIds = p.xBarWMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarWMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }.toList
      val xBarCandidatesEns = xBarIds.map{case n => io.xBarW.map(_.en).flatten.toList(n+i)}
      val xBarCandidatesDatas = xBarIds.map{case n => io.xBarW.map(_.data).flatten.toList(n+i)}

      // Make connections to memory
      mem.io.w.ofs.head := accessor.io.output.count(0).asUInt
      mem.io.w.data.head := Mux1H(xBarCandidatesEns, xBarCandidatesDatas)
      mem.io.w.en.head := xBarCandidatesEns.or
      mem.io.wMask := xBarCandidatesEns.or
    }
  } else {
    (0 until pW).foreach { w_i => 
      (0 until (par /-/ pW)).foreach { i => 
        // Figure out which write port was active in xBar
        val xBarIds = p.xBarWMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarWMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }.toList
        val xBarCandidatesEns = xBarIds.map{case n => io.xBarW.map(_.en).flatten.toList(n+(i*pW+w_i))}
        val xBarCandidatesDatas = xBarIds.map{case n => io.xBarW.map(_.data).flatten.toList(n+(i*pW+w_i))}

        // Make connections to memory
        m(w_i + i*-*pW).io.w.ofs.head := accessor.io.output.count(0).asUInt
        m(w_i + i*-*pW).io.w.data.head := Mux1H(xBarCandidatesEns, xBarCandidatesDatas)
        m(w_i + i*-*pW).io.w.en.head := xBarCandidatesEns.or & (subAccessor.io.output.count(0) === (i*pW).S(sa_width.W))
        m(w_i + i*-*pW).io.wMask := xBarCandidatesEns.or & (subAccessor.io.output.count(0) === (i*pW).S(sa_width.W))
      }
    }
  }

  // Connect popper
  if (pW == pR) {
    m.zipWithIndex.foreach { case (mem, i) => 
      mem.io.r.ofs.head := (accessor.io.output.count(0) - 1.S(a_width.W)).asUInt
      mem.io.r.en.head := io.xBarR.map(_.en).flatten.toList.reduce{_|_}
      mem.io.rMask := io.xBarR.map(_.en).flatten.toList.reduce{_|_}
      io.output.data(i) := mem.io.output.data
    }
  } else {
    (0 until pR).foreach { r_i => 
      val rSel = Wire(Vec( (par/pR), Bool()))
      val rData = Wire(Vec( (par/pR), UInt(p.bitWidth.W)))
      (0 until (par /-/ pR)).foreach { i => 
        m(r_i + i*-*pR).io.r.ofs.head := (accessor.io.output.count(0) - 1.S(sa_width.W)).asUInt
        m(r_i + i*-*pR).io.r.en.head := io.xBarR.map(_.en).flatten.toList.reduce{_|_} & (subAccessor_prev === (i*-*pR).S(sa_width.W))
        m(r_i + i*-*pR).io.rMask := io.xBarR.map(_.en).flatten.toList.reduce{_|_} & (subAccessor_prev === (i*-*pR).S(sa_width.W))
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
            inits: Option[List[Double]] = None, syncMem: Boolean = false, fracBits: Int = 0, isBuf: Boolean = false) = this(MemParams(ShiftRegFileInterface,logicalDims, bitWidth, logicalDims, List(1), xBarWMux, xBarRMux, directWMux, directRMux, BankedMemory, inits, syncMem, fracBits, isBuf))

  def this(tuple: (List[Int], Int, XMap, XMap, DMap, DMap, Option[List[Double]], Boolean, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9)
  def this(tuple: (List[Int], Int, XMap, XMap, DMap, DMap)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: List[Int], strides: List[Int], 
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int) = this(logicalDims, bitWidth, xBarWMux, xBarRMux, directWMux, directRMux, init, syncMem, fracBits)


  // Create list of (mem: Mem1D, coords: List[Int] <coordinates of bank>)
  val m = (0 until p.depth).map{ i => 
    val coords = p.logicalDims.zipWithIndex.map{ case (b,j) => 
      i % (p.logicalDims.drop(j).product) / p.logicalDims.drop(j+1).product
    }
    val initval = if (p.inits.isDefined) (p.inits.get.apply(i)*scala.math.pow(2,p.fracBits)).toLong.U(p.bitWidth.W) else 0.U(p.bitWidth.W)
    val mem = RegInit(initval)
    io.asInstanceOf[ShiftRegFileInterface].dump_out(i) := mem
    (mem,coords,i)
  }

  def stripCoord(l: List[Int], x: Int): List[Int] = {l.take(x) ++ l.drop(x+1)}
  def stripCoord(l: HVec[UInt], x: Int): HVec[UInt] = {HVec(l.take(x) ++ l.drop(x+1))}
  def decrementAxisCoord(l: List[Int], x: Int): List[Int] = {l.take(x) ++ List(l(x) - 1) ++ l.drop(x+1)}
  // Handle Writes
  m.foreach{ case(mem, coords, flatCoord) => 
    // Check all xBar w ports against this bank's coords
    val xBarSelect = (io.xBarW.map(_.banks).flatten.toList.grouped(p.banks.length).toList, io.xBarW.map(_.en).flatten.toList, io.xBarW.map(_.shiftEn).flatten.toList).zipped.map{ case(bids, en, shiftEn) => 
      bids.zip(coords).map{case (b,coord) => b === coord.U}.reduce{_&&_} & {if (p.hasXBarW) en | shiftEn else false.B}
    }
    // Check all direct W ports against this bank's coords
    val directSelectEns = io.directW.map(_.en).flatten.zip(io.directW.map(_.banks.flatten).flatten.grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectDatas = io.directW.map(_.data).flatten.zip(io.directW.map(_.banks.flatten).flatten.grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectOffsets = io.directW.map(_.ofs).flatten.zip(io.directW.map(_.banks.flatten).flatten.grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}
    val directSelectShiftEns = io.directW.map(_.shiftEn).flatten.zip(io.directW.map(_.banks.flatten).flatten.grouped(p.banks.length).toList).collect{case (en, banks) if (banks.zip(coords).map{case (b,coord) => b == coord}.reduce(_&_)) => en}

    // Unmask write port if any of the above match
    val wMask = xBarSelect.reduce{_|_} | directSelectEns.or | directSelectShiftEns.or

    // Check if shiftEn is turned on for this line, guaranteed false on entry plane
    val shiftMask = if (p.axis >= 0 && coords(p.axis) != 0) {
      // XBarW requests shift
      val axisShiftXBar = io.xBarW.map(_.banks).flatten.toList.grouped(p.banks.length).toList.zip(io.xBarW.map(_.shiftEn).flatten.toList).map{ case(bids, en) => 
        bids.zip(coords).zipWithIndex.map{case ((b, coord),id) => if (id == p.axis) true.B else b === coord.U}.reduce{_&&_} & {if (p.hasXBarW) en else false.B}
      }
      // // DirectW requests shift
      // val axisShiftDirect = io.directW.map(_.banks).flatten.filter{case banks => stripCoord(banks, p.axis).zip(stripCoord(coords, p.axis)).map{case (b,coord) => b == coord}.reduce(_&_)}

      // Unmask shift if any of the above match
      axisShiftXBar.reduce{_|_} | directSelectShiftEns.or
    } else false.B

    // Connect matching W port to memory
    val shiftSource = if (p.axis >= 0 && coords(p.axis) != 0) m.filter{case (_,c,_) => decrementAxisCoord(coords,p.axis) == c}.head._1 else mem
    val shiftEnable = if (p.axis >= 0 && coords(p.axis) != 0) shiftMask else false.B
    val (data, enable) = 
      if (directSelectEns.length > 0 & p.hasXBarW) {           // Has direct and x
        val liveDirectWireSelects = directSelectEns.zip(directSelectShiftEns).map{case (e,se) => e | se}
        val liveDirectWireData = chisel3.util.PriorityMux(liveDirectWireSelects, directSelectDatas)
        val liveDirectWireEn = chisel3.util.PriorityMux(liveDirectWireSelects, liveDirectWireSelects)
        val liveXBarWireEn = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.en).flatten.zip(io.xBarW.map(_.shiftEn).flatten).map{case(e,se) => e | se})
        val liveXBarWireData = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.data).flatten)
        val enable = Mux(liveDirectWireSelects.or, liveDirectWireEn, liveXBarWireEn) & wMask
        val data = Mux(liveDirectWireSelects.or, liveDirectWireData, liveXBarWireData)
        (data, enable)
      } else if (p.hasXBarW && directSelectEns.length == 0) {  // Has x only
        val liveXBarWireEn = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.en).flatten.zip(io.xBarW.map(_.shiftEn).flatten).map{case(e,se) => e | se})
        val liveXBarWireData = chisel3.util.PriorityMux(xBarSelect, io.xBarW.map(_.data).flatten)
        val enable = (liveXBarWireEn) & wMask
        val data = liveXBarWireData
        (data, enable)
      } else if (directSelectEns.length > 0) {                                            // Has direct only
        val liveDirectWireSelects = directSelectEns.zip(directSelectShiftEns).map{case (e,se) => e | se}
        val liveDirectWireData = chisel3.util.PriorityMux(liveDirectWireSelects, directSelectDatas)
        val liveDirectWireEn = chisel3.util.PriorityMux(liveDirectWireSelects, liveDirectWireSelects)
        val enable = (liveDirectWireEn) & wMask
        val data = liveDirectWireData
        (data, enable)
      } else (0.U, false.B)
    if (p.isBuf) mem := Mux(io.asInstanceOf[ShiftRegFileInterface].dump_en, io.asInstanceOf[ShiftRegFileInterface].dump_in(flatCoord), Mux(shiftEnable, shiftSource, Mux(enable, data, mem)))
    else mem := Mux(shiftEnable, shiftSource, Mux(enable, data, mem))
  }

  // Connect read data to output
  io.output.data.zipWithIndex.foreach { case (wire,i) => 
    if (p.xBarRMux.toList.length > 0 && i < p.xBarOutputs) {
      // Figure out which read port was active in xBar
      val xBarIds = p.xBarRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }
      val xBarCandidatesEns = xBarIds.map(io.xBarR.map(_.en).flatten.toList(_))
      val xBarCandidatesBanks = xBarIds.map(io.xBarR.map(_.banks).flatten.toList.grouped(p.banks.length).toList(_))
      val xBarCandidatesOffsets = xBarIds.map(io.xBarR.map(_.ofs).flatten.toList(_))
      val sel = m.map{ mem => 
        if (xBarCandidatesEns.toList.length > 0) (xBarCandidatesEns, xBarCandidatesBanks, xBarCandidatesOffsets).zipped.map {case(en,banks,ofs) => 
          banks.zip(mem._2).map{case (b, coord) => b === coord.U}.reduce{_&&_} && en
        }.reduce{_||_} else false.B
      }
      val datas = m.map{ _._1 }
      val d = chisel3.util.PriorityMux(sel, datas)
      wire := d
    } else {
      // Figure out which read port was active in direct
      val directIds = p.directRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i - p.xBarOutputs < entry._1.length) => p.directRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i - p.xBarOutputs}
      val directCandidatesEns = directIds.map(io.directR.map(_.en).flatten.toList(_))
      val directCandidatesBanks = directIds.map(io.directR.map(_.banks).flatten.toList(_))
      val directCandidatesOffsets = directIds.map(io.directR.map(_.ofs).flatten.toList(_))
      // Create bit vector to select which bank was activated by this io
      val sel = m.map{ mem => 
        if (directCandidatesEns.toList.length > 0) (directCandidatesEns, directCandidatesBanks, directCandidatesOffsets).zipped.map {case(en,banks,ofs) => 
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
            inits: Option[List[Double]] = None, syncMem: Boolean = false, fracBits: Int = 0, isBuf: Boolean = false) = this(MemParams(StandardInterface,logicalDims, bitWidth, logicalDims, List(1), xBarWMux, xBarRMux, directWMux, directRMux, BankedMemory, inits, syncMem, fracBits, isBuf))

  def this(tuple: (List[Int], Int, XMap, XMap, DMap, DMap, Option[List[Double]], Boolean, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9)
  def this(tuple: (List[Int], Int, XMap, XMap, DMap, DMap)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)
  def this(logicalDims: List[Int], bitWidth: Int, 
           banks: List[Int], strides: List[Int], 
           xBarWMux: XMap, xBarRMux: XMap, // muxPort -> accessPar
           directWMux: DMap, directRMux: DMap,  // muxPort -> List(banks, banks, ...)
           bankingMode: BankingMode, init: Option[List[Double]], syncMem: Boolean, fracBits: Int) = this(logicalDims, bitWidth, xBarWMux, xBarRMux, directWMux, directRMux, init, syncMem, fracBits)


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
    if (p.xBarRMux.toList.length > 0 && i < p.xBarOutputs) {
      // Figure out which read port was active in xBar
      val xBarIds = p.xBarRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i < entry._1) => p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i }
      val xBarCandidatesEns = xBarIds.map(io.xBarR.map(_.en).flatten.toList(_))
      val xBarCandidatesBanks = xBarIds.map(io.xBarR.map(_.banks).flatten.toList.grouped(p.banks.length).toList(_))
      val xBarCandidatesOffsets = xBarIds.map(io.xBarR.map(_.ofs).flatten.toList(_))
      val sel = m.map{ mem => 
        if (xBarCandidatesEns.toList.length > 0) (xBarCandidatesEns, xBarCandidatesBanks, xBarCandidatesOffsets).zipped.map {case(en,banks,ofs) => 
          banks.zip(mem._2).map{case (b, coord) => b === coord.U}.reduce{_&&_} && en
        }.reduce{_||_} else false.B
      }
      val datas = m.map{ _._1 }
      val d = chisel3.util.PriorityMux(sel, datas)
      wire := d
    } else {
      // Figure out which read port was active in direct
      val directIds = p.directRMux.sortByMuxPortAndCombine.collect{case(muxAddr,entry) if (i - p.xBarOutputs < entry._1.length) => p.directRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum + i - p.xBarOutputs}
      val directCandidatesEns = directIds.map(io.directR.map(_.en).flatten.toList(_))
      val directCandidatesBanks = directIds.map(io.directR.map(_.banks).flatten.toList(_))
      val directCandidatesOffsets = directIds.map(io.directR.map(_.ofs).flatten.toList(_))
      // Create bit vector to select which bank was activated by this io
      val sel = m.map{ mem => 
        if (directCandidatesEns.toList.length > 0) (directCandidatesEns, directCandidatesBanks, directCandidatesOffsets).zipped.map {case(en,banks,ofs) => 
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

  val addrWidth = Utils.log2Up(size)

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
    if (size <= Utils.SramThreshold) {
      val m = (0 until size).map{ i =>
        val reg = RegInit(0.U(bitWidth.W))
        reg := Mux(io.w.en.head & (io.w.ofs.head === i.U(addrWidth.W)) & io.wMask, io.w.data.head, reg)
        (i.U(addrWidth.W) -> reg)
      }
      val radder = Utils.getRetimed(io.r.ofs.head,1,io.flow)
      io.output.data := Utils.getRetimed(MuxLookup(radder, 0.U(bitWidth.W), m), 1, io.flow)
    } else {
      val m = Module(new fringe.SRAM(UInt(bitWidth.W), size, "Generic")) // TODO: Change to BRAM or URAM once we get SRAMVerilogAWS_BRAM/URAM.v
      m.io.raddr     := Utils.getRetimed(io.r.ofs.head, 1, io.flow)
      m.io.waddr     := io.w.ofs.head
      m.io.wen       := io.w.en.head & wInBound & io.wMask
      m.io.wdata     := io.w.data.head
      m.io.flow      := io.flow
      io.output.data := m.io.rdata
    }
  } else {
    if (size <= Utils.SramThreshold) {
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

  if (scala.util.Properties.envOrElse("RUNNING_REGRESSION", "0") == "1") {
    io.debug.invalidRAddr := ~rInBound
    io.debug.invalidWAddr := ~wInBound
    io.debug.rwOn := io.w.en.head & io.r.en.head & io.wMask & io.rMask
    io.debug.error := ~rInBound | ~wInBound | (io.w.en.head & io.r.en.head & io.wMask & io.rMask)
    // io.debug.addrProbe := m(0.U)
  }

}



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
      case DiagonalMemory => wbundle.addr.zipWithIndex.map {case (logical, i) => if (i == N - 1) logical./-/(banks.head.U,None,true.B) else logical}
      case BankedMemory => wbundle.addr.zip(banks).map{ case (logical, b) => logical./-/(b.U,None,true.B) }
    }
    physicalAddrs.zipWithIndex.foreach { case (calculatedAddr, i) => convertedW.addr(i) := calculatedAddr}
    convertedW.data := wbundle.data
    convertedW.en := wbundle.en
    val flatBankId = bankingMode match {
      case DiagonalMemory => wbundle.addr.reduce{_+_}.%-%(banks.head.U, None, true.B)
      case BankedMemory => 
        val bankCoords = wbundle.addr.zip(banks).map{ case (logical, b) => logical.%-%(b.U,None, true.B) }
       bankCoords.zipWithIndex.map{ case (c, i) => c.*-*( (banks.drop(i).reduce{_*_}/banks(i)).U,None, true.B) }.reduce{_+_}
        // bankCoords.zipWithIndex.map{ case (c, i) => FringeGlobals.bigIP.multiply(c, (banks.drop(i).reduce{_.*-*( _,None)}/-/banks(i)).U, 0) }.reduce{_+_}
    }

    (convertedW, flatBankId)
  }
  val convertedWVec = wConversions.map{_._1}
  val bankIdW = wConversions.map{_._2}

  val rConversions = io.r.map{ rbundle => 
    // Reader conversion
    val convertedR = Wire(new multidimR(N,logicalDims,bitWidth))
    val physicalAddrs = bankingMode match {
      case DiagonalMemory => rbundle.addr.zipWithIndex.map {case (logical, i) => if (i == N - 1) logical./-/(banks.head.U,None,true.B) else logical}
      case BankedMemory => rbundle.addr.zip(banks).map{ case (logical, b) => logical./-/(b.U,None,true.B) }
    }
    physicalAddrs.zipWithIndex.foreach { case (calculatedAddr, i) => convertedR.addr(i) := calculatedAddr}
    convertedR.en := rbundle.en
    val syncDelay = 0//if (syncMem) 1 else 0
    val flatBankId = bankingMode match {
      case DiagonalMemory => Utils.getRetimed(rbundle.addr.reduce{_+_}, syncDelay).%-%(banks.head.U, None, true.B)
      case BankedMemory => 
        val bankCoords = rbundle.addr.zip(banks).map{ case (logical, b) => Utils.getRetimed(logical, syncDelay).%-%(b.U,None, true.B) }
       bankCoords.zipWithIndex.map{ case (c, i) => c.*-*( (banks.drop(i).reduce{_*_}/banks(i)).U,None, true.B) }.reduce{_+_}
        // bankCoords.zipWithIndex.map{ case (c, i) => FringeGlobals.bigIP.multiply(c, (banks.drop(i).reduce{_.*-*( _,None)}/-/banks(i)).U, 0) }.reduce{_+_}
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

  def connectWPort(wBundle: Vec[multidimW], ports: List[Int]): Unit = {
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
    // FringeGlobals.bigIP.multiply(addr, (banks.drop(i).reduce{_.*-*( _,None)}/-/banks(i)).U, 0)
   addr.*-*( (dims.drop(i).reduce{_*_}/dims(i)).U, None, true.B)
  }.reduce{_+_}, 0 max Utils.sramstore_latency - 1)
  m.io.r.addr := Utils.getRetimed(io.r.addr.zipWithIndex.map{ case (addr, i) =>
    // FringeGlobals.bigIP.multiply(addr, (dims.drop(i).reduce{_.*-*( _,None)}/dims(i)).U, 0)
   addr.*-*( (dims.drop(i).reduce{_*_}/dims(i)).U, None, true.B)
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
      val mux_selects = num_inputs_per_bundle.zipWithIndex.map{case(j, id) => 
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
      }.flatten
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
  val current_base_bank = Utils.singleCycleModulo(io.headCnt, banks.S(width.W))
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
  val current_base_bank = Utils.singleCycleModulo(io.tailCnt, banks.S(width.W))
  val upper = current_base_bank + numEnabled.asSInt - banks.S(width.W)
  val num_straddling = Mux(upper < 0.S(width.W), 0.S(width.W), upper)
  val num_straight = (numEnabled.asSInt) - num_straddling
  // TODO: Probably has a bug if you have more than one dequeuer
  (0 until ports.max).foreach{ i =>
    val id_from_base = Mux(i.S(width.W) < num_straddling, i.S(width.W) + num_straight, Utils.singleCycleModulo((i.S(width.W) + current_base_bank), banks.S(width.W)))
    val ens_below = if (i>0) (0 until i).map{j => Mux(io.input.deq(j), 1.U(width.W), 0.U(width.W)) }.reduce{_+_} else 0.U(width.W)
    val proper_bank = Utils.singleCycleModulo((current_base_bank.asUInt + ens_below), banks.U(width.W))
    val port_vals = (0 until banks).map{ j => 
      (j.U(width.W) -> io.input.data(j)) 
    }
    io.output.data(i) := chisel3.util.MuxLookup(proper_bank.asUInt, 0.U(bitWidth.W), port_vals)
  }

}