package fringe.templates.memory

import chisel3._
import chisel3.util._
import fringe.utils.HVec
import fringe.Ledger._
import fringe.utils.DMap._
import fringe.utils.XMap._
import fringe.utils.NBufDMap._
import fringe.utils.NBufXMap._
import fringe.utils.implicits._
import fringe._



class R_XBar(val port_width: Int, val ofs_width:Int, val bank_width:List[Int]) extends Bundle {
  val banks = HVec.tabulate(port_width*bank_width.length){i => UInt(bank_width(i%bank_width.length).W)}
  val ofs = Vec(port_width, UInt(ofs_width.W))
  val en = Vec(port_width, Bool())
  val backpressure = Vec(port_width, Bool())

  def connectLane(lhs_lane: Int, rhs_lane: Int, rhs_port: R_XBar, f: Bool): Unit = {
    ofs(lhs_lane) := rhs_port.ofs(rhs_lane)
    bank_width.length.indices[Unit]{i => banks(i + lhs_lane*bank_width.length) := rhs_port.banks(i + rhs_lane*bank_width.length)}
    en(lhs_lane) := rhs_port.en(rhs_lane)
    backpressure(lhs_lane) := f
  }

  def forwardLane(lane: Int, rhs: R_XBar): Unit = {
    ofs(lane) := rhs.ofs(lane)
    bank_width.length.indices[Unit]{i => banks(i + lane*bank_width.length) := rhs.banks(i + lane*bank_width.length)}
    en(lane) := rhs.en(lane)
    backpressure(lane) := rhs.backpressure(lane)
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
  val backpressure = Vec(port_width, Bool())

  def connectLane(lhs_lane: Int, rhs_lane: Int, rhs_port: R_Direct, f: Bool): Unit = {
    ofs(lhs_lane) := rhs_port.ofs(rhs_lane)
    en(lhs_lane) := rhs_port.en(rhs_lane)
    backpressure(lhs_lane) := f
  }

  def forwardLane(lane: Int, rhs: R_Direct): Unit = {
    ofs(lane) := rhs.ofs(lane)
    en(lane) := rhs.en(lane)
    backpressure(lane) := rhs.backpressure(lane)
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
    val cxn = Ledger.lookup(op.hashCode)
    cxn.xBarR.foreach{case RAddr(p,lane) => xBarR(p).forwardLane(lane, op.xBarR(p))}
    cxn.xBarW.foreach{p => xBarW(p) <> op.xBarW(p)}
    cxn.directR.foreach{case RAddr(p,lane) => directR(p).forwardLane(lane, op.directR(p))}
    cxn.directW.foreach{p => directW(p) <> op.directW(p)}
    cxn.reset.foreach{p => reset <> op.reset}
    cxn.output.foreach{p => output(p) <> op.output(p)}
    Ledger.substitute(op.hashCode, this.hashCode)
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



class NBufInterface(val p: NBufParams) extends Bundle {
  val sEn = Vec(p.numBufs, Input(Bool()))
  val sDone = Vec(p.numBufs, Input(Bool()))
  val xBarW = HVec(Array.tabulate(1 max p.numXBarWPorts){i => Input(new W_XBar(p.xBarWMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths, p.bitWidth))})
  val xBarR = HVec(Array.tabulate(1 max p.numXBarRPorts){i => Input(new R_XBar(p.xBarRMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths))}) 
  val directW = HVec(Array.tabulate(1 max p.numDirectWPorts){i => 
      val dBanks = if (p.hasDirectW) {
        p.directWMux.toSeq.sortBy(_._1).toMap.values.flatMap(_.toSeq.sortBy(_._1).toMap.values.map(_._1)).toList(i)
      } else p.defaultDirect
      Input(new W_Direct(p.directWMux.accessPars.getOr1(i), p.ofsWidth, dBanks, p.bitWidth))
    })
  val directR = HVec(Array.tabulate(1 max p.numDirectRPorts){i => 
      val dBanks = if (p.hasDirectR) {
        p.directRMux.toSeq.sortBy(_._1).toMap.values.flatMap(_.toSeq.sortBy(_._1).toMap.values.map(_._1)).toList(i)
      } else p.defaultDirect
      Input(new R_Direct(p.directRMux.accessPars.getOr1(i), p.ofsWidth, dBanks))
    })
  val broadcastW = HVec(Array.tabulate(1 max p.numBroadcastWPorts){i => Input(new W_XBar(p.broadcastWMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths, p.bitWidth))})
  val broadcastR = HVec(Array.tabulate(1 max p.numBroadcastRPorts){i => Input(new R_XBar(p.broadcastRMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths))})
  val reset = Input(Bool())

  // FIFO Specific
  val accessActivesIn = Vec(p.numActives, Input(Bool()))
  val accessActivesOut = Vec(p.numActives, Output(Bool()))
  val full = Output(Bool())
  val almostFull = Output(Bool())
  val empty = Output(Bool())
  val almostEmpty = Output(Bool())
  val numel = Output(UInt(32.W))    
  
  val output = Vec(1 max p.totalOutputs, Output(UInt(p.bitWidth.W)))  

  def connectLedger(op: NBufInterface)(implicit stack: List[KernelHash]): Unit = {
    accessActivesOut.zip(op.accessActivesOut).foreach{case (l,r) => r := l}
    op.full := full
    op.almostFull := almostFull
    op.empty := empty
    op.almostEmpty := almostEmpty
    op.numel := numel
    val cxn = Ledger.lookup(op.hashCode)
    cxn.xBarR.foreach{case RAddr(p,lane) => xBarR(p).forwardLane(lane, op.xBarR(p))}
    cxn.xBarW.foreach{p => xBarW(p) <> op.xBarW(p)}
    cxn.directR.foreach{case RAddr(p,lane) => directR(p).forwardLane(lane, op.directR(p))}
    cxn.directW.foreach{p => directW(p) <> op.directW(p)}
    cxn.output.foreach{p => output(p) <> op.output(p)}
    cxn.broadcastR.foreach{case RAddr(p,lane) => broadcastR(p).forwardLane(lane, op.broadcastR(p))}
    cxn.broadcastW.foreach{p => broadcastW(p) <> op.broadcastW(p)}
    cxn.reset.foreach{p => reset <> op.reset}
    cxn.stageCtrl.foreach{p => sEn(p) := op.sEn(p); sDone(p) := op.sDone(p)}
    Ledger.substitute(op.hashCode, this.hashCode)
  }

  def connectReset(r: Bool)(implicit stack: List[KernelHash]): Unit = {
    reset := r
    Ledger.connectReset(this.hashCode, 0)
  }
  def connectAccessActivesIn(p: Int, e: Bool)(implicit stack: List[KernelHash]): Unit = {
    Ledger.connectAccessActivesIn(this.hashCode, p)
    accessActivesIn(p) := e
  }

  var usedMuxPorts = List[(String,(Int,Int,Int,Int,Int))]() // Check if the bufferPort, muxPort, muxAddr, lane, castgrp is taken for this connection style (xBar or direct)
  def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxAddr: (Int, Int))(implicit stack: List[KernelHash]): Unit = {
    assert(p.hasXBarW)
    assert(!usedMuxPorts.contains(("XBarW", (bufferPort,muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to XBarW port ($bufferPort,$muxAddr) twice!")
    usedMuxPorts ::= ("XBarW", (bufferPort,muxAddr._1,muxAddr._2,0,0))
    val bufferBase = p.xBarWMux.accessParsBelowBufferPort(bufferPort).length
    val muxBase = p.xBarWMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2,0).length
    Ledger.connectXBarW(this.hashCode, bufferBase + muxBase)
    xBarW(bufferBase + muxBase) := wBundle
  }

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {
    assert(p.hasXBarR)
    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
      val castgrp = if (ignoreCastInfo) 0 else cg
      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
      val bufferBase = p.xBarRMux.accessParsBelowBufferPort(bufferPort).length
      val muxBase = p.xBarRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).length
      val outputBufferBase = p.xBarRMux.accessParsBelowBufferPort(bufferPort).sum
      val outputMuxBase = p.xBarRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).sum
      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
      if (bid == 0) {
        if (ignoreCastInfo && i == 0) {
          assert(!usedMuxPorts.contains(("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,0))), s"Attempted to connect to XBarR port ($bufferPort,$muxAddr) twice!")
          usedMuxPorts ::= ("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,0))
        } else if (!ignoreCastInfo) {
          assert(!usedMuxPorts.contains(("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))), s"Attempted to connect to XBarR port ($bufferPort,$muxAddr) twice!")
          usedMuxPorts ::= ("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))
        }
        Ledger.connectXBarR(this.hashCode, bufferBase + muxBase, vecId)
        xBarR(bufferBase + muxBase).connectLane(vecId,i,rBundle, backpressure)
      }
      Ledger.connectOutput(this.hashCode, outputBufferBase + outputMuxBase + vecId)
      output(outputBufferBase + outputMuxBase + vecId)
    }
  }

  def connectBroadcastWPort(wBundle: W_XBar, muxAddr: (Int, Int))(implicit stack: List[KernelHash]): Unit = {
    val muxBase = p.broadcastWMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2,0).length
    broadcastW(muxBase) := wBundle
    Ledger.connectBroadcastW(this.hashCode, muxBase)
  }

  def connectBroadcastRPort(rBundle: R_XBar, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectBroadcastRPort(rBundle, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
  def connectBroadcastRPort(rBundle: R_XBar, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {
    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
      val castgrp = if (ignoreCastInfo) 0 else cg
      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
      val muxBase = p.broadcastRMux.accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).length
      val xBarRBase = p.xBarRMux.accessPars.length
      val directRBase = p.directRMux.accessPars.length
      val outputXBarRBase = p.xBarRMux.accessPars.sum
      val outputDirectRBase = p.directRMux.accessPars.sum
      val outputMuxBase = p.broadcastRMux.accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).sum
      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
      if (bid == 0) {
        Ledger.connectBroadcastR(this.hashCode, muxBase, vecId)
        broadcastR(muxBase).connectLane(vecId,i,rBundle, backpressure)
      }
      Ledger.connectOutput(this.hashCode, outputXBarRBase + outputDirectRBase + outputMuxBase + vecId)
      output(outputXBarRBase + outputDirectRBase + outputMuxBase + vecId)
    }
  }

  def connectDirectWPort(wBundle: W_Direct, bufferPort: Int, muxAddr: (Int, Int))(implicit stack: List[KernelHash]): Unit = {
    assert(p.hasDirectW)
    assert(!usedMuxPorts.contains(("directW", (bufferPort,muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to directW port ($bufferPort,$muxAddr) twice!")
    usedMuxPorts ::= ("directW", (bufferPort,muxAddr._1,muxAddr._2,0,0))
    val bufferBase = p.directWMux.accessParsBelowBufferPort(bufferPort).length 
    val muxBase = p.directWMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2, 0).length
    Ledger.connectDirectW(this.hashCode, bufferBase + muxBase)
    directW(bufferBase + muxBase) := wBundle
  }

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectDirectRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {
    assert(p.hasDirectR)
    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
      val castgrp = if (ignoreCastInfo) 0 else cg
      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
      val bufferBase = p.directRMux.accessParsBelowBufferPort(bufferPort).length
      val xBarRBase = p.xBarRMux.accessPars.length
      val muxBase = p.directRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).length
      val outputBufferBase = p.directRMux.accessParsBelowBufferPort(bufferPort).sum
      val outputXBarRBase = p.xBarRMux.accessPars.sum
      val outputMuxBase = p.directRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).sum
      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
      if (bid == 0) {
        if (ignoreCastInfo && i == 0) {
          assert(!usedMuxPorts.contains(("directR", (bufferPort,muxAddr._1,effectiveOfs,i,0))), s"Attempted to connect to directR port ($bufferPort,$muxAddr) twice!")
          usedMuxPorts ::= ("directR", (bufferPort,muxAddr._1,effectiveOfs,i,0))
        } else if (!ignoreCastInfo) {
          assert(!usedMuxPorts.contains(("directR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))), s"Attempted to connect to directR port ($bufferPort,$muxAddr) twice!")
          usedMuxPorts ::= ("directR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))
        }
        Ledger.connectDirectR(this.hashCode, bufferBase + muxBase, vecId)
        directR(bufferBase + muxBase).connectLane(vecId,i,rBundle, backpressure)
      }
      Ledger.connectOutput(this.hashCode, outputXBarRBase + outputBufferBase + outputMuxBase + vecId)
      output(outputXBarRBase + outputBufferBase + outputMuxBase + vecId)
    }
  }

  def connectStageCtrl(done: Bool, en: Bool, port: Int)(implicit stack: List[KernelHash]): Unit = {
    Ledger.connectStageCtrl(this.hashCode, port)
    sEn(port) := en
    sDone(port) := done
  }


}


class FixFMAAccumBundle(numWriters: Int, d: Int, f: Int) extends Bundle {
  val input = Vec(numWriters, new Bundle{
    val input1 = Input(UInt((d+f).W))
    val input2 = Input(UInt((d+f).W))
    val enable = Input(Bool())
    val last = Input(Bool())
    val first = Input(Bool())
  })
  val reset = Input(Bool())
  val output = Output(UInt((d+f).W))

  def connectLedger(op: FixFMAAccumBundle)(implicit stack: List[KernelHash]): Unit = {
    val cxn = Ledger.lookup(op.hashCode)
    cxn.output.foreach{case p => output <> op.output}
    cxn.xBarW.foreach{p => input(p) <> op.input(p)}
    cxn.reset.foreach{p => reset <> op.reset}
    Ledger.substitute(op.hashCode, this.hashCode)
  }

  def connectReset(r: Bool)(implicit stack: List[KernelHash]): Unit = {
    reset := r
    Ledger.connectReset(this.hashCode, 0)
  }
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {Ledger.connectXBarR(this.hashCode, 0, 0);Seq(output)}
  def connectXBarWPort(index: Int, data1: UInt, data2: UInt, en: Bool, last: Bool, first: Bool)(implicit stack: List[KernelHash]): Unit = {
    input(index).input1 := data1
    input(index).input2 := data2
    input(index).enable := en
    input(index).last := last
    input(index).first := first
    Ledger.connectXBarW(this.hashCode, index)
  }

  override def cloneType(): this.type = new FixFMAAccumBundle(numWriters, d, f).asInstanceOf[this.type]
}


class FixOpAccumBundle(numWriters: Int, d: Int, f: Int) extends Bundle {
  val input = Vec(numWriters, new Bundle{
    val input1 = Input(UInt((d+f).W))
    val enable = Input(Bool())
    val last = Input(Bool())
    val first = Input(Bool())
  })
  val reset = Input(Bool())
  val output = Output(UInt((d+f).W))

  def connectLedger(op: FixOpAccumBundle)(implicit stack: List[KernelHash]): Unit = {
    val cxn = Ledger.lookup(op.hashCode)
    cxn.output.foreach{case p => output <> op.output}
    cxn.xBarW.foreach{p => input(p) <> op.input(p)}
    cxn.reset.foreach{p => reset <> op.reset}
    Ledger.substitute(op.hashCode, this.hashCode)
  }
  def connectReset(r: Bool)(implicit stack: List[KernelHash]): Unit = {
    reset := r
    Ledger.connectReset(this.hashCode, 0)
  }
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {Ledger.connectOutput(this.hashCode, 0);Seq(output)}
  def connectXBarWPort(index: Int, data1: UInt, en: Bool, last: Bool, first: Bool)(implicit stack: List[KernelHash]): Unit = {
    input(index).input1 := data1
    input(index).enable := en
    input(index).last := last
    input(index).first := first
    Ledger.connectXBarW(this.hashCode, index)
  }

  override def cloneType(): this.type = new FixOpAccumBundle(numWriters, d, f).asInstanceOf[this.type]
}

class MultiArgOut(nw: Int) extends Bundle {
  val port = Vec(nw, Decoupled(UInt(64.W)))
  val output = new Bundle{val echo = Input(UInt(64.W))}

  def connectXBarR(): UInt = output.echo
  def connectXBarW(p: Int, data: UInt, valid: Bool)(implicit stack: List[KernelHash]): Unit = {port(p).bits := data; port(p).valid := valid; Ledger.connectXBarW(this.hashCode, p)}
  def connectLedger(op: MultiArgOut)(implicit stack: List[KernelHash]): Unit = {
    val cxn = Ledger.lookup(op.hashCode)
    cxn.xBarR.foreach{case RAddr(p,lane) => output.echo <> op.output.echo}
    cxn.xBarW.foreach{p => port(p) <> op.port(p)}
    Ledger.substitute(op.hashCode, this.hashCode)
    output <> op.output // ?
  }

  override def cloneType(): this.type = new MultiArgOut(nw).asInstanceOf[this.type]
}
