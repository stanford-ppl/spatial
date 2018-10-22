package fringe.templates.dramarbiter

import chisel3._
import chisel3.util._

import fringe._
import fringe.utils._
import fringe.globals._

class StreamArbiter(dramStream: DRAMStream, streamCount: Int) extends Module {
  val io = IO(new Bundle {
    val app = Vec(streamCount, Flipped(dramStream.cloneType))
    val dram = dramStream.cloneType
  })

  val cmdValids = io.app.map { _.cmd.valid }
  val cmdIdx = PriorityEncoder(cmdValids)
  val cmdInDecoder = UIntToOH(cmdIdx)

  // some apps have many streams, so we need to pipeline these muxes to meet timing
  val cmdMux = Module(new MuxPipe(dramStream.cmd.bits, streamCount))
  cmdMux.io.in.valid := cmdValids.reduce { _|_ }
  cmdMux.io.sel := cmdIdx
  cmdMux.io.in.bits.zipWithIndex.map { case (cmd, i) =>
    cmd := io.app(i).cmd.bits
    val tag = io.app(i).cmd.bits.getTag
    tag.streamID := i.U
    cmd.setTag(tag)
  }

  val cmdStreamID = cmdMux.io.out.bits.getTag.streamID
  val cmdOutDecoder = UIntToOH(cmdStreamID)
  val wdataMux = Module(new MuxPipe(dramStream.wdata.bits, streamCount))
  wdataMux.io.in.valid := Vec(io.app.map { _.wdata.valid })(cmdStreamID) & cmdMux.io.out.valid
  wdataMux.io.sel := cmdStreamID
  wdataMux.io.in.bits := io.app.map { _.wdata.bits }

  io.dram.cmd <> cmdMux.io.out
  io.dram.wdata <> wdataMux.io.out

  val rrespStream = io.dram.rresp.bits.getTag.streamID
  val wrespStream = io.dram.wresp.bits.getTag.streamID
  val rrespDecoder = UIntToOH(rrespStream)
  val wrespDecoder = UIntToOH(wrespStream)

  val cmdIssue = io.dram.cmd.valid & io.dram.cmd.ready
  val wdataIssue = io.dram.wdata.valid & io.dram.wdata.ready

  cmdMux.io.out.ready := cmdIssue
  wdataMux.io.out.ready := wdataIssue

  io.app.zipWithIndex.foreach { case (app, i) =>
    app.cmd.ready := cmdMux.io.in.ready & cmdInDecoder(i)
    app.wdata.ready := (wdataMux.io.in.ready & wdataMux.io.in.valid) & cmdOutDecoder(i)

    app.rresp.valid := io.dram.rresp.valid & rrespDecoder(i)
    app.rresp.bits := io.dram.rresp.bits

    app.wresp.valid := io.dram.wresp.valid & wrespDecoder(i)
    app.wresp.bits := io.dram.wresp.bits
  }

  // if we can't meet timing, pipeline these muxes
  io.dram.rresp.ready := Vec(io.app.map { _.rresp.ready })(rrespStream)
  io.dram.wresp.ready := Vec(io.app.map { _.wresp.ready })(wrespStream)
}
