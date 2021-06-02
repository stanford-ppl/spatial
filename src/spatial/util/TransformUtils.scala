package spatial.util

import argon.lang.implicits.castType
import argon.lang.{Arith, Bit, I32, Num}
import argon.{Op, stage}
import forge.tags.stateful
import spatial.lang.{Counter, CounterChain}
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.types._

object TransformUtils {

  @stateful def expandCounterPars(cchain: CounterChain): CounterChain = {
    val newCounters = cchain.counters.map {
      case ctr@Op(CounterNew(start, end, step, par)) =>
        type CT = ctr.CT
        implicit def ev: Num[CT] = ctr.CTeV
        val casted: CT = argon.lang.implicits.numericCast[I32, CT].apply(par)
        stage(CounterNew(start.asInstanceOf[Num[CT]], end.asInstanceOf[Num[CT]], (step.asInstanceOf[Num[CT]] * casted).asInstanceOf[Num[CT]], I32(1)))
    }
    stage(CounterChainNew(newCounters))
  }

  @stateful def isFirstIter[T: Num](iter: Seq[T], cchain: CounterChain): Seq[Bit] = {
    assert(iter.size == cchain.counters.size, s"Iterator(${iter.size}) and CChain (${cchain.counters.size}) must have identical sizes.")
    (iter zip cchain.counters) map {
      case (it, ctr) =>
        type TP = ctr.CT
        implicit def tpev: Num[TP] = ctr.CTeV.asInstanceOf[Num[TP]]
        implicit def cast: argon.Cast[T, TP] = argon.lang.implicits.numericCast[T, TP]
        isFirstIter(it.to[TP], ctr.asInstanceOf[Counter[TP]])
    }
  }

  @stateful def isLastIter[T: Num](iter: Seq[T], cchain: CounterChain): Seq[Bit] = {
    assert(iter.size == cchain.counters.size, s"Iterator(${iter.size}) and CChain (${cchain.counters.size}) must have identical sizes.")
    (iter zip cchain.counters) map {
      case (it, ctr) =>
        type TP = ctr.CT
        implicit def tpev: Num[TP] = ctr.CTeV.asInstanceOf[Num[TP]]
        implicit def cast: argon.Cast[T, TP] = argon.lang.implicits.numericCast[T, TP]
        isLastIter(it.to[TP], ctr.asInstanceOf[Counter[TP]])
    }
  }

  @stateful def isFirstIter[T: Num](iter: T, ctr: Counter[T]): Bit = {
    ctr.start.unbox.asInstanceOf[Num[T]].eql(iter)
  }

  @stateful def isLastIter[T: Num: Arith](iter: T, ctr: Counter[T]): Bit = {
    val nextIter = iter.asInstanceOf[Arith[T]] + ctr.step.unbox
    ctr.end.unbox.asInstanceOf[Num[T]] >= nextIter
  }

//  // Expands a Foreach with Par Factors into one without by performing unrolling here.
//  @stateful def earlyExpandForeach(foreach: OpForeach): (Seq[Counter[_]], CounterChain, OpForeach) = {
//    val parFactors = foreach.cchain.counters.map(_.ctrParOr1)
//    val shifts = spatial.util.computeShifts(parFactors)
//    val newCounters = foreach.cchain.counters.map {
//      case ctr@Op(CounterNew(start, end, step, par)) =>
//        // All these asInstanceOfs are to
//        type CT = ctr.CT
//        implicit def ev: Num[CT] = ctr.CTeV
//        val stepCT = step.asInstanceOf[Num[CT]]
//        val parCT = ctr.CTeV.from(par.toInt).asInstanceOf[CT]
//        val newStep = stepCT * parCT
//        stage(CounterNew(start.asInstanceOf[Num[CT]], end.asInstanceOf[Num[CT]], newStep.asInstanceOf[Num[CT]], I32(1)))
//    }
//
//    OpForeach(foreach.ens, cchain, argon.stageBlock {
//
//    }, iters, stopWhen)
//  }
}
