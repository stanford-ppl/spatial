package spatial.transform.stream

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.metadata.control.IndexCounterInfo
import spatial.traversal.AccelTraversal
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.TransformUtils.makeIters

case class ReduceToForeach(IR: State) extends MutateTransformer with AccelTraversal {

  private def transformReduce[A](sym: Sym[A], reduce: OpReduce[A]) = {
    // re-staging map portion.
    // This section mirrors the ctrchain exactly.
    val newCChain = mirrorSym(reduce.cchain)
    val newIters = makeIters(newCChain.unbox.counters)

    implicit def bitsEV: Bits[A] = reduce.A
    val commFIFO = FIFO[A](I32(128))

    val mapOp = isolateSubst() {
      (reduce.iters zip newIters) foreach {
        case (oldIter, newIter) => register(oldIter -> newIter)
      }
      stage(OpForeach(f(reduce.ens), newCChain.unbox, stageBlock {
        visitBlock(reduce.map)
        val result = f(reduce.map.result)
      }, newIters.asInstanceOf[Seq[I32]], f(reduce.stopWhen)))
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (
    rhs match {
      case reduce: OpReduce[_] => transformReduce(lhs, rhs.asInstanceOf[OpReduce[A]])
      case _ => super.transform(lhs, rhs)
    }
  ).asInstanceOf[Sym[A]]
}
