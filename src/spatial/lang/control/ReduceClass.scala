package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node.OpReduce

protected class ReduceAccum[A](accum: Option[Reg[A]], ident: Option[A], init: Option[A], opt: CtrlOpt) {

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
    val pipe = stage(OpReduce[A](Set.empty,cchain,acc,mapBlk,ldBlk,redBlk,stBlk,ident,init,iters))
    opt.set(pipe)
    acc
  }
}
protected class ReduceConstant[A](a: A, isFold: Boolean, opt: CtrlOpt) {
  @rig private def accum(implicit A: Bits[A]) = Some(Reg[A](a))
  private def init = Some(a)
  private def fold = if (isFold) init else None
  private def zero = if (!isFold) init else None

  @api def apply(domain1: Counter[I32])(map: I32 => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    new ReduceAccum(accum, zero, fold, opt).apply(domain1)(map)(reduce)
  }
  @api def apply(domain1: Counter[I32], domain2: Counter[I32])(map: (I32,I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    new ReduceAccum(accum, zero, fold, opt).apply(domain1, domain2)(map)(reduce)
  }
  @api def apply(domain1: Counter[I32], domain2: Counter[I32], domain3: Counter[I32])(map: (I32, I32, I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    new ReduceAccum(accum, zero, fold, opt).apply(domain1, domain2, domain3)(map)(reduce)
  }
  @api def apply(domain: Seq[Counter[I32]])(map: List[I32] => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    new ReduceAccum(accum, zero, fold, opt).apply(domain)(map)(reduce)
  }
}

protected class ReduceClass(opt: CtrlOpt) extends ReduceAccum(None, None, None, opt) {
  /** Reduction with implicit accumulator */
  def apply[A](zero: Lift[A]) = new ReduceConstant[A](zero.unbox, isFold = false, opt)
  def apply[A](zero: Sym[A]) = new ReduceConstant[A](zero.unbox, isFold = false, opt)

  /** Reduction with explicit accumulator */
  def apply[T](accum: Reg[T]) = new ReduceAccum(Some(accum), None, None, opt)
}

protected class FoldClass(opt: CtrlOpt) {
  /** Fold with implicit accumulator */
  def apply[A](zero: Lift[A]) = new ReduceConstant[A](zero.unbox, isFold = true, opt)
  def apply[A](zero: Sym[A]) = new ReduceConstant[A](zero.unbox, isFold = false, opt)

  /** Fold with explicit accumulator */
  def apply[A](accum: Reg[A]) = new MemFoldClass(opt).apply(accum)
}