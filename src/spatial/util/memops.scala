package spatial.util

import argon._
import forge.tags._
import spatial.data._
import spatial.lang._
import spatial.node._

object memops {

  implicit class AliasOps[A](x: Sym[A]) {
    def rank: Int = rankOf(x)

    @rig def starts(): Seq[I32] = Seq.tabulate(rank){i => stage(MemStart(x, i)) }
    @rig def steps(): Seq[I32] = Seq.tabulate(rank){i => stage(MemStep(x, i)) }
    @rig def ends(): Seq[I32] = Seq.tabulate(rank){i => stage(MemEnd(x, i)) }
    @rig def pars(): Seq[I32] = Seq.tabulate(rank){i => stage(MemPar(x, i)) }
    @rig def lens(): Seq[I32] = Seq.tabulate(rank){i => stage(MemLen(x, i)) }

    // TODO[2]: Units
    //def units(): Seq[Boolean] = metadata[AliasUnit](x).get.unit()
  }

}
