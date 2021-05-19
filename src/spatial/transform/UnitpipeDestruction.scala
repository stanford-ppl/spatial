package spatial.transform

import argon.node.Enabled
import argon._
import argon.transform.MutateTransformer
import spatial.dsl.retimeGate
import spatial.node.UnitPipe
import spatial.traversal.AccelTraversal

case class UnitpipeDestruction(IR: argon.State) extends MutateTransformer with AccelTraversal {
  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit srcCtx: argon.SrcCtx): Sym[A] = {
    rhs match {
      case UnitPipe(ens, block, stopWhen) if stopWhen.isEmpty =>
        block.stms foreach {
          stmt =>
            val newSym = mirrorSym(stmt)
            newSym.op match {
              case Some(op: Enabled[_]) =>
                op.updateEn(f, ens)
              case _ =>
            }
            register(stmt -> newSym)
        }

        argon.lang.Void.c.asSym.asInstanceOf[Sym[A]]
      case _ => super.transform(lhs, rhs)
    }
  }
}
