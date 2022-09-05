package spatial.transform.streamify

import argon._
import argon.passes.Traversal
import spatial.lang.SRAM

case class ForceHierarchical(IR: State) extends Traversal {
  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = lhs match {
    case sr: SRAM[_, _] =>
      dbgs(s"Marking $sr as hierarchical")
      sr.hierarchical
    case _ => super.visit(lhs, rhs)
  }
}
