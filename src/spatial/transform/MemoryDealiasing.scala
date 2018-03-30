package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.util._

case class MemoryDealiasing(IR: State) extends MutateTransformer {
  override val allowUnsafeSubst: Boolean = true // Allow mem -> Invalid (to drop)

  def recomputeAddr(series: Series[Idx], idx: Idx): Idx = {
    def _recomputeAddr[A<:Exp[_,A]:IntLike](series: Series[A], idx: A): Idx = {
      import IntLike._
      (idx * series.step + series.start).asInstanceOf[Idx]
    }
    _recomputeAddr[Idx](series, idx)
  }

  def readMux[A:Bits,Src[T]](
    conds:  Seq[Bit],
    mems:   Seq[Src[A]],
    ranges: Seq[Seq[Series[Idx]]],
    addr:   Seq[Idx],
    ens:    Set[Bit]
  ): A = {
    val reads = (conds,mems,ranges).zipped.map{case (c, mem2, rngs) =>
      val addr2 = rngs.zip(addr).map{case (rng, idx) => recomputeAddr(rng, f(idx)) }
      val mem = mem2.asInstanceOf[LocalMem[A,Src]]
      mem.__read(addr2, ens + c)
    }
    oneHotMux(conds, reads)
  }

  def writeDemux[A:Bits,Src[T]](
    data:   A,
    conds:  Seq[Bit],
    mems:   Seq[Src[A]],
    ranges: Seq[Seq[Series[Idx]]],
    addr:   Seq[Idx],
    ens:    Set[Bit]
  ): Seq[Sym[Void]] = {
    (conds,mems,ranges).zipped.map{case (c, mem2, rngs) =>
      val addr2 = rngs.zip(addr).map{case (rng, idx) => recomputeAddr(rng, f(idx)) }
      val mem = mem2.asInstanceOf[LocalMem[A,Src]]
      mem.__write(data, addr2, ens + c)
    }
  }

  def resetDemux[A,Src[T]](
    conds:  Seq[Bit],
    mems:   Seq[Src[A]],
    ens:    Set[Bit]
  ): Seq[Sym[Void]] = {
    (conds,mems).zipped.map{case (c, mem2) =>
      val mem = mem2.asInstanceOf[LocalMem[A,Src]]
      mem.__reset(ens + c)
    }
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    // These are still needed to track accumulators for Reduce, MemReduce
    //case _: MemDenseAlias[_,_,_]    => Invalid.asInstanceOf[Sym[A]]
    //case _: MemSparseAlias[_,_,_,_] => Invalid.asInstanceOf[Sym[A]]

    case op @ GetDRAMAddress(Op(MemDenseAlias(F(conds),F(mems),_))) =>
      implicit val ba: Bits[_] = op.A
      val addrs = mems.map{mem => stage(GetDRAMAddress(mem.asInstanceOf[DRAM[A,C forSome{type C[_]}]])) }
      oneHotMux(conds, addrs).asInstanceOf[Sym[A]]

    case op: Reader[_,_] if op.mem.isDenseAlias =>
      val Reader(Op(MemDenseAlias(F(conds),F(mems),F(ranges))), addr, F(ens)) = op

      readMux(conds, mems, ranges, addr, ens)(op.A).asInstanceOf[Sym[A]]

    case op: Writer[_] if op.mem.isDenseAlias =>
      val Writer(Op(MemDenseAlias(F(conds),F(mems),F(ranges))), F(data), F(addr), F(ens)) = op

      writeDemux(data,conds,mems,ranges,addr,ens)(Bits.m(op.A)).head.asInstanceOf[Sym[A]]

    case op: Resetter[_] if op.mem.isDenseAlias =>
      val Resetter(Op(MemDenseAlias(F(conds),F(mems),_)), F(ens)) = op

      resetDemux(conds,mems,ens).head.asInstanceOf[Sym[A]]

    case op: StatusReader[_] if op.mem.isDenseAlias =>
      val StatusReader(mem @ Op(MemDenseAlias(F(conds),F(mems),_)), F(ens)) = op

      val reads = conds.zip(mems).map{case (c,mem2) =>
        isolateSubstWith(mem -> mem2.asInstanceOf[Sym[_]]){
          transferMetadataIfNew(lhs){ stage(op.mirrorEn(f, Set(c))).asInstanceOf[Sym[A]] }._1
        }
      }
      implicit val A: Bits[A] = op.R.asInstanceOf[Bits[A]]
      box(oneHotMux(conds, reads.map(_.unbox)))

    case _ => super.transform(lhs, rhs)
  }
}
