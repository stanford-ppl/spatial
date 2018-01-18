package pcc.traversal.transform

import pcc.core._
import pcc.node._
import pcc.lang._

case class GlobalAllocation(IR: State) extends MutateTransformer {
  override val name = "Global Allocation"

  /*override def transform[A:Sym](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope()

  }*/
}
