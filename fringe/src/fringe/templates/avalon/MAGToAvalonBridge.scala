package fringe.templates.avalon

import chisel3._
import chisel3.util.Cat
import fringe.globals
import fringe.DRAMStream
import fringe.templates.axi4.{AvalonBundleParameters, AvalonMaster}

class MAGToAvalonBridge(val p: AvalonBundleParameters) extends Module {
  val io = IO(new Bundle {
    val in =
      Flipped(new DRAMStream(globals.DATA_WIDTH, globals.WORDS_PER_STREAM))
    scala.Console.println(
      s"MAGToAvalonBridge.in: data_width = ${globals.DATA_WIDTH}, word_per_stream = ${globals.WORDS_PER_STREAM}")
    val M_AVALON = new AvalonMaster(p)
  })

  io <> DontCare
  // TODO: How is dram cmd ID handled?
  val numPipelinedLevels: Int = globals.magPipelineDepth
  val size: UInt = io.in.cmd.bits.size
}
