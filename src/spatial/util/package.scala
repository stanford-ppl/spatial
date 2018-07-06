package spatial

import argon._
import forge.tags.stateful
import spatial.metadata.control._

package object util {

  def spatialConfig(implicit state: State): SpatialConfig = state.config.asInstanceOf[SpatialConfig]

  /** True if the given symbols can be safely moved out of a conditional scope. */
  @stateful def canMotionFromConditional(stms: Seq[Sym[_]]): Boolean = {
    stms.forall{s => !s.takesEnables && s.effects.isIdempotent }
  }

  /** True if these symbols can be motioned and have little cost to be moved. */
  @stateful def shouldMotionFromConditional(stms: Seq[Sym[_]], inHw: Boolean): Boolean = {
    canMotionFromConditional(stms) && (inHw || stms.length == 1)
  }

}
