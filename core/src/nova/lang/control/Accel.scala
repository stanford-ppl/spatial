package nova.lang
package control

import forge.tags._
import nova.core._
import nova.node._

object Accel {
  @api def apply(scope: => Void): Void = stage(AccelScope(stageBlock{ scope }))
}


