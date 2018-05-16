package spatial.lang
package control

import argon._
import forge.tags._

import spatial.node._

object Parallel {
  @api def apply(scope: => Any): Void = {
    val block = stageBlock{ scope; void }
    stage(ParallelPipe(Set.empty, block))
  }
}
