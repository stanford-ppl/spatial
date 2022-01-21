package spatial.util

import argon.lang.implicits.castType
import argon.lang.types.Bits
import argon._
import forge.tags.{api, stateful}
import spatial.lang._
import spatial.node._
import spatial.metadata.control._

object TransformUtils {

  @api def expandCounterPar(x: Counter[_]): Counter[_] = x match {
    case ctr@Op(CounterNew(start, end, step, par)) =>
      type CT = ctr.CT
      implicit def ev: Num[CT] = ctr.CTeV
      val casted: CT = argon.lang.implicits.numericCast[I32, CT].apply(par)
      stage(CounterNew(start.asInstanceOf[Num[CT]], end.asInstanceOf[Num[CT]], (step.asInstanceOf[Num[CT]] * casted).asInstanceOf[Num[CT]], I32(1)))
  }

  @stateful def expandCounterPars(cchain: CounterChain): CounterChain = {
    val newCounters = cchain.counters.map(expandCounterPar)
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

  @stateful def CreateVecEV[T: Bits](length: Int): Bits[Vec[T]] = {
    Vec.fromSeq(Range(0, length) map {_ => implicitly[Bits[T]].zero})
  }

  @api def makeIters(ctrs: Seq[Counter[_]]): Seq[_] = {
    ctrs map { ctr =>
      implicit def tpEV: Type[ctr.CT] = ctr.CTeV.asInstanceOf[Type[ctr.CT]]
      val n = boundVar[ctr.CT]
      n.asInstanceOf[Bits[ctr.CT]].counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1) { i => i })
      n
    }
  }

  def makeSymOpPair(sym: Sym[_]): Option[(Sym[_], Op[_])] = {
    sym.op match {
      case Some(op) => Some((sym, op))
      case None => None
    }
  }
}

trait TransformerUtilMixin {
  this: argon.transform.ForwardTransformer =>

  import TransformUtils._

  case class RemappedChainData(counterChain: CounterChain, parentIters: Seq[I32], childIters: Seq[I32])

  /**
    * Creates a new CounterChain and Iters
    * @param parentChain
    * @param childChain
    * @param parentIters
    * @param childIters
    */
  def parentAndFlattenedChildChain(parentChain: CounterChain, childChain: CounterChain, parentIters: Seq[Sym[_]], childIters: Seq[Sym[_]]): RemappedChainData = {
    val parentCounters = parentChain.counters.map {
      ctr =>
        mirrorSym(ctr).unbox
    }
    val childCounters = childChain.counters map {
      ctr => expandCounterPar(ctr)
    }
    val ctrChain = stage(CounterChainNew((parentCounters ++ childCounters)))
    val newParentIters = makeIters(parentCounters).map {_.asInstanceOf[I32]}
    val newChildIters = makeIters(childCounters).map {_.asInstanceOf[I32]}
    dbgs(s"New Chain: $ctrChain = ${ctrChain.rhs}")
    dbgs(s"New Parent Iters: $newParentIters")
    dbgs(s"New Child Iters: $newChildIters")
    RemappedChainData(ctrChain, newParentIters, newChildIters)
  }
}
