package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.traversal.AccelTraversal
import spatial.metadata.control._
import spatial.node._

import spatial.util.TransformUtils._

case class UnitIterationElimination(IR: State) extends MutateTransformer with AccelTraversal {
  private def transformable(ctr: Counter[_]): Boolean = ctr.isUnit && ctr.ctrParOr1 == 1
  private def transformForeach(lhs: Sym[_], foreachOp: OpForeach): Sym[_] = {
    if (foreachOp.cchain.counters.forall(transformable)) {
      // Transform into a unitpipe. Simply remap all of the iterators to their start, and then revisit all nodes.
      isolateSubst() {
        foreachOp.cchain.counters.foreach {
          ctr =>
            register(ctr.iter.get, ctr.start)
        }
        stageWithFlow(UnitPipe(f(foreachOp.ens), visitBlock(foreachOp.block), f(foreachOp.stopWhen))) {
          lhs2 => transferData(lhs, lhs2)
            lhs2.ctx = implicitly[SrcCtx].copy(previous = Seq(lhs.ctx))
        }
      }
    } else {
      // Transform into another foreach
      isolateSubst() {
        foreachOp.cchain.counters.filter(transformable) foreach {
          ctr =>
            register(ctr.iter.get, ctr.start)
        }
        val remaining = foreachOp.cchain.counters.filterNot(transformable)
        dbgs(s"Remaining: $remaining")
        val mirroredCtrs = remaining.map(mirrorSym(_).unbox)
        register(remaining, mirroredCtrs)
        val newIters = makeIters(mirroredCtrs).asInstanceOf[Seq[I32]]
        register(remaining.map(_.iter.get), newIters)
        dbgs(s"Substs: $subst")
        val newChain = CounterChain(mirroredCtrs)
        stageWithFlow(OpForeach(f(foreachOp.ens), newChain, visitBlock(foreachOp.block), newIters, f(foreachOp.stopWhen))) {
          lhs2 => transferData(lhs, lhs2)
            lhs2.ctx = implicitly[SrcCtx].copy(previous = Seq(lhs.ctx))
        }
      }
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case foreachOp: OpForeach if foreachOp.cchain.counters.exists(transformable) =>
      dbgs(s"Transforming: $lhs = $foreachOp")
      indent {
        transformForeach(lhs, foreachOp)
      }
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}
