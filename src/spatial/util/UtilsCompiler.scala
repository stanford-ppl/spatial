package spatial.util

import argon._
import spatial.SpatialConfig

trait UtilsCompiler {

  def spatialConfig(implicit state: State): SpatialConfig = state.config.asInstanceOf[SpatialConfig]

}
