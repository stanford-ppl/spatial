package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node._

protected class AccelClass(name: Option[String]) {
  lazy val options = CtrlOpt(name, None, None, mop = false, pom = false)

  @api def apply(wild: Wildcard)(scope: => Any): Void = {
    Accel {
      Stream(*){ scope }
    }
  }

  @api def apply(scope: => Any): Void = {
    stageWithFlow(AccelScope(stageBlock{ scope; void })){pipe => options.set(pipe) }
  }
}
