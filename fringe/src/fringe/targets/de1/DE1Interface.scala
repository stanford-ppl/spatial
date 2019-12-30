package fringe.targets.de1

import chisel3._
import fringe.globals._
import fringe.SpatialIPInterface
import fringe.templates.axi4.{AXI4Inlined, AXI4Probe, AvalonBundleParameters, AvalonSlave}

class DE1Interface extends SpatialIPInterface {
  val avalonLiteParams = new AvalonBundleParameters(ADDR_WIDTH, DATA_WIDTH, 1)
  val avalonBurstParams = new AvalonBundleParameters(ADDR_WIDTH, 512, 32)

  val S_AVALON = new AvalonSlave(avalonLiteParams)
  // TODO: Get the memStream working.
  val M_AXI: Vec[AXI4Inlined] =
    Vec(NUM_CHANNELS, new AXI4Inlined(avalonBurstParams))

  // TODO: Avalon debugging probes.
  //  For now just use AXI interfaces to stop firrtl from panicing

  val TOP_AXI = new AXI4Probe(avalonLiteParams)
  val DWIDTH_AXI = new AXI4Probe(avalonLiteParams)
  val PROTOCOL_AXI = new AXI4Probe(avalonLiteParams)
  val CLOCKCONVERT_AXI = new AXI4Probe(avalonLiteParams)
}
