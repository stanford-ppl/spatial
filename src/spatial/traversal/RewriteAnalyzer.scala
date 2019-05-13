package spatial.traversal

import argon._
import argon.node._
import spatial.metadata.rewrites._
import spatial.metadata.memory._
import spatial.node._

/** Flags whether hardware rewrites should be allowed on given nodes.
  *
  * This is required because RewriteTransformer is a MutateTransformer, so not all metadata
  * will be up to date (e.g. consumers) during the transformation.
  */
case class RewriteAnalyzer(IR: State) extends AccelTraversal {

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case AccelScope(_) => inAccel{ super.visit(lhs,rhs) }
    case FixAdd(_, mul @ Op(FixMul(_,_))) => lhs.canFuseAsFMA = inHw && mul.consumers.size == 1 && lhs.isInnerReduceOp == mul.isInnerReduceOp
    case FixAdd(mul @ Op(FixMul(_,_)), _) => lhs.canFuseAsFMA = inHw && mul.consumers.size == 1 && lhs.isInnerReduceOp == mul.isInnerReduceOp

    case FltAdd(_, mul @ Op(FltMul(_,_))) => lhs.canFuseAsFMA = inHw && mul.consumers.size == 1 && lhs.isInnerReduceOp == mul.isInnerReduceOp
    case FltAdd(mul @ Op(FltMul(_,_)), _) => lhs.canFuseAsFMA = inHw && mul.consumers.size == 1 && lhs.isInnerReduceOp == mul.isInnerReduceOp

    case _ => super.visit(lhs,rhs)
  }

}
