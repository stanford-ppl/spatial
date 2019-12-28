package fringe.targets.de1

import chisel3._
import fringe.globals._
import fringe.SpatialIPInterface
import fringe.templates.axi4.{AXI4Inlined, AvalonBundleParameters, AvalonSlave}

class DE1Interface extends SpatialIPInterface {
  val avalonLiteParams = new AvalonBundleParameters(ADDR_WIDTH, DATA_WIDTH, 1)
  val avalonBurstParams = new AvalonBundleParameters(ADDR_WIDTH, 512, 32)
  val S_AVALON = new AvalonSlave(avalonLiteParams)

  // We rely on Intel's protocol converter to do the weight-lifting part.
  val M_AXI: Vec[AXI4Inlined] =
    Vec(NUM_CHANNELS, new AXI4Inlined(avalonBurstParams))

  // TODO: avalon debugging probes
}
