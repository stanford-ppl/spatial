package spatial.lang
package control

import forge.tags._
import core._
import spatial.node._

object Accel {
  @api def apply(scope: => Void): Void = stage(AccelScope(stageBlock{ scope }))
}


