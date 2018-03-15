package spatial

import core.Config
import spatial.targets.HardwareTarget

class SpatialConfig extends Config {
  var target: HardwareTarget = _

  var addRetimeRegisters = true // Enable adding registers after specified comb. logic

  var enableAsyncMem = true
  var enableRetiming = true

  def ignoreParEdgeCases: Boolean = false
  def noInnerLoopUnroll: Boolean = false
  def enableBufferCoalescing: Boolean = true
}
