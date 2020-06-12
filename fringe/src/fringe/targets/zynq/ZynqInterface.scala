package fringe.targets.zynq

import chisel3._
import chisel3.util.Decoupled
import fringe.globals._
import fringe._
import fringe.SpatialIPInterface
import fringe.templates.axi4.{AXI4BundleParameters, AXI4Inlined, AXI4Lite, AXI4Probe}
import fringe.templates.axi4._

class ZynqInterface extends SpatialIPInterface {
  val axiLiteParams = new AXI4BundleParameters(ADDR_WIDTH, DATA_WIDTH, 1)
  val axiParams = new AXI4BundleParameters(ADDR_WIDTH, DATA_WIDTH * WORDS_PER_STREAM, 32)

  val S_AXI = Flipped(new AXI4Lite(axiLiteParams))
  val M_AXI = Vec(NUM_CHANNELS, new AXI4Inlined(axiParams))

  // AXI debugging loopbacks
  val TOP_AXI = new AXI4Probe(axiLiteParams)
  val DWIDTH_AXI = new AXI4Probe(axiLiteParams)
  val PROTOCOL_AXI = new AXI4Probe(axiLiteParams)
  val CLOCKCONVERT_AXI = new AXI4Probe(axiLiteParams)

}
