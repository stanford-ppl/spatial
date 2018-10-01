package fringe.targets.aws

import chisel3._
import fringe.globals._
import fringe.TopInterface
import fringe.templates.axi4.{AXI4BundleParameters,AXI4Inlined}


class AWSInterface extends TopInterface {
  val axiLiteParams = new AXI4BundleParameters(ADDR_WIDTH, DATA_WIDTH, 1)
  val axiParams = new AXI4BundleParameters(ADDR_WIDTH, 512, 16)

  val enable = Input(UInt(DATA_WIDTH.W))
  val done = Output(UInt(DATA_WIDTH.W))
  val scalarIns = Input(Vec(NUM_ARG_INS, UInt(64.W)))
  val scalarOuts = Output(Vec(NUM_ARG_OUTS, UInt(64.W)))

  //  val dbg = new DebugSignals

  // DRAM interface - currently only one stream
  //  val dram = new DRAMStream(p.dataWidth, p.v)
  val M_AXI = Vec(NUM_CHANNELS, new AXI4Inlined(axiParams))
}
