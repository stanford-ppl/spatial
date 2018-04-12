package spatial.util

import argon._
import forge.tags._
import spatial.data._
import spatial.lang._
import spatial.node._

import utils.implicits.collections._

object memops {

  implicit class AliasOps[A](x: Sym[A]) {
    def rank: Int = rankOf(x)

    @rig def starts(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get starts of sparse alias")
      Seq.tabulate(rank){i => stage(MemStart(x, i)) }
    }
    @rig def steps(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get steps of sparse alias")
      Seq.tabulate(rank){i => stage(MemStep(x, i)) }
    }
    @rig def ends(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get ends of sparse alias")
      Seq.tabulate(rank){i => stage(MemEnd(x, i)) }
    }
    @rig def pars(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get pars of sparse alias")
      Seq.tabulate(rank){i => stage(MemPar(x, i)) }
    }
    @rig def lens(): Seq[I32] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get lens of sparse alias")
      Seq.tabulate(rank){i => stage(MemLen(x, i)) }
    }
    @rig def series(): Seq[Series[I32]] = {
      if (x.isSparseAlias) throw new Exception(s"Cannot get series of sparse alias")
      Seq.tabulate(rank){i =>
        val start = stage(MemStart(x, i))
        val end   = stage(MemEnd(x, i))
        val step  = stage(MemStep(x, i))
        val par   = stage(MemPar(x, i))
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
