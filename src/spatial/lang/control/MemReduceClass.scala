package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node._
import spatial.metadata.memory._
import spatial.util.memops._

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
    val cchainMap = CounterChain(domain)
    val acc = C.evMem(accum)

    val starts  = acc.starts()
    val strides = acc.steps()
    val ends    = acc.ends()
    val pars    = acc.pars()
    val ctrsRed = (0 to acc.seqRank.length-1).map{i =>
        Counter[I32](start = 0, step = strides(acc.seqRank(i)), end = ends(acc.seqRank(i)) - starts(acc.seqRank(i)), par = pars(acc.seqRank(i)))
      }
    val cchainRed = CounterChain(ctrsRed)

    //logs(s"Creating MemReduce on accumulator of rank ${acc.seqRank.length}")

    val itersMap = List.fill(domain.length){ boundVar[I32] }
    val itersRed = List.fill(acc.seqRank.length){ boundVar[I32] }

    //logs(s"  itersMap: $itersMap")
    //logs(s"  itersRed: $itersRed")

    val lA = boundVar[A]
    val rA = boundVar[A]
    val mapBlk: Block[C[A]] = stageBlock{ map(itersMap) }
    val redBlk: Lambda2[A,A,A] = stageLambda2(lA,rA){ reduce(lA, rA) }
    val resLd:  Lambda1[C[A],A] = stageLambda1(mapBlk.result){ C.evMem(mapBlk.result.unbox).__read(itersRed, Set.empty) }
    val accLd:  Lambda1[C[A],A] = stageLambda1(acc){ acc.__read(itersRed, Set.empty) }
    val accSt:  Lambda2[C[A],A,Void] = stageLambda2(acc, redBlk.result){ acc.__write(redBlk.result.unbox,itersRed,Set.empty) }
    stageWithFlow(OpMemReduce[A,C](
      ens = Set.empty,
      cchainMap,
      cchainRed,
      accum,
      mapBlk,
      resLd,
      accLd,
      redBlk,
      accSt,
      ident,
      fold,
      itersMap,
      itersRed)
    ){pipe =>
      opt.set(pipe)
    }
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
