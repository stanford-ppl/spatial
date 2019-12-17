package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node._

protected class SpatialModuleClass(name: Option[String]) {
  lazy val options = CtrlOpt(name, None, None, mop = false, pom = false)

//  @api def apply(scope: => Any): Void = {
//    stageWithFlow(SpatialModuleScope(stageBlock{ scope; void })){pipe => options.set(pipe) }
//  }

