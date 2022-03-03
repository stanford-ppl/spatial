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
import spatial.util.TransformUtils
import spatial.util.TransformUtils._


@argon.tags.struct case class ReduceIterInfo[A: Bits](value: A, isFirst: Bit, isLast: Bit)
case class ReduceToForeach(IR: State) extends MutateTransformer with AccelTraversal with spatial.util.TransformerUtilMixin {

  private def canReduce(reduceOp: OpReduce[_]): Boolean = {
    reduceOp.cchain.isStatic
  }

  private def transformReduce[A: Bits](sym: Sym[A], reduceOp: OpReduce[A]) = {
    dbgs(s"Transforming: $sym = $reduceOp")
    // re-staging map portion.
    // This section mirrors the ctrchain exactly.
    val newCChain = mirrorSym(reduceOp.cchain)
    val newIters = makeIters(newCChain.unbox.counters).asInstanceOf[Seq[I32]]

    val commFIFO = FIFO[ReduceIterInfo[A]](I32(128))
    commFIFO.explicitName = s"ReduceToForeach_FIFO_$sym"

    dbgs(s"Staging Map Phase")
//    val mapStage = isolateSubst() {
//      (reduceOp.iters zip newIters) foreach {
//        case (oldIter, newIter) => register(oldIter -> newIter)
//      }
//      stageWithFlow(OpForeach(f(reduceOp.ens), newCChain.unbox, stageBlock {
//        indent {
//          reduceOp.map.stms.foreach(visit)
//        }
//        val result = f(reduceOp.map.result)
//        val isFirst = isFirstIter(newIters, newCChain.unbox)
//        val isLast = isLastIter(newIters, newCChain.unbox)
//        commFIFO.enq(ReduceIterInfo(result.unbox, isFirst.reduceTree {_ && _}, isLast.reduceTree {_ && _}))
//      }, newIters, f(reduceOp.stopWhen))) { pipe =>
//        pipe.explicitName = s"ReduceToForeach_Map_$sym"
//        pipe.userSchedule = Pipelined
//      }
//    }

    isolateSubst() {
      // Fully unrolled
      val iterSpaces = reduceOp.cchain.counters map { TransformUtils.counterToSeries }
      dbgs(s"Iteration Spaces: $iterSpaces")
      val allIters = spatial.util.crossJoin(iterSpaces)

      stageWithFlow(UnitPipe(f(reduceOp.ens), stageBlock {
        val savedSubsts = saveSubsts()
        val substitutions = allIters map {
          iters =>
            restoreSubsts(savedSubsts)
            (reduceOp.iters zip iters) foreach {
              case (oldIter, newIter) => register(oldIter -> I32(newIter))
            }
            saveSubsts()
        }
        val resultantSubsts = visitWithSubsts(substitutions.toSeq, reduceOp.map.stms)
        dbgs(s"Substitutions: $resultantSubsts")
        val subResults = resultantSubsts map {
          subs =>
            restoreSubsts(subs)
            f(reduceOp.map.result)
        }
        val values = Vec.fromSeq(subResults map {
          result => ReduceIterInfo(result.unbox, Bit(false), Bit(false))
        })
        commFIFO.enqVec(values)
      }, None)) {
        lhs2 => lhs2.explicitName = s"ReduceToForeach_Map_$sym"
      }
    }

    // TODO: Should change this to be dynamic based on par factor, but that requires messing with accumulators
    val reduceSize = reduceOp.cchain.approxIters
    dbgs(s"CChain: ${reduceOp.cchain} = ${reduceOp.cchain.counters.map(_.op).mkString(", ")}")
    dbgs(s"Reduce Size: $reduceSize")
    val newAccum = f(reduceOp.accum)
    dbgs(s"Mirrored accum: ${newAccum}")
    newAccum.explicitName = newAccum.explicitName.getOrElse("") + s"ToForeach_${sym}"

    dbgs(s"Staging Reduce Phase")
    val reduceStage = isolateSubst() {
      stageWithFlow(UnitPipe(f(reduceOp.ens), stageBlock {
        // Take the entire width of elements at the same time, and reduce
        val elements = commFIFO.deqVec(reduceSize)
        val values = elements.elems.map {_.value}
        val result = values.reduceTree { case (a, b) => reduceOp.reduce.reapply(a, b) }
        newAccum := result
      }, None)) {
        lhs2 =>
          lhs2.explicitName = s"ReduceToForeach_Red_$sym"
      }
    }
    newAccum
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (
    rhs match {
      case reduce: OpReduce[_] if canReduce(reduce) =>
        implicit def bitsEV: Bits[A] = reduce.A.asInstanceOf[Bits[A]]
        transformReduce(lhs, rhs.asInstanceOf[OpReduce[A]])
      case _ =>
        super.transform(lhs, rhs)
    }
  ).asInstanceOf[Sym[A]]
}
