package spatial.util

import argon.lang.implicits.castType
import argon.lang.types.Bits
import argon._
import argon.transform.{ForwardTransformer, MutateTransformer, TransformerInterface}
import forge.tags.{api, stateful}
import spatial.lang._
import spatial.metadata.access.TimeStamp
import spatial.node._
import spatial.metadata.control._
import spatial.traversal.AccelTraversal

object TransformUtils {

  @api def isFirstIter[T: Num](iter: Num[T]): Bit = {
    val ctr = iter.counter.ctr.asInstanceOf[Counter[Num[T]]]
    implicit def castEV: Cast[I32, T] = argon.lang.implicits.numericCast[I32, T]
//    iter < (ctr.start.unbox + (ctr.step.unbox * ctr.ctrPar.to[T]))
    iter === ctr.start.unbox
  }

  @api def isFirstIters[T: Num](iters: Num[T]*): Seq[Bit] = {
    val isFirst = iters.map(isFirstIter(_))
    isFirst.scanRight(Bit(true)){_&_}.dropRight(1)
  }

  @api def isLastIter[T: Num](iter: Num[T]): Bit = {
    val ctr = iter.counter.ctr.asInstanceOf[Counter[T]]
    val nextIter = iter + ctr.step.unbox
    ctr.end.unbox.asInstanceOf[Num[T]] <= nextIter
  }

  @api def isLastIters[T: Num](iters: Num[T]*): Seq[Bit] = {
    val isLast = iters.map(isLastIter(_))
    isLast.scanRight(Bit(true)){_&_}.dropRight(1)
  }

  @api def makeIters(ctrs: Seq[Counter[_]]): Seq[Sym[_]] = {
    ctrs map(makeIter(_))
  }

  @api def makeIter[T](ctr: Counter[T]): Sym[T] = {
    implicit def tpEV: Type[ctr.CT] = ctr.CTeV.asInstanceOf[Type[ctr.CT]]
    val n = boundVar[ctr.CT]
    n.asInstanceOf[Bits[ctr.CT]].counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1) { i => i })
    n.asSym
  }

  @api def counterToSeries(ctr: Counter[_]): Seq[Int] = {
    val start = ctr.start.c.get.asInstanceOf[emul.FixedPoint].toInt
    val end = ctr.end.c.get.asInstanceOf[emul.FixedPoint].toInt
    val step = ctr.step.c.get.asInstanceOf[emul.FixedPoint].toInt
    Range(start, end, step)
  }

  @api def willRun[T: Num](ctr: Counter[T], f: TransformerInterface): Bit = {
    val direction: Bit = f(ctr.step.unbox.asInstanceOf[Num[T]]) > Num[T].zero
    val start = f(ctr.start.unbox.asInstanceOf[Num[T]])
    val end = f(ctr.end.unbox)
    mux(direction, start <= end, start >= end)
  }

  @api def willRunUT(ctr: Counter[_], f: TransformerInterface): Bit = {
    implicit def nEV: Num[ctr.CT] = ctr.CTeV.asInstanceOf[Num[ctr.CT]]
    willRun(ctr.asInstanceOf[Counter[ctr.CT]], f)
  }

  @api def withPreviousCtx(previousCtx: SrcCtx*): SrcCtx = {
    implicitly[SrcCtx].copy(previous=previousCtx)
  }

  @api def augmentCtx(originalCtx: SrcCtx): SrcCtx = {
    originalCtx.copy(previous = originalCtx.previous ++ Seq(implicitly[SrcCtx]))
  }

  @api def getOutermostCounter(ctrls: Seq[Ctrl]): Option[Counter[_]] = {
    ctrls.flatMap(_.s).flatMap(_.cchains).flatMap(_.counters).headOption
  }

  @api def getOutermostIter(ctrls: Seq[Ctrl]): Option[Sym[_ <: Num[_]]] = {
    getOutermostCounter(ctrls).flatMap(_.iter).asInstanceOf[Option[Sym[_ <: Num[_]]]]
  }

  @api def pseudoUnitpipe(body: Block[Void]) = {
    val ctr = Counter(I32(0), I32(1), I32(1), I32(1))
    val cchain = CounterChain(Seq(ctr))
    stage(OpForeach(Set.empty, cchain, body, Seq(makeIter(ctr).unbox), None))
  }

  @api def isFirstIter(timestamp: TimeStamp, counters: Seq[Counter[_]]): Bit = {
    if (counters.isEmpty) return Bit(true)
    type CType = T forSome {type T <: Num[T]}
    (counters.map {
      case counter: Counter[CType] =>
        val iter = counter.iter.get
        implicit def nEV: Num[CType] = counter.CTeV.asInstanceOf[Num[CType]]
        val currentTime = timestamp(iter)
        counter.start.unbox === currentTime
    }).reduceTree(_ & _)
  }

  @api def isLastIter(timestamp: TimeStamp, counters: Seq[Counter[_]]): Bit = {
    if (counters.isEmpty) return Bit(true)
    (counters.map {
      counter =>
        implicit def bEV: Bits[counter.CT] = counter.CTeV.asInstanceOf[Bits[counter.CT]]
        implicit def nEV: Num[counter.CT] = counter.CTeV.asInstanceOf[Num[counter.CT]]
        val castCtr: Counter[Num[counter.CT]] = counter.asInstanceOf[Counter[Num[counter.CT]]]
        val iter = castCtr.iter.get
        val currentTime = timestamp(iter.unbox.unbox.asSym)
        val next = currentTime.asInstanceOf[Num[counter.CT]] + castCtr.step.unbox.asInstanceOf[counter.CT]
        next.asInstanceOf[Num[counter.CT]] >= castCtr.end.unbox.asInstanceOf[counter.CT]
    }).reduceTree(_ & _)
  }
}

trait TransformerUtilMixin[T <: argon.transform.ForwardTransformer] {
  this: T =>

  def createSubstData(thunk: => Unit): TransformerStateBundle = {
    val tmp = saveSubsts()
    thunk
    val result = saveSubsts()
    restoreSubsts(tmp)
    result
  }

  def visitWithSubsts(substs: Seq[TransformerStateBundle], stms: Seq[Sym[_]])(func: Sym[_] => Any)(implicit ctx: SrcCtx): Seq[TransformerStateBundle] = {
    val currentSubsts = saveSubsts()
    val substitutions = substs.toArray

    def cyclingVisit(sym: Sym[_]): Unit = {
      updateSubstsWith({func(sym)})
    }

    def updateSubstsWith(thunk: => Unit): Unit = {
      substitutions.zipWithIndex foreach {
        case (data, ind) =>
          restoreSubsts(data)
          thunk
          substitutions(ind) = saveSubsts()
      }
    }

    stms foreach {
      case unitpipe@Op(UnitPipe(ens, block, stopWhen)) if unitpipe.isInnerControl =>
        stageWithFlow(UnitPipe(f(ens), stageBlock {
          block.stms foreach cyclingVisit
        }, f(stopWhen))) {
          lhs2 =>
            transferData(unitpipe, lhs2)
            lhs2.ctx = ctx.copy(previous = Seq(lhs2.ctx))
        }
      case stm if stm.isControl && substs.size > 1 => Parallel {cyclingVisit(stm)}
      case stm => cyclingVisit(stm)
    }
    restoreSubsts(currentSubsts)
    substitutions.toSeq
  }

  def mapSubsts[T](substs: Seq[TransformerStateBundle])(func: => T): Seq[T] = {
    val current = saveSubsts()
    val values = substs map {
      subst =>
        restoreSubsts(subst)
        func
    }
    restoreSubsts(current)
    values
  }

  def expandCounterPar(x: Counter[_]): Counter[_] = x match {
    case ctr@Op(CounterNew(start, end, step, par)) =>
      type CT = ctr.CT

      implicit def ev: Num[CT] = ctr.CTeV

      val casted: CT = argon.lang.implicits.numericCast[I32, CT].apply(par)
      val newStep = (f(step).asInstanceOf[Num[CT]] * f(casted)).asInstanceOf[Num[CT]]
      val mappedEnd = f(end)
      // Need to round end up to next multiple
//      val newEnd = {
//        val residual = mappedEnd.asInstanceOf[Num[CT]] % newStep.asInstanceOf[CT]
//        val bump = mux[CT](residual > ctr.CTeV.from(0), newStep - residual, ctr.CTeV.from(0))
//        mappedEnd.asInstanceOf[Num[CT]] + bump
//      }
      stage(CounterNew(f(start).asInstanceOf[Num[CT]], mappedEnd.asInstanceOf[Num[CT]], newStep, I32(1)))
  }

  def expandCounterPars(cchain: CounterChain): CounterChain = {
    val newCounters = cchain.counters.map(expandCounterPar)
    stage(CounterChainNew(newCounters))
  }
}

trait CounterIterUpdateMixin extends ForwardTransformer with AccelTraversal {

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case _:CounterNew[_] if inHw =>
      val transformed = super.transform(lhs, rhs)
      // Update the iter info as well
      if (transformed != lhs) {
        val newIter = TransformUtils.makeIter(transformed.asInstanceOf[Counter[_]])
        val oldIter = lhs.asInstanceOf[Counter[_]].iter.get
        register(oldIter, newIter)
      }
      transformed
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}
