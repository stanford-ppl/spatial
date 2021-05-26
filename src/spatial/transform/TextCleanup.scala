package spatial.transform

import argon.node.DSLOp
import argon.{Invalid, Op, SrcCtx, State, Sym, Type, dbgs}
import argon.transform.MutateTransformer
import spatial.node.AccelScope
import spatial.traversal.AccelTraversal

case class TextCleanup(IR: State) extends MutateTransformer with AccelTraversal {
  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    rhs match {
      case AccelScope(_) => inAccel{ super.transform(lhs, rhs) }
      case dslOp: DSLOp[_] if inAccel && !dslOp.canAccel =>
        // delete
        dbgs(s"Deleting: $lhs = $rhs")
        Invalid.asInstanceOf[Sym[A]]
      case _ => super.transform(lhs, rhs)
    }
  }
}
