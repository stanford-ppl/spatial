package spatial.util

import argon._
import forge.tags._
import spatial.data._
import spatial.lang._
import spatial.node._

import utils.implicits.collections._

object memops {

  implicit class AliasOps[A](x: Sym[A]) {
    def rank: Seq[Int] = rankOf(x)

    @rig def starts(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get starts of sparse alias")
      Seq.tabulate(rank.length){i => stage(MemStart(x, rank(i))) }
    }
    @rig def steps(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get steps of sparse alias")
      Seq.tabulate(rank.length){i => stage(MemStep(x, rank(i))) }
    }
    @rig def ends(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get ends of sparse alias")
      Seq.tabulate(rank.length){i => stage(MemEnd(x, rank(i))) }
    }
    @rig def pars(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get pars of sparse alias")
      Seq.tabulate(rank.length){i => stage(MemPar(x, rank(i))) }
    }
    @rig def lens(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get lens of sparse alias")
      Seq.tabulate(rank.length){i => stage(MemLen(x, rank(i))) }
    }
    @rig def series(): Seq[Series[I32]] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get series of sparse alias")
      Seq.tabulate(rank.length){i =>
        val start = stage(MemStart(x, rank(i)))
        val end   = stage(MemEnd(x, rank(i)))
        val step  = stage(MemStep(x, rank(i)))
        val par   = stage(MemPar(x, rank(i)))
        Series(start, end, step, par)
      }
    }

    @rig def addrs() = x match {
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

      case _ => throw new Exception(s"No sparse addresses available for $x")
    }



    // TODO[2]: Units
    //def units(): Seq[Boolean] = metadata[AliasUnit](x).get.unit()
  }

}
