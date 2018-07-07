package spatial.util

import argon._
import forge.tags._

import spatial.lang._
import spatial.node._
import spatial.metadata.memory._

import utils.implicits.collections._

object memops {

  implicit class AliasOps[A](mem: Sym[A]) {
    @rig def starts(): Seq[I32] = {
      if (mem.isSparseAlias) throw new Exception(s"Cannot get starts of sparse alias")
      Seq.tabulate(mem.seqRank.length){i => stage(MemStart(mem, mem.seqRank(i))) }
    }
    @rig def steps(): Seq[I32] = {
      if (mem.isSparseAlias) throw new Exception(s"Cannot get steps of sparse alias")
      Seq.tabulate(mem.seqRank.length){i => stage(MemStep(mem, mem.seqRank(i))) }
    }
    @rig def ends(): Seq[I32] = {
      if (mem.isSparseAlias) throw new Exception(s"Cannot get ends of sparse alias")
      Seq.tabulate(mem.seqRank.length){i => stage(MemEnd(mem, mem.seqRank(i))) }
    }
    @rig def pars(): Seq[I32] = {
      if (mem.isSparseAlias) throw new Exception(s"Cannot get pars of sparse alias")
      Seq.tabulate(mem.seqRank.length){i => stage(MemPar(mem, mem.seqRank(i))) }
    }
    @rig def lens(): Seq[I32] = {
      Seq.tabulate(mem.seqRank.length){i => stage(MemLen(mem, mem.seqRank(i))) }
    }

    @rig def rawStarts(): Seq[I32] = {
      if (mem.isSparseAlias) throw new Exception(s"Cannot get rawStarts of sparse alias")
      Seq.tabulate(mem.rawRank.length){i => stage(MemStart(mem, mem.rawRank(i))) }
    }

    @rig def rawDims(): Seq[I32] = {
      if (mem.isSparseAlias) throw new Exception(s"Cannot get rawDims of sparse alias")
      Seq.tabulate(mem.rawRank.length){i => stage(MemDim(mem, mem.rawRank(i))) }
    }

    @rig def series(): Seq[Series[I32]] = {
      if (mem.isSparseAlias) throw new Exception(s"Cannot get series of sparse alias")
      Seq.tabulate(mem.seqRank.length){i =>
        val start = stage(MemStart(mem, mem.seqRank(i)))
        val end   = stage(MemEnd(mem, mem.seqRank(i)))
        val step  = stage(MemStep(mem, mem.seqRank(i)))
        val par   = stage(MemPar(mem, mem.seqRank(i)))
        Series(start, end, step, par)
      }
    }

    @rig def addrs() = mem match {
      case Op(op: MemSparseAlias[_,_,_,_]) =>
        def addrAlias[Addr[T]](implicit Addr: Type[Addr[I32]]) = {
          val addr = op.addr.map{mem => mem.asInstanceOf[Addr[I32]]}

          if (addr.lengthMoreThan(1)) {
            val ranges = addr.map{mem => Addr.boxed(mem).series() }
            stage(MemDenseAlias[I32,Addr,Addr](op.cond,addr,ranges))
          }
          else addr.head
        }
        addrAlias(op.Addr).asInstanceOf[LocalMem[I32,C forSome{type C[_]}]]

      case _ => throw new Exception(s"No sparse addresses available for $mem")
    }



    // TODO[2]: Units
    //def units(): Seq[Boolean] = metadata[AliasUnit](x).get.unit()
  }

}
