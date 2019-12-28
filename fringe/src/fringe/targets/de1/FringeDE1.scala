package fringe.targets.de1

import fringe.targets.zynq.FringeZynq
import fringe.templates.axi4._

class FringeDE1(blockingDRAMIssue: Boolean,
                axiLiteParams: AXI4BundleParameters,
                axiParams: AXI4BundleParameters)
    extends FringeZynq(blockingDRAMIssue, axiLiteParams, axiParams) {
  // TODO: Later we need to override some of FringeZynq's defs.
}
