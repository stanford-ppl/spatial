package spatial.transform.stream

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._

trait CounterChainToStream { this: MutateTransformer =>
  case class StreamCounter(index: FIFO[Vec[I32]], isFirst: FIFO[Bit], isLast: FIFO[Bit])

  def makeIters(ctrs: Seq[Counter[_]]) = {
    ctrs map { ctr =>
      val n = boundVar[I32]
      n.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1) { i => i })
      n
    }
  }

  def mirrorCounterChain(cchain: CounterChain, parentCChain: CounterChain, parentShift: Seq[Int], parentEns: Set[Bit], parentStopWhen: Option[Reg[Bit]]): StreamCounter = {
    // cchains are converted into FIFO generating streams
    // these are generated within the parent context. The PShift is the shifting context, and no parent parallelization
    // is needed.

    val kFifoDepth = 32
    // Sends along the index, with as a vector of <parent index ... child index>.
    val indexFIFO = {
      implicit def ev: Bits[Vec[I32]] = spatial.util.TransformUtils.CreateVecEV[I32](cchain.nDim + parentCChain.nDim)
      FIFO[Vec[I32]](kFifoDepth)
    }
    // Indicates if this is the first/last iteration of the child (in that parent iter), in order to avoid the DPP
    val isFirstFIFO = FIFO[Bit](kFifoDepth)
    val isLastFIFO = FIFO[Bit](kFifoDepth)

    // Create counterchain for feeder. The parent Chain should be shifted by parentShift
    val newParentCtrs = (parentCChain.counters zip parentShift) map {
      case (ctr, pshift) =>
        ctr match {
          case Op(CounterNew(start, stop, step, par)) =>
            // we're handling the parent par at a high level.
            stage(CounterNew(
              f(start).asInstanceOf[I32] + pshift,
              f(stop).asInstanceOf[I32],
              f(step).asInstanceOf[I32] * par,
              I32(1)
            ))

          case Op(ForeverNew()) =>
            // Don't need to shift, since ForeverNews are parallelized by 1.
            stage(ForeverNew())
        }
    }

    // child cchains shifts / par is handled within the child expansions, so we can drop the par factor and keep everything else.
    val newChildCtrs = cchain.counters map {
      case Op(CounterNew(start, stop, step, par)) =>
        stage(CounterNew(
          f(start).asInstanceOf[I32],
          f(stop).asInstanceOf[I32],
          f(step).asInstanceOf[I32] * par,
          I32(1)
        ))
    }

    val newCtrChain = CounterChain(newParentCtrs ++ newChildCtrs)
    val parentIters = makeIters(newParentCtrs)
    val childIters = makeIters(newChildCtrs)

    stage(OpForeach(f(parentEns), newCtrChain, stageBlock {
      val isFirstSet = (childIters zip newChildCtrs) map {
        case (ci, ctr) =>
          ci === ctr.start
      }
      val isLastSet = (childIters zip newChildCtrs) map {
        case (ci, ctr) =>
          val nextIter = ctr.step.unbox.asInstanceOf[I32] + ci
          val end = ctr.end.asInstanceOf[I32]
          nextIter >= end
      }
      val index = Vec.fromSeq(parentIters ++ childIters)
      indexFIFO.enq(index)
      isFirstFIFO.enq(isFirstSet.reduceTree({_ && _}))
      isLastFIFO.enq(isLastSet.reduceTree({_ && _}))
    }, parentIters ++ childIters, f(parentStopWhen)))

    StreamCounter(indexFIFO, isFirstFIFO, isLastFIFO)
  }

  def stagePreamble(ctrl: Control[_], newParentCtrs: Seq[Counter[_]], parentIters: Seq[I32]): (CounterChain, Seq[I32]) = {
    val (cchain, iters) = ctrl.cchains.head
    val newChildCtrs = cchain.counters map {
      case Op(CounterNew(start, stop, step, par)) =>
        stage(CounterNew[I32](f(start).asInstanceOf[I32], f(stop).asInstanceOf[I32], f(step).asInstanceOf[I32] * par, I32(1)))
      case Op(ForeverNew()) =>
        stage(ForeverNew())
    }

    val newctrs = newParentCtrs ++ newChildCtrs
    val ccnew = stage(CounterChainNew(newctrs))

    val alliters = parentIters ++ iters

    val newiters = alliters.zip(newctrs).map { case (i, ctr) =>
      val n = boundVar[I32]
      n.name = i.name
      n.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1) { i => i })
      n
    }
    (ccnew, newiters)
  }
}
