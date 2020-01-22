package fringe.targets.de1

import chisel3._
import fringe.globals._
import fringe.SpatialIPInterface
import fringe.templates.axi4.{AXI4BundleParameters, AXI4Inlined, AXI4Probe, AvalonBundleParameters, AvalonSlave}

class DE1Interface extends SpatialIPInterface {
  val avalonLiteParams = new AvalonBundleParameters(ADDR_WIDTH, DATA_WIDTH, 1)
//  val avalonBurstParams = new AvalonBundleParameters(ADDR_WIDTH, 512, 32)
  val axiParams = new AXI4BundleParameters(ADDR_WIDTH, 512, 32)

  val S_AVALON = new AvalonSlave(avalonLiteParams)
  // TODO: M_AVALON may work better.
  val M_AXI = Vec(NUM_CHANNELS, new AXI4Inlined(axiParams))

  // TODO: Avalon debugging probes.
  //  For now just use AXI interfaces to stop firrtl from panicing
  val TOP_AXI = new AXI4Probe(avalonLiteParams)
  val DWIDTH_AXI = new AXI4Probe(avalonLiteParams)
  val PROTOCOL_AXI = new AXI4Probe(avalonLiteParams)
  val CLOCKCONVERT_AXI = new AXI4Probe(avalonLiteParams)
}
