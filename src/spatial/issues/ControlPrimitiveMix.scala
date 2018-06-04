package spatial.issues

import argon._
import forge.tags._

case class ControlPrimitiveMix(parent: Sym[_], ctrl: Seq[Sym[_]], prim: Seq[Sym[_]]) extends Issue {
  @stateful override def onUnresolved(traversal: String): Unit = {
    bug(parent.ctx, s"Control ${stm(parent)} has illegal mix of primitives and control:")
    bug("Control: ")
    ctrl.foreach{c => bug(s"  ${stm(c)}")}
    bug("Primitives: ")
    prim.foreach{c => bug(s"  ${stm(c)}")}
    bug(parent.ctx)
  }
}
