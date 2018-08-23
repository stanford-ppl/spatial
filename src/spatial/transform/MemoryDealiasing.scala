package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.memory._
import spatial.util.IntLike

import utils.implicits.collections._

case class MemoryDealiasing(IR: State) extends MutateTransformer {

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

  def fieldsRanges(
    ranges: Seq[Seq[Series[Idx]]],
    d:      Int
  )(field: Series[Idx] => Idx): Seq[I32] = {
    val fieldsIdx = ranges.map{rngs => rngs.get(d).map{range => field(range) }.getOrElse(I32(0)) }
    val fields = fieldsIdx.map{field: Idx =>
      implicit val T: Bits[Idx] = field.tp.asInstanceOf[Bits[Idx]]
      field.asUnchecked[I32]
    }
    fields
  }

  def dealiasRanges(
    conds:  Seq[Bit],
    ranges: Seq[Seq[Series[Idx]]],
    d:      Int,
  )(field: Series[Idx] => Idx): I32 = {
    val fields = fieldsRanges(ranges, d)(field)
    oneHotMux(conds, fields)
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    // These are still needed to track accumulators for Reduce, MemReduce
    // MemDenseAlias and MemSparseAlias are removed after unrolling in AliasCleanup
    case op: MemDenseAlias[_,_,_]    if op.mem.size == 1 => op.mem.head.asInstanceOf[Sym[A]]
    case op: MemSparseAlias[_,_,_,_] if op.mem.size == 1 => op.mem.head.asInstanceOf[Sym[A]]

    case op @ GetDRAMAddress(Op(MemDenseAlias(F(conds),F(mems),F(ranges)))) =>
      implicit val ba: Bits[_] = op.A
      val addrs = mems.map{mem => stage(GetDRAMAddress(mem.asInstanceOf[DRAM[A,C forSome{type C[_]}]])) }
      oneHotMux(conds, addrs)

    case op @ GetDRAMAddress(Op(MemSparseAlias(F(conds), F(mems), F(ranges), F(size)))) =>
      implicit val ba: Bits[_] = op.A
      val addrs = mems.map{mem => stage(GetDRAMAddress(mem.asInstanceOf[DRAM[A,C forSome{type C[_]}]]))}
      oneHotMux(conds, addrs)


    case MemDim(Op(MemDenseAlias(F(conds),F(ms),_)), d) =>
      val mems = ms.map(_.asInstanceOf[Sym[_]])
      val dims = mems.map{case Op(op: MemAlloc[_,_]) => op.dims.indexOrElse(d, I32(1)) }
      oneHotMux(conds, dims)

    case MemDim(Op(MemSparseAlias(F(conds),F(ms),_,_)),d) =>
      val mems = ms.map(_.asInstanceOf[Sym[_]])
      val dims = mems.map{case Op(op: MemAlloc[_,_]) => op.dims.indexOrElse(d, I32(1)) }
      oneHotMux(conds, dims)

    case MemRank(Op(op: MemDenseAlias[_,_,_])) => I32(op.rank.length)
    case MemRank(Op(op: MemSparseAlias[_,_,_,_])) => I32(op.rank.length)

    case MemLen(Op(MemDenseAlias(F(conds),_,F(ranges))), d)   => dealiasRanges(conds, ranges, d)(_.length)
    case MemLen(Op(MemSparseAlias(F(conds),_,_,F(sizes))), _) => oneHotMux(conds,sizes)

    case MemPar(Op(MemDenseAlias(_,_,F(ranges))), d)          => fieldsRanges(ranges, d)(_.par).head
    case MemPar(Op(MemSparseAlias(F(conds),_,F(addrs),_)), _) =>
      val pars = addrs.map(_.asInstanceOf[Sym[_]]).map{
        case Def(MemPar(Op(MemDenseAlias(_,_,F(ranges))), d)) => fieldsRanges(ranges, d)(_.par).head
        case _ => I32(1)
      }
      oneHotMux(conds,pars)

    // --- The remaining operations are currently disallowed for SparseAlias:

    case MemStart(Op(MemDenseAlias(F(conds),_,F(ranges))), d) => dealiasRanges(conds, ranges, d)(_.start)
    case MemEnd(Op(MemDenseAlias(F(conds),_,F(ranges))), d)   => dealiasRanges(conds, ranges, d)(_.end)
    case MemStep(Op(MemDenseAlias(F(conds),_,F(ranges))), d)  => dealiasRanges(conds, ranges, d)(_.step)

    case op: Reader[_,_] if op.mem.isDenseAlias =>
      val Reader(Op(MemDenseAlias(F(conds),F(mems),F(ranges))), addr, F(ens)) = op

      readMux(conds, mems, ranges, addr, ens)(op.A)

    case op: Writer[_] if op.mem.isDenseAlias =>
      val Writer(Op(MemDenseAlias(F(conds),F(mems),F(ranges))), F(data), F(addr), F(ens)) = op

      writeDemux(data,conds,mems,ranges,addr,ens)(Bits.m(op.A)).head

    case op: Resetter[_] if op.mem.isDenseAlias =>
      val Resetter(Op(MemDenseAlias(F(conds),F(mems),_)), F(ens)) = op

      resetDemux(conds,mems,ens).head

    case op: StatusReader[_] if op.mem.isDenseAlias =>
      val StatusReader(mem @ Op(MemDenseAlias(F(conds),F(mems),_)), F(ens)) = op

      val reads = conds.zip(mems).map{case (c,mem2) =>
        isolateSubstWith(escape=Nil, mem -> mem2.asInstanceOf[Sym[_]]){
          stageWithFlow(op.mirrorEn(f, Set(c))){lhs2 => transferData(lhs,lhs2) }.asInstanceOf[Sym[A]]
        }
      }
      implicit val A: Bits[A] = op.R.asInstanceOf[Bits[A]]
      box(oneHotMux(conds, reads.map(_.unbox)))

    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}
