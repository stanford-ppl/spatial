package spatial

import core.Config
import spatial.targets.HardwareTarget

class SpatialConfig extends Config {
  var target: HardwareTarget = _

  var enablePIR: Boolean = false
}
