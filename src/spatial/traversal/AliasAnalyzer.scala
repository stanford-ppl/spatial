package spatial.traversal

import argon._
import spatial.data._
import spatial.lang._
import spatial.node._

case class AliasAnalyzer(IR: State) extends AccelTraversal {

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _ => super.visit(lhs,rhs)
  }

}
