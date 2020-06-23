package fringe.targets.zynq

import fringe.globals._
import fringe.templates.axi4.AXI4BundleParameters

class ZCUInterface extends ZynqInterface {
  override val axiParams = new AXI4BundleParameters(ADDR_WIDTH, 128, 32)

}
