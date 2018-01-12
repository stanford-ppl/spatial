package pcc.traversal.transform

import pcc.core._
import pcc.data._
import pcc.lang._
import pcc.node._

case class PipeInserter(IR: State) extends ForwardTransformer {
  override val name = "Pipe Inserter"

  /*override def transform[A: Sym](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {

  }*/
}
