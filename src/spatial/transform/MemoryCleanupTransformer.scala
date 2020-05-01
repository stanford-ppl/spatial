package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.metadata.memory._
import spatial.node._
import spatial.traversal.BlkTraversal

/** Performs hardware-specific rewrite rules. */
case class MemoryCleanupTransformer(IR: State) extends MutateTransformer with BlkTraversal {

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case _:AccelScope => inAccel{ super.transform(lhs,rhs) }
    case _:SpatialBlackboxImpl[_,_] => inBox{ super.transform(lhs,rhs) }
    case _:SpatialCtrlBlackboxImpl[_,_] => inBox{ super.transform(lhs,rhs) }

    case _: MemAlloc[_,_] if !lhs.isDRAM && !lhs.keepUnused && inHw && lhs.readers.size == 0 && lhs.writers.size == 0 =>
      Invalid.asInstanceOf[Sym[A]] // Drop

    case _ =>
      super.transform(lhs,rhs)
  }

}
