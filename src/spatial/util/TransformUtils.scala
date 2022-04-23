package spatial.util

import argon.lang.implicits.castType
import argon.lang.types.Bits
import argon._
import argon.transform.MutateTransformer
import forge.tags.{api, stateful}
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.traversal.AccelTraversal

object TransformUtils {

  @api def expandCounterPar(x: Counter[_]): Counter[_] = x match {
    case ctr@Op(CounterNew(start, end, step, par)) =>
      type CT = ctr.CT
      implicit def ev: Num[CT] = ctr.CTeV
      val casted: CT = argon.lang.implicits.numericCast[I32, CT].apply(par)
      stage(CounterNew(start.asInstanceOf[Num[CT]], end.asInstanceOf[Num[CT]], (step.asInstanceOf[Num[CT]] * casted).asInstanceOf[Num[CT]], I32(1)))
  }

  @api def expandCounterPars(cchain: CounterChain): CounterChain = {
    val newCounters = cchain.counters.map(expandCounterPar)
    stage(CounterChainNew(newCounters))
  }

  @api def isFirstIter[T: Num](iter: Num[T]): Bit = {
    val ctr = iter.counter.ctr.asInstanceOf[Counter[Num[T]]]
    implicit def castEV: Cast[I32, T] = argon.lang.implicits.numericCast[I32, T]
    iter < (ctr.start.unbox + (ctr.step.unbox * ctr.ctrPar.to[T]))
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

  @api def CreateVecEV[T: Bits](length: Int): Bits[Vec[T]] = {
    Vec.fromSeq(Range(0, length) map {_ => implicitly[Bits[T]].zero})
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

  def makeSymOpPair(sym: Sym[_]): Option[(Sym[_], Op[_])] = {
    sym.op match {
      case Some(op) => Some((sym, op))
      case None => None
    }
  }

  @api def counterToSeries(ctr: Counter[_]): Seq[Int] = {
    val start = ctr.start.c.get.asInstanceOf[emul.FixedPoint].toInt
    val end = ctr.end.c.get.asInstanceOf[emul.FixedPoint].toInt
    val step = ctr.step.c.get.asInstanceOf[emul.FixedPoint].toInt
    Range(start, end, step)
  }

  @api def withPreviousCtx(previousCtx: SrcCtx*): SrcCtx = {
    implicitly[SrcCtx].copy(previous=previousCtx)
  }

  @api def persistentDequeue[T: Bits](fifo: FIFO[T], en: Set[Bit]): T = {
    val v = stage(FIFODeq(fifo, en))
    val reg = Reg[T]
    stage(RegWrite(reg, v, en))
    mux(en.toSeq.reduceTree {_ && _}, v, reg.value)
  }

  @api def getOutermostCounter(ctrls: Seq[Ctrl]): Option[Counter[_]] = {
    ctrls.flatMap(_.s).flatMap(_.cchains).flatMap(_.counters).headOption
  }

  @api def getOutermostIter(ctrls: Seq[Ctrl]): Option[Sym[_]] = {
    getOutermostCounter(ctrls).flatMap(_.iter)
  }
}

trait TransformerUtilMixin {
  this: argon.transform.MutateTransformer =>

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

  def createSubstData(thunk: => Unit): TransformerStateBundle = {
    val tmp = saveSubsts()
    thunk
    val result = saveSubsts()
    restoreSubsts(tmp)
    result
  }

  def visitWithSubsts(substs: Seq[TransformerStateBundle], stms: Seq[Sym[_]])(implicit ctx: SrcCtx): Seq[TransformerStateBundle] = {
    val currentSubsts = saveSubsts()
    val substitutions = substs.toArray

    def cyclingVisit(sym: Sym[_]): Unit = {
      updateSubstsWith({visit(sym)})
    }

    def updateSubstsWith(thunk: => Unit): Unit = {
      substitutions.zipWithIndex foreach {
        case (data, ind) =>
          restoreSubsts(data)
          thunk
          substitutions(ind) = saveSubsts()
      }
    }

    inCopyMode(substs.size > 1) {
      stms foreach {
        case unitpipe@Op(UnitPipe(ens, block, stopWhen)) if unitpipe.isInnerControl =>
          stageWithFlow(UnitPipe(f(ens), stageBlock {
            block.stms foreach cyclingVisit
          }, f(stopWhen))) {
            lhs2 =>
              transferData(unitpipe, lhs2)
              lhs2.ctx = ctx.copy(previous = Seq(lhs2.ctx))
          }
        case disguisedUnitpipe@Op(OpForeach(ens, cchain, block, iters, stopWhen)) if disguisedUnitpipe.isInnerControl && cchain.isStatic && cchain.approxIters == 1 =>
          updateSubstsWith({
            iters foreach {
              iter =>
                val counterStart = iter.ctrStart.asSym
                register(iter.asSym, () => f(counterStart))
            }
          })

          stageWithFlow(UnitPipe(f(ens), stageBlock {
            block.stms foreach cyclingVisit
          }, f(stopWhen))) {
            lhs2 =>
              transferData(disguisedUnitpipe, lhs2)
              lhs2.ctx = ctx.copy(previous = Seq(lhs2.ctx))
          }
        case stm => cyclingVisit(stm)
      }
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
}

trait CounterIterUpdateMixin extends MutateTransformer with AccelTraversal {

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case _:CounterNew[_] if inHw && copyMode =>
      val transformed = super.transform(lhs, rhs)
      // Update the iter info as well
      val newIter = TransformUtils.makeIter(transformed.asInstanceOf[Counter[_]])
      val oldIter = lhs.asInstanceOf[Counter[_]].iter.get
      register(oldIter, newIter)
      transformed
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}
