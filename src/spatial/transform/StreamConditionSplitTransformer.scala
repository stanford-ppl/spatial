package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.traversal.AccelTraversal

case class StreamConditionSplitTransformer(IR: State)  extends MutateTransformer with AccelTraversal {
  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {

    super.transform(lhs, rhs)
  }
}
