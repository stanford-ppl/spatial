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

  val cmd = Module(new FIFO(app.cmd.bits, target.bufferDepth))
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

  val cmd = Module(new FIFO(app.cmd.bits, target.bufferDepth))
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
  wdata.io.in.valid := io.store.wdata.valid
  wdata.io.in.bits.data := io.store.wdata.bits
  wdata.io.in.bits.strobe := io.store.wstrb.bits
  io.store.wdata.ready := wdata.io.in.ready
  io.store.wstrb.ready := wdata.io.in.ready

  io.dram.wdata.valid := wdata.io.out.valid
  io.dram.wdata.bits.wdata := wdata.io.out.bits.data
  io.dram.wdata.bits.wstrb := Vec(wdata.io.out.bits.strobe.toBools).reverse
  wdata.io.out.ready := io.dram.wdata.ready

  val wresp = Module(new FIFO(Bool(), target.bufferDepth))
  wresp.io.in.valid := io.dram.wresp.valid
  wresp.io.in.bits := io.dram.wresp.valid
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
}
