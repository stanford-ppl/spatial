package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node.MemReduceBlackBox

protected class MemReduceAccum[A,C[T]](
  accum: C[A],
  ident: Option[A],
  fold:  Boolean,
  opt:   CtrlOpt
) {
  /** 1 dimensional memory reduction */
  @api def apply(domain1: Counter[I32])(map: I32 => C[A])(reduce: (A,A) => A)(implicit A: Bits[A], C: LocalMem[A,C]): C[A] = {
    apply(Seq(domain1)){x => map(x(0)) }{reduce}
  }

  /** 2 dimensional memory reduction */
  @api def apply(domain1: Counter[I32], domain2: Counter[I32])(map: (I32,I32) => C[A])(reduce: (A,A) => A)(implicit A: Bits[A], C: LocalMem[A,C]): C[A] = {
    apply(Seq(domain1,domain2)){x => map(x(0),x(1)) }{reduce}
  }

  /** 3 dimensional memory reduction */
  @api def apply(domain1: Counter[I32], domain2: Counter[I32], domain3: Counter[I32])(map: (I32,I32,I32) => C[A])(reduce: (A,A) => A)(implicit A: Bits[A], C: LocalMem[A,C]): C[A] = {
    apply(Seq(domain1,domain2,domain3)){x => map(x(0),x(1),x(2)) }{reduce}
  }

  /** N dimensional memory reduction */
  @api def apply(domain: Seq[Counter[I32]])(map: List[I32] => C[A])(reduce: (A,A) => A)(implicit A: Bits[A], C: LocalMem[A,C]): C[A] = {
    val cchain = CounterChain(domain)
    val iters = List.fill(domain.length){ bound[I32] }
    val lA = bound[A]
    val rA = bound[A]
    val mapBlk: Block[C[A]] = stageBlock{ map(iters) }
    val redBlk: Lambda2[A,A,A] = stageLambda2(lA,rA){ reduce(lA, rA) }
    val pipe = stage(MemReduceBlackBox[A,C](Set.empty,cchain,accum,mapBlk,redBlk,ident,fold,iters))
    opt.set(pipe)
    accum
  }
}

protected class MemReduceClass(opt: CtrlOpt) {
  def apply[A,C[T]](accum: C[A]) = new MemReduceAccum[A,C](accum, None, fold = false, opt)
  def apply[A,C[T]](accum: C[A], zero: A) = new MemReduceAccum[A,C](accum, Some(zero), fold = false, opt)
}

protected class MemFoldClass(opt: CtrlOpt) {
  def apply[A,C[T]](accum: C[A]) = new MemReduceAccum[A,C](accum, None, fold = true, opt)
  def apply[A,C[T]](accum: C[A], zero: A) = new MemReduceAccum[A,C](accum, Some(zero), fold = true, opt)
}
