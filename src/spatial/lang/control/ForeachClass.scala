package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node._
import spatial.metadata.control._

class ForeachClass(opt: CtrlOpt) {
  @api def apply(ctr: Counter[I32])(func: I32 => Any): Void = {
    apply(Seq(ctr)){is => func(is.head) }
  }

  @api def apply(ctr0: Counter[I32], ctr1: Counter[I32])(func: (I32,I32) => Any): Void = {
    apply(Seq(ctr0,ctr1)){is => func(is(0),is(1)) }
  }

  @api def apply(ctr0: Counter[I32], ctr1: Counter[I32], ctr2: Counter[I32])(func: (I32,I32,I32) => Any): Void = {
    apply(Seq(ctr0,ctr1,ctr2)){is => func(is(0),is(1),is(2)) }
  }

  @api def apply(ctr0: Counter[I32], ctr1: Counter[I32], ctr2: Counter[I32], ctr3: Counter[I32], ctrs: Counter[I32]*)(func: Seq[I32] => Any): Void = {
    apply(Seq(ctr0,ctr1,ctr2,ctr3) ++ ctrs){is => func(is) }
  }

  @rig def apply(ctrs: Seq[Counter[I32]])(func: Seq[I32] => Any): Void = {
    val iters  = ctrs.map{_ => boundVar[I32] }
    val cchain = CounterChain(ctrs)
    cchain.counters.zip(iters).foreach{case (ctr, i) => i.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1){i => i}) }
    stageWithFlow(OpForeach(Set.empty, cchain, stageBlock{ func(iters); void }, iters, opt.stopWhen)){pipe =>
      opt.set(pipe)
    }
  }

}
