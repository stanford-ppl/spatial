package spatial.lang
package control

import core._
import forge.tags._

protected class ReduceAccum[A](accum: Option[Reg[A]], zero: Option[A], fold: Option[A], opt: CtrlOpt) {
  @rig private def acc(implicit A: Bits[A]): Reg[A] = accum.getOrElse(Reg[A])

  /** 1 dimensional reduction **/
  @api def apply(domain: Counter[I32])(map: I32 => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    apply(Seq(domain)){l => map(l.head) }{reduce}
  }
  /** 2 dimensional reduction **/
  @api def apply(domain1: Counter[I32], domain2: Counter[I32])(map: (I32,I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    apply(Seq(domain1,domain2)){l => map(l(0),l(1)) }{reduce}
  }

  /** 3 dimensional reduction **/
  @api def apply(domain1: Counter[I32], domain2: Counter[I32], domain3: Counter[I32])(map: (I32,I32,I32) => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    apply(Seq(domain1,domain2,domain3)){l => map(l(0),l(1),l(2)) }{reduce}
  }

  /** N dimensional reduction **/
  @api def apply(domain: Seq[Counter[I32]])(map: List[I32] => A)(reduce: (A,A) => A)(implicit A: Bits[A]): Reg[A] = {
    ???
  }
}
protected class ReduceConstant[A](a: Lift[A], isFold: Boolean, opt: CtrlOpt) {
  @rig private def accum(implicit A: Bits[A]) = Some(Reg[A](a.unbox))
  private def init = Some(a.unbox)
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
  /** Reduction with implicit accumulator **/
  def apply[A](zero: Lift[A]) = new ReduceConstant[A](zero, isFold = false, opt)

  /** Reduction with explicit accumulator **/
  def apply[T](accum: Reg[T]) = new ReduceAccum(Some(accum), None, None, opt)
}

protected class FoldClass(opt: CtrlOpt) {
  /** Fold with implicit accumulator **/
  def apply[A](zero: Lift[A]) = new ReduceConstant[A](zero, isFold = true, opt)

  /** Fold with explicit accumulator **/
  def apply[A](accum: Reg[A]) = new MemFoldClass(opt).apply(accum)
}