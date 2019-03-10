package fringe.targets.arria10

import chisel3._
import fringe.globals._
import fringe.TopInterface
import fringe.templates.axi4._

class Arria10Interface extends TopInterface {
  // To fit the sysid interface, we only want to have 7 bits for 0x0000 ~ 0x01ff
  val axiLiteParams = new AXI4BundleParameters(7, DATA_WIDTH, 1)
  // TODO: This group of params is for memory
  val axiParams = new AXI4BundleParameters(DATA_WIDTH, 512, 6)
  val S_AVALON = new AvalonSlave(axiLiteParams) // scalars
//  val M_AXI = Vec(NUM_CHANNELS, new AXI4Inlined(axiParams))
  val M_AVALON = Vec(NUM_CHANNELS, new AvalonMaster(axiParams))

  // AXI debugging loopbacks
  val TOP_AXI = new AXI4Probe(axiLiteParams)
  val DWIDTH_AXI = new AXI4Probe(axiLiteParams)
  val PROTOCOL_AXI = new AXI4Probe(axiLiteParams)
  val CLOCKCONVERT_AXI = new AXI4Probe(axiLiteParams)
}
