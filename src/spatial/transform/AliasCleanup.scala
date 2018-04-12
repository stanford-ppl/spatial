package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.node._

case class AliasCleanup(IR: State) extends MutateTransformer {
  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case _:MemDenseAlias[_,_,_]    => Invalid.asInstanceOf[Sym[A]]
    case _:MemSparseAlias[_,_,_,_] => Invalid.asInstanceOf[Sym[A]]
    case _ => super.transform(lhs, rhs)
  }
}
