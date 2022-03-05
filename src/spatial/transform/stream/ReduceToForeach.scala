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
import spatial.util.{TransformUtils, computeShifts}
import spatial.util.TransformUtils._


//@argon.tags.struct case class ReduceIterInfo[A: Bits](value: A, isFirst: Bit, isLast: Bit)
@argon.tags.struct case class ReduceIterInfo[A: Bits](value: A)
case class ReduceToForeach(IR: State) extends MutateTransformer with AccelTraversal with spatial.util.TransformerUtilMixin {

  private def canReduce(reduceOp: OpReduce[_]): Boolean = {
    reduceOp.cchain.isStatic
  }

  private def transformReduce[A: Bits](sym: Sym[A], reduceOp: OpReduce[A]) = {
    dbgs(s"Transforming: $sym = $reduceOp")
    // re-staging map portion.
    // This section mirrors the ctrchain exactly.
//    val newCChain = mirrorSym(reduceOp.cchain)
//    val newIters = makeIters(newCChain.unbox.counters).asInstanceOf[Seq[I32]]

    val commFIFO = FIFO[ReduceIterInfo[A]](I32(128))
    commFIFO.explicitName = s"ReduceToForeach_FIFO_$sym"

    dbgs(s"Staging Map Phase")

    isolateSubst() {
      val newCChain = expandCounterPars(reduceOp.cchain)
      val newIters = makeIters(newCChain.counters)
      val shifts = computeShifts(reduceOp.cchain.counters map {_.ctrParOr1})
      val substitutions = shifts.map(shift => createSubstData {
        (reduceOp.iters zip newIters zip shift zip reduceOp.cchain.counters) foreach {
          case (((oldIter, newIter), parShift), ctr) =>
            val castedShift = ctr.CTeV.from(parShift)
            val replacement = () => {
              val offset =  ctr.step.asInstanceOf[Num[ctr.CT]] * castedShift.asInstanceOf[ctr.CT]
              newIter.asInstanceOf[Num[ctr.CT]] + offset.asInstanceOf[ctr.CT]
            }
            register(oldIter, replacement)
        }
      })
      stageWithFlow(OpForeach(f(reduceOp.ens), newCChain.unbox, stageBlock {
        val updatedSubsts = indent { visitWithSubsts(substitutions, reduceOp.map.stms) }
        val mappedValues = mapSubsts(reduceOp.map.result, updatedSubsts)(f.apply)
        val signalValues = mappedValues map {
          value => ReduceIterInfo(value.unbox)
        }
        commFIFO.enqVec(Vec.fromSeq(signalValues))
      }, newIters.asInstanceOf[Seq[I32]], f(reduceOp.stopWhen))) {
        pipe =>
          transferData(sym, pipe)
          pipe.ctx = withPreviousCtx(sym.ctx)
      }
    }

//    isolateSubst() {
//      // Fully unrolled
//      val iterSpaces = reduceOp.cchain.counters map { TransformUtils.counterToSeries }
//      dbgs(s"Iteration Spaces: $iterSpaces")
//      val allIters = spatial.util.crossJoin(iterSpaces)
//
//      stageWithFlow(UnitPipe(f(reduceOp.ens), stageBlock {
//        val savedSubsts = saveSubsts()
//        val substitutions = allIters map {
//          iters =>
//            restoreSubsts(savedSubsts)
//            (reduceOp.iters zip iters) foreach {
//              case (oldIter, newIter) => register(oldIter -> I32(newIter))
//            }
//            saveSubsts()
//        }
//        val resultantSubsts = visitWithSubsts(substitutions.toSeq, reduceOp.map.stms)
//        dbgs(s"Substitutions: $resultantSubsts")
//        val subResults = resultantSubsts map {
//          subs =>
//            restoreSubsts(subs)
//            f(reduceOp.map.result)
//        }
//        val values = Vec.fromSeq(subResults map {
//          result => ReduceIterInfo(result.unbox, Bit(false), Bit(false))
//        })
//        commFIFO.enqVec(values)
//      }, None)) {
//        lhs2 => lhs2.explicitName = s"ReduceToForeach_Map_$sym"
//      }
//    }

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
