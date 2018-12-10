package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node._

sealed abstract class ReduceLike[A] {
  @api def apply(domain1: Counter[I32])(map: I32 => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A]
  @api def apply(domain1: Counter[I32], domain2: Counter[I32])(map: (I32,I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A]
  @api def apply(domain1: Counter[I32], domain2: Counter[I32], domain3: Counter[I32])(map: (I32, I32, I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A]
  @api def apply(domain1: Counter[I32], domain2: Counter[I32], domain3: Counter[I32], domain4: Counter[I32], domains: Counter[I32]*)(map: List[I32] => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A]
  @api def apply(domain: Seq[Counter[I32]])(map: List[I32] => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A]
}

protected class ReduceAccum[A](accum: Option[Reg[A]], ident: Option[A], init: Option[A], opt: CtrlOpt, stopWhen: Option[Reg[Bit]]) extends ReduceLike[A] {

  /** 1 dimensional reduction */
  @api def apply(domain: Counter[I32])(map: I32 => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    apply(Seq(domain)){l => map(l.head) }{reduce}
  }
  /** 2 dimensional reduction */
  @api def apply(domain1: Counter[I32], domain2: Counter[I32])(map: (I32,I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    apply(Seq(domain1,domain2)){l => map(l(0),l(1)) }{reduce}
  }

  /** 3 dimensional reduction */
  @api def apply(domain1: Counter[I32], domain2: Counter[I32], domain3: Counter[I32])(map: (I32,I32,I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    apply(Seq(domain1,domain2,domain3)){l => map(l(0),l(1),l(2)) }{reduce}
  }

  @api def apply(domain1: Counter[I32], domain2: Counter[I32], domain3: Counter[I32], domain4: Counter[I32], domains: Counter[I32]*)(map: List[I32] => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    apply(Seq(domain1,domain2,domain3,domain4) ++ domains)(map){reduce}
  }

  /** N dimensional reduction */
  @api def apply(domain: Seq[Counter[I32]])(map: List[I32] => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    val acc = accum.getOrElse(Reg[A])
    val cchain = CounterChain(domain)
    val lA = boundVar[A]
    val rA = boundVar[A]
    val iters  = List.fill(domain.length){ boundVar[I32] }
    val mapBlk = stageBlock{ map(iters) }
    val ldBlk  = stageLambda1(acc){ acc.value }
    val redBlk = stageLambda2(lA,rA){ reduce(lA,rA) }
    val stBlk  = stageLambda2(acc,redBlk.result){ acc := redBlk.result.unbox }
    stageWithFlow(OpReduce[A](Set.empty,cchain,acc,mapBlk,ldBlk,redBlk,stBlk,ident,init,iters,stopWhen)){pipe =>
      opt.set(pipe)
    }
    acc
  }
}
protected class ReduceConstant[A](a: A, isFold: Boolean, opt: CtrlOpt, stopWhen: Option[Reg[Bit]]) extends ReduceLike[A] {
  @rig private def accum(implicit A: Bits[A]) = Some(Reg[A](a))
  private def init = Some(a)
  private def fold: Option[A] = if (isFold) init else None
  private def zero: Option[A] = if (!isFold) init else None

  @api def apply(domain1: Counter[I32])(map: I32 => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    new ReduceAccum(accum, zero, fold, opt, stopWhen).apply(domain1)(map)(reduce)
  }
  @api def apply(domain1: Counter[I32], domain2: Counter[I32])(map: (I32,I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    new ReduceAccum(accum, zero, fold, opt, stopWhen).apply(domain1, domain2)(map)(reduce)
  }
  @api def apply(domain1: Counter[I32], domain2: Counter[I32], domain3: Counter[I32])(map: (I32, I32, I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    new ReduceAccum(accum, zero, fold, opt, stopWhen).apply(domain1, domain2, domain3)(map)(reduce)
  }
  @api def apply(domain1: Counter[I32], domain2: Counter[I32], domain3: Counter[I32], domain4: Counter[I32], domains: Counter[I32]*)(map: List[I32] => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    new ReduceAccum(accum, zero, fold, opt, stopWhen).apply(Seq(domain1, domain2, domain3, domain4) ++ domains)(map)(reduce)
  }
  @api def apply(domain: Seq[Counter[I32]])(map: List[I32] => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    new ReduceAccum(accum, zero, fold, opt, stopWhen).apply(domain)(map)(reduce)
  }
}

protected class ReduceClass(opt: CtrlOpt, stopWhen: Option[Reg[Bit]]) extends ReduceAccum(None, None, None, opt, stopWhen) {
  /** Reduction with implicit accumulator */
  def apply[A](zero: Lift[A]) = new ReduceConstant[A](zero.unbox, isFold = false, opt, stopWhen)
  def apply[A](zero: Sym[A]): ReduceLike[A] = zero match {
    case Op(RegRead(reg)) => new ReduceAccum(Some(reg),None,None,opt, stopWhen) // TODO[4]: Hack to get explicit accum
    case _ => new ReduceConstant[A](zero.unbox, isFold = false, opt, stopWhen)
  }

  /** Reduction with explicit accumulator */
  def apply[T](accum: Reg[T]) = new ReduceAccum(Some(accum), None, None, opt, stopWhen)
}

protected class FoldClass(opt: CtrlOpt, stopWhen: Option[Reg[Bit]]) {
  /** Fold with implicit accumulator */
  def apply[A](zero: Lift[A]) = new ReduceConstant[A](zero.unbox, isFold = true, opt, stopWhen)
  def apply[A](zero: Sym[A]) = new ReduceConstant[A](zero.unbox, isFold = false, opt, stopWhen)

  /** Fold with explicit accumulator */
  def apply[A](accum: Reg[A]) = new MemFoldClass(opt, stopWhen).apply(accum)
}