package spatial.lang
package control

import forge.tags._
import argon._
import spatial.node._

protected class AccelClass(name: Option[String]) {
  lazy val options = CtrlOpt(name, None, None)

  @api def apply(scope: => Void): Void = {
    val pipe = stage(AccelScope(stageBlock{ scope }))
    options.set(pipe)
    pipe
  }
}
