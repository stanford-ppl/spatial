package fringe.templates.dramarbiter

import chisel3._
import chisel3.util._

import fringe._
import fringe.globals._

abstract class StreamController(
  info: StreamParInfo,
  dramStream: DRAMStream
) extends Module {
  class StreamControllerIO extends Bundle {
    val dram = dramStream.cloneType
  }

  val io: StreamControllerIO

  // app stream sends bytes but DRAM stream expects bursts
  def sizeBytesToSizeBurst(size: UInt) = {
    size >> log2Ceil(target.burstSizeBytes)
  }
}

class StreamControllerLoad(
  info: StreamParInfo,
  dramStream: DRAMStream,
  app: LoadStream
) extends StreamController(info, dramStream) {

  class StreamControllerLoadIO extends StreamControllerIO {
    val load = app.cloneType
  }

  val io = IO(new StreamControllerLoadIO)

  io <> DontCare
  val cmd = Module(new FIFO(app.cmd.bits, target.bufferDepth))
  cmd.io <> DontCare
  cmd.io.in.valid := io.load.cmd.valid
  cmd.io.in.bits := io.load.cmd.bits
  io.load.cmd.ready := cmd.io.in.ready
  cmd.io.out.ready := io.dram.cmd.ready
  io.dram.cmd.valid := cmd.io.out.valid
  
  io.dram.cmd.bits.addr := cmd.io.out.bits.addr
  io.dram.cmd.bits.rawAddr := cmd.io.out.bits.addr
  io.dram.cmd.bits.size := sizeBytesToSizeBurst(cmd.io.out.bits.size)
  io.dram.cmd.bits.isWr := false.B

  val rdata = Module(new FIFOWidthConvert(EXTERNAL_W, EXTERNAL_V, info.w, info.v, target.bufferDepth))
  rdata.io <> DontCare
  rdata.io.in.bits.data := io.dram.rresp.bits.rdata
  rdata.io.in.valid := io.dram.rresp.valid
  io.dram.rresp.ready := rdata.io.in.ready

  io.load.rdata.valid := rdata.io.out.valid
  io.load.rdata.bits := rdata.io.out.bits.data
  rdata.io.out.ready := io.load.rdata.ready
}

class StreamControllerStore(
  info: StreamParInfo,
  dramStream: DRAMStream,
  app: StoreStream
) extends StreamController(info, dramStream) {

  class StreamControllerStoreIO extends StreamControllerIO {
    val store = app.cloneType
  }

  val io = IO(new StreamControllerStoreIO)
  io <> DontCare

  val cmd = Module(new FIFO(app.cmd.bits, target.bufferDepth))
  cmd.io <> DontCare
  cmd.io.in.valid := io.store.cmd.valid
  cmd.io.in.bits := io.store.cmd.bits
  io.store.cmd.ready := cmd.io.in.ready
  cmd.io.out.ready := io.dram.cmd.ready
  io.dram.cmd.valid := cmd.io.out.valid
  
  io.dram.cmd.bits.addr := cmd.io.out.bits.addr
  io.dram.cmd.bits.rawAddr := cmd.io.out.bits.addr
  io.dram.cmd.bits.size := sizeBytesToSizeBurst(cmd.io.out.bits.size)
  io.dram.cmd.bits.isWr := true.B

  val wdata = Module(new FIFOWidthConvert(info.w, info.v, EXTERNAL_W, EXTERNAL_V, target.bufferDepth))
  wdata.io <> DontCare
  wdata.io.in.valid := io.store.wdata.valid
  wdata.io.in.bits.data := io.store.wdata.bits
  wdata.io.in.bits.strobe := io.store.wstrb.bits
  io.store.wdata.ready := wdata.io.in.ready
  io.store.wstrb.ready := wdata.io.in.ready

  io.dram.wdata.valid := wdata.io.out.valid
  io.dram.wdata.bits.wdata := wdata.io.out.bits.data
  io.dram.wdata.bits.wstrb := VecInit(wdata.io.out.bits.strobe.asBools).reverse
  wdata.io.out.ready := io.dram.wdata.ready

  val wresp = Module(new FIFO(Bool(), target.bufferDepth))
  wresp.io <> DontCare
  wresp.io.in.valid := io.dram.wresp.valid
  wresp.io.in.bits := true.B
  io.dram.wresp.ready := wresp.io.in.ready

  io.store.wresp.valid := wresp.io.out.valid
  io.store.wresp.bits := wresp.io.out.bits
  wresp.io.out.ready := io.store.wresp.ready
}

class StreamControllerGather(
  info: StreamParInfo,
  dramStream: DRAMStream,
  app: GatherStream
) extends StreamController(info, dramStream) {

  class StreamControllerGatherIO extends StreamControllerIO {
    val gather = app.cloneType
  }

  val io = IO(new StreamControllerGatherIO)

  io <> DontCare

  val cmd = List.tabulate(info.v) { i =>
    val fifo = Module(new FIFO(new DRAMAddress, target.bufferDepth / info.v))
    fifo.io <> DontCare
    fifo.io.in.valid := io.gather.cmd.valid
    fifo.io.in.bits := DRAMAddress(io.gather.cmd.bits.addr(i))
    fifo
  }
  io.gather.cmd.ready := cmd.map { _.io.in.ready }.reduce { _ & _ }

  val gatherBuffer = Module(new GatherBuffer(info.w, info.v, target.bufferDepth))
  gatherBuffer.io <> DontCare
  gatherBuffer.io.cmdAddr <> VecInit(cmd.map { _.io.out })

  gatherBuffer.io.rresp.valid := io.dram.rresp.valid
  gatherBuffer.io.rresp.bits.burstTag := io.dram.rresp.bits.getTag.uid
  gatherBuffer.io.rresp.bits.data := io.dram.rresp.bits.rdata.asTypeOf(gatherBuffer.io.rresp.bits.data)
  io.dram.rresp.ready := gatherBuffer.io.rresp.ready

  io.gather.rdata.valid := gatherBuffer.io.rdata.valid
  io.gather.rdata.bits := gatherBuffer.io.rdata.bits
  gatherBuffer.io.rdata.ready := io.gather.rdata.ready

  io.dram.cmd.valid := gatherBuffer.io.issueAddr.valid
  io.dram.cmd.bits.addr := gatherBuffer.io.issueAddr.bits.bits
  val tag = Wire(new DRAMTag)
  tag := DontCare
  tag.uid := gatherBuffer.io.issueAddr.bits.burstTag
  io.dram.cmd.bits.setTag(tag)
  gatherBuffer.io.issueAddr.ready := io.dram.cmd.ready
  io.dram.cmd.bits.size := 1.U
  io.dram.cmd.bits.isWr := false.B
}

class StreamControllerScatter(
  info: StreamParInfo,
  dramStream: DRAMStream,
  app: ScatterStream
) extends StreamController(info, dramStream) {

  class StreamControllerScatterIO extends StreamControllerIO {
    val scatter = app.cloneType
  }

  val io = IO(new StreamControllerScatterIO)
  io <> DontCare

  val cmd = Module(new FIFOVec(new DRAMAddress, target.bufferDepth, info.v))
  cmd.io <> DontCare
  cmd.io.chainEnq := false.B
  cmd.io.chainDeq := true.B
  cmd.io.in.valid := io.scatter.cmd.valid
  cmd.io.in.bits := io.scatter.cmd.bits.addr.map { DRAMAddress(_) }
  io.scatter.cmd.ready := cmd.io.in.ready

  io.dram.cmd.valid := cmd.io.out.valid
  io.dram.cmd.bits.addr := cmd.io.out.bits(0).burstAddr
  cmd.io.out.ready := io.dram.cmd.ready
  io.dram.cmd.bits.size := 1.U
  io.dram.cmd.bits.isWr := true.B

  val wdata = Module(new FIFOVec(UInt(info.w.W), target.bufferDepth, info.v))
  wdata.io <> DontCare
  wdata.io.chainEnq := false.B
  wdata.io.chainDeq := true.B
  wdata.io.in.valid := io.scatter.wdata.valid
  wdata.io.in.bits := io.scatter.wdata.bits
  io.scatter.wdata.ready := wdata.io.in.ready

  val wstrobe = Module(new FIFO(io.dram.wdata.bits.wstrb, target.bufferDepth))
  wstrobe.io <> DontCare
  wstrobe.io.in.valid := cmd.io.in.valid & cmd.io.in.ready
  val strobeDecoder = UIntToOH(cmd.io.in.bits(0).wordOffset(info.w))
  wstrobe.io.in.bits.zipWithIndex.foreach { case (strobe, i) =>
    val offset = i / (info.w / 8)
    strobe := strobeDecoder(offset)
  }

  io.dram.wdata.valid := wdata.io.out.valid & wstrobe.io.out.valid
  io.dram.wdata.bits.wdata := VecInit(List.fill(EXTERNAL_W * EXTERNAL_V / info.w) {
    wdata.io.out.bits(0)
  }).asTypeOf(io.dram.wdata.bits.wdata)
  io.dram.wdata.bits.wstrb := wstrobe.io.out.bits
  wdata.io.out.ready := io.dram.wdata.ready
  wstrobe.io.out.ready := io.dram.wdata.ready

  val wresp = Module(new FIFO(Bool(), target.bufferDepth))
  wresp.io <> DontCare
  wresp.io.in.valid := io.dram.wresp.valid
  wresp.io.in.bits := true.B
  io.dram.wresp.ready := wresp.io.in.ready

  io.scatter.wresp.valid := wresp.io.out.valid
  io.scatter.wresp.bits := wresp.io.out.bits
  wresp.io.out.ready := io.scatter.wresp.ready
}
