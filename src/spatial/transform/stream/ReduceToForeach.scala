package spatial.transform.stream

import argon._
import spatial.dsl.struct
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.metadata.control.IndexCounterInfo
import spatial.traversal.AccelTraversal
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.TransformUtils._

case class ReduceToForeach(IR: State) extends MutateTransformer with AccelTraversal with spatial.util.TransformerUtilMixin {

  private def canReduce(reduceOp: OpReduce[_]): Boolean = {
    reduceOp.cchain.isStatic
  }

  private def transformReduce[A](sym: Sym[A], reduceOp: OpReduce[A]) = {
    dbgs(s"Transforming: $sym = $reduceOp")
    // re-staging map portion.
    // This section mirrors the ctrchain exactly.
    val newCChain = mirrorSym(reduceOp.cchain)
    val newIters = makeIters(newCChain.unbox.counters).asInstanceOf[Seq[I32]]

    implicit def bitsEV: Bits[A] = reduceOp.A
    @struct case class ReduceIterInfo(value: A, isFirst: Bit, isLast: Bit)
    val commFIFO = FIFO[ReduceIterInfo](I32(128))

    val mapStage = isolateSubst() {
      (reduceOp.iters zip newIters) foreach {
        case (oldIter, newIter) => register(oldIter -> newIter)
      }
      stage(OpForeach(f(reduceOp.ens), newCChain.unbox, stageBlock {
        reduceOp.map.stms.foreach(visit)
        val result = f(reduceOp.map.result)
        val isFirst = isFirstIter(newIters, newCChain.unbox)
        val isLast = isLastIter(newIters, newCChain.unbox)
        commFIFO.enq(ReduceIterInfo(result.unbox, isFirst.reduceTree {_ && _}, isLast.reduceTree {_ && _}))
      }, newIters, f(reduceOp.stopWhen)))
    }


    val flattenedCChain = spatial.util.TransformUtils.expandCounterPars(reduceOp.cchain)
    val newReduceIters = makeIters(flattenedCChain.unbox.counters).asInstanceOf[Seq[I32]]

    // TODO: Should change this to be dynamic based on par factor, but that requires messing with accumulators
    val reduceSize = reduceOp.cchain.approxIters
    dbgs(s"CChain: ${reduceOp.cchain}")
    dbgs(s"Reduce Size: $reduceSize")
    val newAccum = f(reduceOp.accum)
    dbgs(s"Mirrored accum: ${newAccum}")
    newAccum.explicitName = newAccum.explicitName.getOrElse("") + "ToForeach"

//    val numAccums = I32(16)
//    val accumulators = RegFile(numAccums)
//    val reduceStage = isolateSubst() {
//      stage(OpForeach(f(reduceOp.ens), flattenedCChain, stageBlock {
//        // Take the entire width of elements at the same time, and reduce
//        val elements = commFIFO.deqVec(reduceSize)
//        val values = elements.elems.map {_.value}
//        val result = values.reduceTree { case (a, b) => reduceOp.reduce.reapply(a, b) }
//        f(reduceOp.accum) := result
//      }, newReduceIters, None))
//    }
    val reduceStage = isolateSubst() {
      stage(UnitPipe(f(reduceOp.ens), stageBlock {
        // Take the entire width of elements at the same time, and reduce
        val elements = commFIFO.deqVec(reduceSize)
        val values = elements.elems.map {_.value}
        val result = values.reduceTree { case (a, b) => reduceOp.reduce.reapply(a, b) }
        f(reduceOp.accum) := result
      }, None))
    }
    f(reduceOp.accum)
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (
    rhs match {
      case reduce: OpReduce[_] if canReduce(reduce) => transformReduce(lhs, rhs.asInstanceOf[OpReduce[A]])
      case _ => super.transform(lhs, rhs)
    }
  ).asInstanceOf[Sym[A]]
}
