package fringe.templates.memory

import chisel3._
import fringe.utils.HVec
import fringe.Ledger._
import fringe.utils.DMap._
import fringe.utils.XMap._
import fringe.utils.implicits._
import fringe._


sealed trait MemInterfaceType

sealed abstract class MemInterface(val p: MemParams) extends Bundle {
  val xBarW = HVec(Array.tabulate(1 max p.numXBarWPorts){i => Input(new W_XBar(p.xBarWMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths, p.bitWidth))})
  val xBarR = HVec(Array.tabulate(1 max p.numXBarRPorts){i => Input(new R_XBar(p.xBarRMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths))})
  val directW = HVec(Array.tabulate(1 max p.numDirectWPorts){i =>
    Input(new W_Direct(p.directWMux.accessPars.getOr1(i), p.ofsWidth, if (p.hasDirectW) p.directWMux.sortByMuxPortAndOfs.values.flatMap(_._1).flatten.toList.grouped(p.banks.length).toList else p.defaultDirect, p.bitWidth))
  })
  val directR = HVec(Array.tabulate(1 max p.numDirectRPorts){i =>
    Input(new R_Direct(p.directRMux.accessPars.getOr1(i), p.ofsWidth, if (p.hasDirectR) p.directRMux.sortByMuxPortAndOfs.values.flatMap(_._1).flatten.toList.grouped(p.banks.length).toList else p.defaultDirect))
  })
  val output = Vec(1 max p.totalOutputs, Output(UInt(p.bitWidth.W)))
  val reset = Input(Bool())

  def connectLedger(op: MemInterface)(implicit stack: List[KernelHash]): Unit = {
    if (Ledger.connections.contains(op.hashCode) && Ledger.connections(op.hashCode).contains(stack.head.hashCode)) {
      val cxn = Ledger.connections(op.hashCode)(stack.head.hashCode)
      cxn.xBarR.foreach{case RAddr(p,lane) => xBarR(p).forwardLane(lane, op.xBarR(p))}
      cxn.xBarW.foreach{p => xBarW(p) <> op.xBarW(p)}
      cxn.directR.foreach{case RAddr(p,lane) => directR(p).forwardLane(lane, op.directR(p))}
      cxn.directW.foreach{p => directW(p) <> op.directW(p)}
      cxn.reset.foreach{p => reset <> op.reset}
      cxn.output.foreach{p => output(p) <> op.output(p)}
    }
    else this <> op
  }

  def connectReset(r: Bool)(implicit stack: List[KernelHash]): Unit = {
    reset := r
    Ledger.connectReset(this.hashCode, 0)
  }

  var usedMuxPorts = List[(String,(Int,Int,Int,Int))]() // Check if the muxPort, muxAddr, lane, castgrp is taken for this connection style (xBar or direct)
  def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxAddr: (Int, Int))(implicit stack: List[KernelHash]): Unit = {
    assert(p.hasXBarW)
    assert(p.xBarWMux.contains((muxAddr._1,muxAddr._2,0)))
    assert(!usedMuxPorts.contains(("XBarW", (muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to XBarW port $muxAddr twice!")
    usedMuxPorts ::= ("XBarW", (muxAddr._1,muxAddr._2,0,0))
    val base = p.xBarWMux.accessParsBelowMuxPort(muxAddr._1,muxAddr._2,0).size
    xBarW(base) := wBundle
    Ledger.connectXBarW(this.hashCode, base)
  }

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {
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
        Ledger.connectXBarR(this.hashCode, base, vecId)
        xBarR(base).connectLane(vecId, i, rBundle, backpressure)
      }
      // Temp fix for merged readers not recomputing port info
      Ledger.connectOutput(this.hashCode, outBase + vecId)
      output(outBase + vecId)
    }
    
  }

  def connectDirectWPort(wBundle: W_Direct, bufferPort: Int, muxAddr: (Int, Int))(implicit stack: List[KernelHash]): Unit = {
    assert(p.hasDirectW)
    assert(p.directWMux.contains((muxAddr._1,muxAddr._2,0)))
    assert(!usedMuxPorts.contains(("DirectW", (muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to DirectW port $muxAddr twice!")
    usedMuxPorts ::= ("DirectW", (muxAddr._1,muxAddr._2,0,0))
    val base = p.directWMux.accessParsBelowMuxPort(muxAddr._1,muxAddr._2,0).size
    directW(base) := wBundle
    Ledger.connectDirectW(this.hashCode, base)
  }

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectDirectRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {
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
        Ledger.connectDirectR(this.hashCode, base, vecId)
        directR(base).connectLane(vecId, i, rBundle, backpressure)
      }
      // Temp fix for merged readers not recomputing port info
      Ledger.connectOutput(this.hashCode, p.xBarOutputs + outBase + vecId)
      output(p.xBarOutputs + outBase + vecId)
    }
  }
}

class StandardInterface(p: MemParams) extends MemInterface(p) {
  def connectLedger(op: StandardInterface)(implicit stack: List[KernelHash]): Unit = this.asInstanceOf[MemInterface].connectLedger(op.asInstanceOf[MemInterface])
}
object StandardInterfaceType extends MemInterfaceType 


class ShiftRegFileInterface(p: MemParams) extends MemInterface(p) {
  val dump_out = Vec(p.depth, Output(UInt(p.bitWidth.W)))
  val dump_in = Vec(p.depth, Input(UInt(p.bitWidth.W)))
  val dump_en = Input(Bool())

  def connectLedger(op: ShiftRegFileInterface)(implicit stack: List[KernelHash]): Unit = {
    dump_out <> op.dump_out
    dump_in <> op.dump_in
    dump_en <> op.dump_en
    this.asInstanceOf[MemInterface].connectLedger(op.asInstanceOf[MemInterface])
  }
}
object ShiftRegFileInterfaceType extends MemInterfaceType


class FIFOInterface(p: MemParams) extends MemInterface(p) {
  val full = Output(Bool())
  val almostFull = Output(Bool())
  val empty = Output(Bool())
  val almostEmpty = Output(Bool())
  val numel = Output(UInt(32.W))
  val accessActivesOut = Vec(p.numActives, Output(Bool()))
  val accessActivesIn = Vec(p.numActives, Input(Bool()))

  def connectAccessActivesIn(p: Int, e: Bool)(implicit stack: List[KernelHash]): Unit = {
    Ledger.connectAccessActivesIn(this.hashCode, p)
    accessActivesIn(p) := e
  }
  def connectLedger(op: FIFOInterface)(implicit stack: List[KernelHash]): Unit = {
    full <> op.full
    almostFull <> op.almostFull
    empty <> op.empty
    almostEmpty <> op.almostEmpty
    numel <> op.numel
    accessActivesOut <> op.accessActivesOut
    accessActivesIn <> op.accessActivesIn
    this.asInstanceOf[MemInterface].connectLedger(op.asInstanceOf[MemInterface])
  }
}
object FIFOInterfaceType extends MemInterfaceType
