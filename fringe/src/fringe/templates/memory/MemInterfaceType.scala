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
import fringe.utils.{PortInfo,Access}
import emul.ResidualGenerator._

class R_Port(val port_width: Int, val ofs_width:Int, val bank_width:List[Int], val data_width: Int, val visibleBanks: List[List[ResidualGenerator]]) extends Bundle {
  def this(a: PortInfo) = this(a.portWidth, a.ofsWidth, a.banksWidth, a.dataWidth, a.visibleBanks)

  val banks = HVec.tabulate(port_width*bank_width.length){i => Input(UInt(bank_width(i%bank_width.length).W))}
  val ofs = Vec(port_width, Input(UInt(ofs_width.W)))
  val en = Vec(port_width, Input(Bool()))
  val backpressure = Input(Bool())
  val output = Vec(port_width, Output(UInt(data_width.W)))

  def connectLane(lane: Int, rhs_port: R_Port, f: Bool): UInt = {
    ofs(lane) := rhs_port.ofs(lane)
    bank_width.length.indices[Unit]{i => banks(i + lane*bank_width.length) := rhs_port.banks(i + lane*bank_width.length)}
    en(lane) := rhs_port.en(lane)
    backpressure := f
    output(lane)
  }

  def forwardLane(lane: Int, rhs: R_Port): Unit = {
    ofs(lane) := rhs.ofs(lane)
    bank_width.length.indices[Unit]{i => banks(i + lane*bank_width.length) := rhs.banks(i + lane*bank_width.length)}
    en(lane) := rhs.en(lane)
    backpressure := rhs.backpressure
    rhs.output(lane) := output(lane)
  }

  override def cloneType = (new R_Port(port_width, ofs_width, bank_width, data_width, visibleBanks)).asInstanceOf[this.type] // See chisel3 bug 358
}

class W_Port(val port_width: Int, val ofs_width:Int, val bank_width:List[Int], val data_width:Int, val visibleBanks: List[List[ResidualGenerator]]) extends Bundle {
  def this(a: PortInfo) = this(a.portWidth, a.ofsWidth, a.banksWidth, a.dataWidth, a.visibleBanks)

  val banks = HVec.tabulate(port_width*bank_width.length){i => Input(UInt(bank_width(i%bank_width.length).W))}
  val ofs = Vec(port_width, Input(UInt(ofs_width.W)))
  val data = Vec(port_width, Input(UInt(data_width.W)))
  val reset = Input(Bool()) // For FF
  val init = Input(UInt(data_width.W)) // For FF
  val shiftEn = Vec(port_width, Input(Bool())) // For ShiftRegFile
  val en = Vec(port_width, Input(Bool()))

  override def cloneType = (new W_Port(port_width, ofs_width, bank_width, data_width, visibleBanks)).asInstanceOf[this.type] // See chisel3 bug 358
}


sealed trait MemInterfaceType

sealed abstract class MemInterface(val p: MemParams) extends Bundle {
  val rPort = HVec(p.RMapping.map{x => new R_Port(x.port)})
  val wPort = HVec(p.WMapping.map{x => new W_Port(x.port)})
  val reset = Input(Bool())

  def connectLedger(op: MemInterface)(implicit stack: List[KernelHash]): Unit = {
    if (stack.isEmpty) this <> op
    else {
      val cxn = Ledger.lookup(op.hashCode)
      cxn.rPort.foreach{case p => rPort(p) <> op.rPort(p)}
      cxn.wPort.foreach{case p => wPort(p) <> op.wPort(p)}
      cxn.reset.foreach{p => reset <> op.reset}
      cxn.accessActivesIn.foreach{p => this.asInstanceOf[FIFOInterface].accessActivesIn(p) <> op.asInstanceOf[FIFOInterface].accessActivesIn(p)}
      Ledger.substitute(op.hashCode, this.hashCode)
    }
  }

  def connectReset(r: Bool)(implicit stack: List[KernelHash]): Unit = {
    reset := r
    Ledger.connectReset(this.hashCode, 0)
  }


  def connectWPort(
    accHash: Int, 
    banks: Seq[UInt],
    ofs: Seq[UInt],
    data: Seq[UInt],
    en: Seq[Bool]
  )(implicit stack: List[KernelHash]): Unit = {
    val base = p.lookupWBase(accHash)
    wPort(base).banks.zip(banks).foreach{case (a,b) => a := b}
    wPort(base).ofs.zip(ofs).foreach{case (a,b) => a := b}
    wPort(base).data.zip(data).foreach{case (a,b) => a := b}
    wPort(base).reset := reset
    if (p.lookupW(accHash).shiftAxis.isDefined) wPort(base).shiftEn.zip(en).foreach{case (a,b) => a := b}
    else wPort(base).en.zip(en).foreach{case (a,b) => a := b}
    Ledger.connectWPort(this.hashCode, base)
  }

  def connectRPort(
    accHash: Int, 
    banks: Seq[UInt],
    ofs: Seq[UInt],
    backpressure: Bool,
    en: Seq[Bool],
    ignoreCastInfo: Boolean
  )(implicit stack: List[KernelHash]): Seq[UInt] = {
    val base = p.lookupRBase(accHash)
    rPort(base).banks.zip(banks).foreach{case (a,b) => a := b}
    rPort(base).ofs.zip(ofs).foreach{case (a,b) => a := b}
    rPort(base).backpressure := backpressure
    rPort(base).en.zip(en).foreach{case (a,b) => a := b}
    Ledger.connectRPort(this.hashCode, base)
    rPort(base).output
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
    op.full <> full
    op.almostFull <> almostFull
    op.empty <> empty
    op.almostEmpty <> almostEmpty
    op.numel <> numel
    op.accessActivesOut <> accessActivesOut
    this.asInstanceOf[MemInterface].connectLedger(op.asInstanceOf[MemInterface])
  }
}
object FIFOInterfaceType extends MemInterfaceType



class NBufInterface(val np: NBufParams) extends FIFOInterface(np.p) {
  val sEn = Vec(np.numBufs, Input(Bool()))
  val sDone = Vec(np.numBufs, Input(Bool()))

  def connectLedger(op: NBufInterface)(implicit stack: List[KernelHash]): Unit = {
    if (stack.isEmpty) this <> op
    else {
      accessActivesOut.zip(op.accessActivesOut).foreach{case (l,r) => r := l}
      op.full := full
      op.almostFull := almostFull
      op.empty := empty
      op.almostEmpty := almostEmpty
      op.numel := numel
      val cxn = Ledger.lookup(op.hashCode)
      cxn.stageCtrl.foreach{p => sEn(p) := op.sEn(p); sDone(p) := op.sDone(p)}
      super.connectLedger(op)
    }
  }

//   def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxAddr: (Int, Int))(implicit stack: List[KernelHash]): Unit = {
//     assert(p.hasXBarW)
//     assert(!usedMuxPorts.contains(("XBarW", (bufferPort,muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to XBarW port ($bufferPort,$muxAddr) twice!")
//     usedMuxPorts ::= ("XBarW", (bufferPort,muxAddr._1,muxAddr._2,0,0))
//     val bufferBase = p.xBarWMux.accessParsBelowBufferPort(bufferPort).length
//     val muxBase = p.xBarWMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2,0).length
//     Ledger.connectXBarW(this.hashCode, bufferBase + muxBase)
//     xBarW(bufferBase + muxBase) := wBundle
//   }

//   def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
//   def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {
//     assert(p.hasXBarR)
//     castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
//       val castgrp = if (ignoreCastInfo) 0 else cg
//       val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
//       val bufferBase = p.xBarRMux.accessParsBelowBufferPort(bufferPort).length
//       val muxBase = p.xBarRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).length
//       val outputBufferBase = p.xBarRMux.accessParsBelowBufferPort(bufferPort).sum
//       val outputMuxBase = p.xBarRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).sum
//       val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
//       if (bid == 0) {
//         if (ignoreCastInfo && i == 0) {
//           assert(!usedMuxPorts.contains(("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,0))), s"Attempted to connect to XBarR port ($bufferPort,$muxAddr) twice!")
//           usedMuxPorts ::= ("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,0))
//         } else if (!ignoreCastInfo) {
//           assert(!usedMuxPorts.contains(("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))), s"Attempted to connect to XBarR port ($bufferPort,$muxAddr) twice!")
//           usedMuxPorts ::= ("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))
//         }
//         Ledger.connectXBarR(this.hashCode, bufferBase + muxBase, vecId)
//         xBarR(bufferBase + muxBase).connectLane(vecId,i,rBundle, backpressure)
//       }
//       Ledger.connectOutput(this.hashCode, outputBufferBase + outputMuxBase + vecId)
//       output(outputBufferBase + outputMuxBase + vecId)
//     }
//   }

//   def connectBroadcastWPort(wBundle: W_XBar, muxAddr: (Int, Int))(implicit stack: List[KernelHash]): Unit = {
//     val muxBase = p.broadcastWMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2,0).length
//     broadcastW(muxBase) := wBundle
//     Ledger.connectBroadcastW(this.hashCode, muxBase)
//   }

//   def connectBroadcastRPort(rBundle: R_XBar, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectBroadcastRPort(rBundle, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
//   def connectBroadcastRPort(rBundle: R_XBar, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {
//     castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
//       val castgrp = if (ignoreCastInfo) 0 else cg
//       val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
//       val muxBase = p.broadcastRMux.accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).length
//       val xBarRBase = p.xBarRMux.accessPars.length
//       val directRBase = p.directRMux.accessPars.length
//       val outputXBarRBase = p.xBarRMux.accessPars.sum
//       val outputDirectRBase = p.directRMux.accessPars.sum
//       val outputMuxBase = p.broadcastRMux.accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).sum
//       val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
//       if (bid == 0) {
//         Ledger.connectBroadcastR(this.hashCode, muxBase, vecId)
//         broadcastR(muxBase).connectLane(vecId,i,rBundle, backpressure)
//       }
//       Ledger.connectOutput(this.hashCode, outputXBarRBase + outputDirectRBase + outputMuxBase + vecId)
//       output(outputXBarRBase + outputDirectRBase + outputMuxBase + vecId)
//     }
//   }

//   def connectDirectWPort(wBundle: W_Direct, bufferPort: Int, muxAddr: (Int, Int))(implicit stack: List[KernelHash]): Unit = {
//     assert(p.hasDirectW)
//     assert(!usedMuxPorts.contains(("directW", (bufferPort,muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to directW port ($bufferPort,$muxAddr) twice!")
//     usedMuxPorts ::= ("directW", (bufferPort,muxAddr._1,muxAddr._2,0,0))
//     val bufferBase = p.directWMux.accessParsBelowBufferPort(bufferPort).length 
//     val muxBase = p.directWMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2, 0).length
//     Ledger.connectDirectW(this.hashCode, bufferBase + muxBase)
//     directW(bufferBase + muxBase) := wBundle
//   }

//   def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean)(implicit stack: List[KernelHash]): Seq[UInt] = {connectDirectRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}

//   def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool)(implicit stack: List[KernelHash]): Seq[UInt] = {
//     assert(p.hasDirectR)
//     castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
//       val castgrp = if (ignoreCastInfo) 0 else cg
//       val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
//       val bufferBase = p.directRMux.accessParsBelowBufferPort(bufferPort).length
//       val xBarRBase = p.xBarRMux.accessPars.length
//       val muxBase = p.directRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).length
//       val outputBufferBase = p.directRMux.accessParsBelowBufferPort(bufferPort).sum
//       val outputXBarRBase = p.xBarRMux.accessPars.sum
//       val outputMuxBase = p.directRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).sum
//       val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
//       if (bid == 0) {
//         if (ignoreCastInfo && i == 0) {
//           assert(!usedMuxPorts.contains(("directR", (bufferPort,muxAddr._1,effectiveOfs,i,0))), s"Attempted to connect to directR port ($bufferPort,$muxAddr) twice!")
//           usedMuxPorts ::= ("directR", (bufferPort,muxAddr._1,effectiveOfs,i,0))
//         } else if (!ignoreCastInfo) {
//           assert(!usedMuxPorts.contains(("directR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))), s"Attempted to connect to directR port ($bufferPort,$muxAddr) twice!")
//           usedMuxPorts ::= ("directR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))
//         }
//         Ledger.connectDirectR(this.hashCode, bufferBase + muxBase, vecId)
//         directR(bufferBase + muxBase).connectLane(vecId,i,rBundle, backpressure)
//       }
//       Ledger.connectOutput(this.hashCode, outputXBarRBase + outputBufferBase + outputMuxBase + vecId)
//       output(outputXBarRBase + outputBufferBase + outputMuxBase + vecId)
//     }
//   }

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
    if (stack.isEmpty) this <> op
    else {
      val cxn = Ledger.lookup(op.hashCode)
      output <> op.output
      cxn.rPort.foreach{case p => output <> op.output} // Unused
      cxn.wPort.foreach{case p => input(p) <> op.input(p)}
      cxn.reset.foreach{p => reset <> op.reset}
      Ledger.substitute(op.hashCode, this.hashCode)
    }
  }

  def connectReset(r: Bool)(implicit stack: List[KernelHash]): Unit = {
    reset := r
    Ledger.connectReset(this.hashCode, 0)
  }
  def connectRPort(
    accHash: Int, 
    banks: Seq[UInt],
    ofs: Seq[UInt],
    backpressure: Bool,
    en: Seq[Bool],
    ignoreCastInfo: Boolean
  )(implicit stack: List[KernelHash]): Seq[UInt] = {
    Ledger.connectRPort(this.hashCode, 0)
    Seq(output)
  }

  def connectWPort(
    index: Int, 
    data1: UInt,
    data2: UInt,
    en: Bool,
    last: Bool,
    first: Bool
  )(implicit stack: List[KernelHash]): Unit = {
    input(index).input1 := data1
    input(index).input2 := data2
    input(index).enable := en
    input(index).last := last
    input(index).first := first
    Ledger.connectWPort(this.hashCode, index)
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
    if (stack.isEmpty) this <> op
    else {
      val cxn = Ledger.lookup(op.hashCode)
      output <> op.output
      cxn.rPort.foreach{case p => output <> op.output} // Unused
      cxn.wPort.foreach{case p => input(p) <> op.input(p)}
      cxn.reset.foreach{p => reset <> op.reset}
      Ledger.substitute(op.hashCode, this.hashCode)
    }
  }
  def connectReset(r: Bool)(implicit stack: List[KernelHash]): Unit = {
    reset := r
    Ledger.connectReset(this.hashCode, 0)
  }
  def connectRPort(
    accHash: Int, 
    banks: Seq[UInt],
    ofs: Seq[UInt],
    backpressure: Bool,
    en: Seq[Bool],
    ignoreCastInfo: Boolean
  )(implicit stack: List[KernelHash]): Seq[UInt] = {
    Ledger.connectRPort(this.hashCode, 0)
    Seq(output)
  }

  def connectWPort(
    index: Int, 
    data: UInt,
    en: Bool,
    last: Bool,
    first: Bool
  )(implicit stack: List[KernelHash]): Unit = {
    input(index).input1 := data
    input(index).enable := en
    input(index).last := last
    input(index).first := first
    Ledger.connectWPort(this.hashCode, index)
  }

  override def cloneType(): this.type = new FixOpAccumBundle(numWriters, d, f).asInstanceOf[this.type]
}

class MultiArgOut(nw: Int) extends Bundle {
  val port = Vec(nw, Decoupled(UInt(64.W)))
  val output = new Bundle{val echo = Input(UInt(64.W))}

  def connectRPort(): UInt = output.echo
  def connectWPort(p: Int, data: UInt, valid: Bool)(implicit stack: List[KernelHash]): Unit = {port(p).bits := data; port(p).valid := valid; Ledger.connectWPort(this.hashCode, p)}
  def connectLedger(op: MultiArgOut)(implicit stack: List[KernelHash]): Unit = {
    if (stack.isEmpty) this <> op
    else {
      val cxn = Ledger.lookup(op.hashCode)
      cxn.rPort.foreach{p => output.echo <> op.output.echo}
      cxn.wPort.foreach{p => port(p) <> op.port(p)}
      Ledger.substitute(op.hashCode, this.hashCode)
      output <> op.output // ?
    }
  }

  override def cloneType(): this.type = new MultiArgOut(nw).asInstanceOf[this.type]
}
