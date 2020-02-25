package fringe.targets.de1

import chisel3._
import fringe.globals._
import fringe.SpatialIPInterface
import fringe.templates.axi4.{
  AXI4Probe,
  AvalonBundleParameters,
  AvalonMaster,
  AvalonSlave
}

class DE1Interface extends SpatialIPInterface {
  val avalonLiteParams = new AvalonBundleParameters(ADDR_WIDTH, DATA_WIDTH, 1)
//  val avalonBurstParams = new AvalonBundleParameters(ADDR_WIDTH, 512, 32)
  // TODO: 26 and 17 shouldn't be hard-coded. This is the addr bits for m_axi on de1.
  val avalonParams = new AvalonBundleParameters(26, 512, 17, 11, 64)

  val S_AVALON = new AvalonSlave(avalonLiteParams)
  // TODO: M_AVALON may work better.
  val M_AVALON: Vec[AvalonMaster] = Vec(NUM_CHANNELS, new AvalonMaster(avalonParams))

  // TODO: Avalon debugging probes.
  //  For now just use AXI interfaces to stop FIRRTL from panicing
  val TOP_AXI = new AXI4Probe(avalonLiteParams)
  val DWIDTH_AXI = new AXI4Probe(avalonLiteParams)
  val PROTOCOL_AXI = new AXI4Probe(avalonLiteParams)
  val CLOCKCONVERT_AXI = new AXI4Probe(avalonLiteParams)
}
