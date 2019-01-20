package fringe

import chisel3._
import chisel3.util._
import fringe.utils._

class AppLoadData(val v: Int, val w: Int) extends Bundle {
  def this(tup: (Int, Int)) = this(tup._1, tup._2)
  val rdata = Vec(v, UInt(w.W))

  override def cloneType(): this.type = new AppLoadData(v, w).asInstanceOf[this.type]
}

class AppStoreData(val v: Int, val w: Int) extends Bundle {
  def this(tup: (Int, Int)) = this(tup._1, tup._2)
  val wdata = Vec(v, UInt(w.W))
  val wstrb = UInt(v.W)

  override def cloneType(): this.type = new AppStoreData(v, w).asInstanceOf[this.type]
}

class AppCommandDense(val addrWidth: Int = 64, val sizeWidth: Int = 32) extends Bundle {
  def this(tup: (Int, Int)) = this(tup._1, tup._2)
  val addr = UInt(addrWidth.W)
  val size = UInt(sizeWidth.W)

  override def cloneType(): this.type = new AppCommandDense(addrWidth, sizeWidth).asInstanceOf[this.type]
}

class AppCommandSparse(val v: Int, val addrWidth: Int = 64) extends Bundle {
  def this(tup: (Int, Int)) = this(tup._1, tup._2)
  val addr = Vec(v, UInt(addrWidth.W))

  override def cloneType(): this.type = new AppCommandSparse(v, addrWidth).asInstanceOf[this.type]
}

case class StreamParInfo(w: Int, v: Int, memChannel: Int)

class LoadStream(val p: StreamParInfo) extends Bundle {
  val cmd = Flipped(Decoupled(new AppCommandDense))
  val data = Decoupled(new AppLoadData(p.v, p.w))

  override def cloneType(): this.type = {
    new LoadStream(p).asInstanceOf[this.type]
  }
}

class StoreStream(val p: StreamParInfo) extends Bundle {
  val cmd = Flipped(Decoupled(new AppCommandDense))
  val data = Flipped(Decoupled(new AppStoreData(p.v, p.w)))
  val wresp = Decoupled(Bool())

  override def cloneType(): this.type = new StoreStream(p).asInstanceOf[this.type]
}

class GatherStream(val p: StreamParInfo) extends Bundle {
  val cmd = Flipped(Decoupled(new AppCommandSparse(p.v)))
  val data = Decoupled(Vec(p.v, UInt(p.w.W)))

  override def cloneType(): this.type = {
    new GatherStream(p).asInstanceOf[this.type]
  }
}

class ScatterCmdStream(p: StreamParInfo) extends Bundle {
  val addr = new AppCommandSparse(p.v)
  val wdata = Vec(p.v, UInt(p.w.W))

  override def cloneType(): this.type = new ScatterCmdStream(p).asInstanceOf[this.type]    
}

class ScatterStream(p: StreamParInfo) extends Bundle {
  val cmd = Flipped(Decoupled(new ScatterCmdStream(p)))
  val wresp = Decoupled(Bool())

  override def cloneType(): this.type = new ScatterStream(p).asInstanceOf[this.type]
}

class AppStreams(loadPar: List[StreamParInfo], storePar: List[StreamParInfo],
                 gatherPar: List[StreamParInfo], scatterPar: List[StreamParInfo]) extends Bundle {
  val loads = HVec.tabulate(loadPar.size){ i => new LoadStream(loadPar(i)) }
  val stores = HVec.tabulate(storePar.size){ i => new StoreStream(storePar(i)) }
  val gathers = HVec.tabulate(gatherPar.size){ i => new GatherStream(gatherPar(i)) }
  val scatters = HVec.tabulate(scatterPar.size){ i => new ScatterStream(scatterPar(i)) }

  override def cloneType(): this.type = new AppStreams(loadPar, storePar, gatherPar, scatterPar).asInstanceOf[this.type]
}

class ArgOut() extends Bundle {
  val port = Decoupled(UInt(64.W))
  val echo = Input(UInt(64.W))

  override def cloneType(): this.type = new ArgOut().asInstanceOf[this.type]
}

class MultiArgOut(nw: Int) extends Bundle {
  val port = Vec(nw, Decoupled(UInt(64.W)))
  val output = new Bundle{val echo = Input(UInt(64.W))}

  def connectXBarR(): UInt = output.echo
  def connectXBarW(p: Int, data: UInt, valid: Bool): Unit = {port(p).bits := data; port(p).valid := valid}
  def connectLedger(op: MultiArgOut): Unit = {
    if (Ledger.connections.contains(op.hashCode)) {
      val cxn = Ledger.connections(op.hashCode)
      cxn.xBarR.foreach{case RAddr(p,lane) => output.echo <> op.output.echo}
      cxn.xBarW.foreach{p => port(p) <> op.port(p)}
      Ledger.reset(op.hashCode)
    }
    else {port <> op.port; output <> op.output}
  }

  override def cloneType(): this.type = new MultiArgOut(nw).asInstanceOf[this.type]
}

class InstrCtr() extends Bundle {
  val cycs  = UInt(64.W)
  val iters = UInt(64.W)
  val stalls = UInt(64.W)
  val idles = UInt(64.W)

  override def cloneType(): this.type = new InstrCtr().asInstanceOf[this.type]
}

class DRAMCommand extends Bundle {
  val addr = UInt(64.W)
  val size = UInt(32.W)
  val rawAddr = UInt(64.W)
  val isWr = Bool()
  val tag = UInt(32.W)

  def getTag = tag.asTypeOf(new DRAMTag)
  def setTag(t: DRAMTag) = tag := t.asUInt

  override def cloneType(): this.type = new DRAMCommand().asInstanceOf[this.type]
}

class DRAMWdata(w: Int, v: Int) extends Bundle {
  val wdata = Vec(v, UInt(w.W))
  val wstrb = Vec(w*v/8, Bool())
  val wlast = Bool()

  override def cloneType(): this.type = new DRAMWdata(w, v).asInstanceOf[this.type]
}

class DRAMReadResponse(w: Int, v: Int) extends Bundle {
  val rdata = Vec(v, UInt(w.W))
  val tag = UInt(32.W)

  def getTag = tag.asTypeOf(new DRAMTag)

  override def cloneType(): this.type = new DRAMReadResponse(w, v).asInstanceOf[this.type]
}

class DRAMWriteResponse(w: Int, v: Int) extends Bundle {
  val tag = UInt(32.W)

  def getTag = tag.asTypeOf(new DRAMTag)

  override def cloneType(): this.type = new DRAMWriteResponse(w, v).asInstanceOf[this.type]
}

class DRAMStream(w: Int, v: Int) extends Bundle {
  val cmd = Decoupled(new DRAMCommand)
  val wdata = Decoupled(new DRAMWdata(w, v))
  val rresp = Flipped(Decoupled(new DRAMReadResponse(w, v)))
  val wresp = Flipped(Decoupled(new DRAMWriteResponse(w, v)))

  override def cloneType(): this.type = new DRAMStream(w, v).asInstanceOf[this.type]
}

class DRAMAddress(addrWidth: Int = 64) extends Bundle {
  val bits = UInt(addrWidth.W)

  val burstSize = globals.target.burstSizeBytes
  def burstTag = bits(bits.getWidth - 1, log2Ceil(burstSize))
  def wordOffset(w: Int) = bits(log2Ceil(burstSize) - 1, log2Ceil(w / 8))
  def burstAddr = Cat(burstTag, 0.U(log2Ceil(burstSize).W))

  override def cloneType(): this.type = {
    new DRAMAddress(addrWidth).asInstanceOf[this.type]
  }
}

object DRAMAddress {
  def apply(addr: UInt) = {
    val dramAddr = Wire(new DRAMAddress(addr.getWidth))
    dramAddr.bits := addr
    dramAddr
  }
}

class DRAMTag(w: Int = 32) extends Bundle {
  val uid = UInt((w - 9).W)
  // if the AXI command gets split, tag the last command here
  val cmdSplitLast = Bool()
  // Order is important here; streamId should be at [7:0] so all FPGA targets will see the
  // value on their AXI bus. uid may be truncated on targets with narrower bus.
  val streamID = UInt(8.W)

  override def cloneType(): this.type = new DRAMTag(w).asInstanceOf[this.type]
}

class GenericStreams(streamIns: List[StreamParInfo], streamOuts: List[StreamParInfo]) extends Bundle {
  val ins = HVec.tabulate(streamIns.size) { i => StreamIn(streamIns(i)) }
  val outs = HVec.tabulate(streamOuts.size) { i => StreamOut(streamOuts(i)) }

  override def cloneType(): this.type = new GenericStreams(streamIns, streamOuts).asInstanceOf[this.type]
}

class StreamIO(val p: StreamParInfo) extends Bundle {
  val data = UInt(p.w.W)
  val tag = UInt(p.w.W)
  val last = Bool()

  override def cloneType(): this.type = new StreamIO(p).asInstanceOf[this.type]
}

object StreamOut {
  def apply(p: StreamParInfo) = Decoupled(new StreamIO(p))
}

object StreamIn {
  def apply(p: StreamParInfo) = Flipped(Decoupled(new StreamIO(p)))
}

class DebugSignals extends Bundle {
  val num_enable = Output(UInt(32.W))
  val num_cmd_valid = Output(UInt(32.W))
  val num_cmd_valid_enable = Output(UInt(32.W))
  val num_cmd_ready = Output(UInt(32.W))
  val num_cmd_ready_enable = Output(UInt(32.W))
  val num_resp_valid = Output(UInt(32.W))
  val num_resp_valid_enable = Output(UInt(32.W))
  val num_rdata_enq = Output(UInt(32.W))
  val num_rdata_deq = Output(UInt(32.W))
  val num_app_rdata_ready = Output(UInt(32.W))

  // rdata enq
  val rdata_enq0_0 = Output(UInt(32.W))
  val rdata_enq0_1 = Output(UInt(32.W))
  val rdata_enq0_15 = Output(UInt(32.W))

  val rdata_enq1_0 = Output(UInt(32.W))
  val rdata_enq1_1 = Output(UInt(32.W))
  val rdata_enq1_15 = Output(UInt(32.W))

  // rdata deq
  val rdata_deq0_0 = Output(UInt(32.W))
  val rdata_deq0_1 = Output(UInt(32.W))
  val rdata_deq0_15 = Output(UInt(32.W))

  val rdata_deq1_0 = Output(UInt(32.W))
  val rdata_deq1_1 = Output(UInt(32.W))
  val rdata_deq1_15 = Output(UInt(32.W))

  // wdata enq
  val wdata_enq0_0 = Output(UInt(32.W))
  val wdata_enq0_1 = Output(UInt(32.W))
  val wdata_enq0_15 = Output(UInt(32.W))

  val wdata_enq1_0 = Output(UInt(32.W))
  val wdata_enq1_1 = Output(UInt(32.W))
  val wdata_enq1_15 = Output(UInt(32.W))

  // wdata deq
  val wdata_deq0_0 = Output(UInt(32.W))
  val wdata_deq0_1 = Output(UInt(32.W))
  val wdata_deq0_15 = Output(UInt(32.W))

  val wdata_deq1_0 = Output(UInt(32.W))
  val wdata_deq1_1 = Output(UInt(32.W))
  val wdata_deq1_15 = Output(UInt(32.W))

}

class HeapReq extends Bundle {
  val allocDealloc = Bool()
  val sizeAddr = UInt(64.W)
}

class HeapResp extends Bundle {
  val allocDealloc = Bool()
  val sizeAddr = UInt(64.W)
}

class HeapIO() extends Bundle {
  val req = Flipped(Valid(new HeapReq))
  val resp = Valid(new HeapResp)

  override def cloneType(): this.type = new HeapIO().asInstanceOf[this.type]
}

