package pcc.lang
package control

import forge._
import pcc.core._
import pcc.node._

object Accel {
  @api def apply(scope: => Void): Void = stage(AccelScope(stageBlock{ scope }))
}


