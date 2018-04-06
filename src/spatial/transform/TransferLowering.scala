package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.node.{DenseTransfer, SparseTransfer}

case class TransferLowering(IR: State) extends MutateTransformer {

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case op: DenseTransfer[_,_,_] => op.lower()
    case op: SparseTransfer[_,_]  => op.lower()
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]

}
