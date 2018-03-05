package spatial

import core.State

package object internal extends Switches with Debug {
  def spatialConfig(implicit state: State): SpatialConfig = state.config.asInstanceOf[SpatialConfig]
}
