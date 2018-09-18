package fringe

import chisel3._
import chisel3.util._
import templates._

class GatherBuffer(
  val streamW: Int,
  val d: Int,
  val streamV: Int,
  val burstSize: Int,
  val addrWidth: Int,
  val loadCmd: Command,
  val readResp: DRAMReadResponse
) extends Module {

  class MetaData extends Bundle {
    val valid = Bool()
    val addr = new BurstAddr(addrWidth, streamW, burstSize)

    override def cloneType(): this.type = {
      new MetaData().asInstanceOf[this.type]
    }
  }
  class GatherData extends Bundle {
    val data = UInt(streamW.W)
    val meta = new MetaData

    override def cloneType(): this.type = {
      new GatherData().asInstanceOf[this.type]
    }
  }

  class GatherBufferIO extends Bundle {
    val fifo = new FIFOBaseIO(UInt(streamW.W), d, streamV)
    val rresp = Input(Valid(readResp))
    val cmd = Input(Valid(loadCmd))
    val hit = Output(Bool())
    val complete = Output(Bool())
  }
  
  val io = IO(new GatherBufferIO)
  
  val f = Module(new FIFOCore(new GatherData, d, streamV, true))
  val config = Wire(new FIFOOpcode(d, streamV))
  config.chainRead := true.B
  config.chainWrite := true.B
  f.io.config := config

  val b = f.io.banks match {
    case Some(b) => b
    case None => throw new Exception
  }

  val cmdAddr = Wire(new BurstAddr(addrWidth, streamW, burstSize))
  cmdAddr.bits := io.cmd.bits.addr

  io.hit := b.map { _.map { i =>
    i.valid & (cmdAddr.burstTag === i.rdata.meta.addr.burstTag) & ~i.rdata.meta.valid
  }.reduce { _|_ } }.reduce { _|_ } & io.cmd.valid

  val data = Wire(new GatherData)
  data.meta.valid := false.B
  data.meta.addr.bits := io.cmd.bits.addr
  f.io.enq(0) := data
  f.io.enqVld := io.cmd.valid
  
  val crossbars = List.tabulate(f.bankSize) { i => 
    val rrespVec = Utils.vecWidthConvert(io.rresp.bits.rdata, streamW)
    val switch = SwitchParams(rrespVec.length, streamV)
    val config = Wire(CrossbarConfig(switch))

    val valid = b(i).map { _.valid }
    val rdata = b(i).map { _.rdata }
    val wdata = b(i).map { _.wdata }
    val wen = b(i).map { _.wen }
    val respHits = rdata.map { _.meta }.zip(valid).map { case (m, v) =>
      v & io.rresp.valid & (m.addr.burstTag === io.rresp.bits.tag.uid) & ~m.valid
    }

    config.outSelect.zip(rdata).foreach { case (s, d) =>
      s := d.meta.addr.wordOffset
    }
    wen.zip(respHits).map { case (w, h) => w := h }

    val core = Module(new CrossbarCore(UInt(streamW.W), switch))
    core.io.ins := rrespVec
    wdata.zip(core.io.outs).foreach { case (w, o) => w.data := o }
    wdata.zip(rdata).map { case (w, r) =>
      val m = Wire(new MetaData)
      m.valid := true.B
      m.addr := r.meta.addr
      w.meta := m
    }
    core.io.config := config
    core
  }

  io.fifo.deq := f.io.deq.map { _.data }
  io.fifo.full := f.io.full
  io.complete := f.io.deq(0).meta.valid & ~f.io.empty
  io.fifo.empty := f.io.empty
  
  f.io.deqVld := io.fifo.deqVld & io.complete
}
