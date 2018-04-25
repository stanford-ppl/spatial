package spatial

import argon.State

package object internal extends Switches with Debug {
  def spatialConfig(implicit state: State): SpatialConfig = state.config.asInstanceOf[SpatialConfig]

  type Var[A] = spatial.lang.Var[A]
  lazy val Var = spatial.lang.Var
}
