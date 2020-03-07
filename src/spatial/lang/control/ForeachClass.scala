package spatial.lang
package control

import argon._
import forge.tags._
import spatial.node._
import spatial.metadata.control._

class ForeachClass(opt: CtrlOpt) {
  @api def apply(ctr: Counter[ICTR])(func: ICTR => Any): Void = {
    apply(Seq(ctr)){is => func(is.head) }
  }

  @api def apply(ctr0: Counter[ICTR], ctr1: Counter[ICTR])(func: (ICTR,ICTR) => Any): Void = {
    apply(Seq(ctr0,ctr1)){is => func(is(0),is(1)) }
  }

  @api def apply(ctr0: Counter[ICTR], ctr1: Counter[ICTR], ctr2: Counter[ICTR])(func: (ICTR,ICTR,ICTR) => Any): Void = {
    apply(Seq(ctr0,ctr1,ctr2)){is => func(is(0),is(1),is(2)) }
  }

  @api def apply(ctr0: Counter[ICTR], ctr1: Counter[ICTR], ctr2: Counter[ICTR], ctr3: Counter[ICTR], ctrs: Counter[ICTR]*)(func: Seq[ICTR] => Any): Void = {
    apply(Seq(ctr0,ctr1,ctr2,ctr3) ++ ctrs){is => func(is) }
  }

  @rig def apply(ctrs: Seq[Counter[ICTR]])(func: Seq[ICTR] => Any): Void = {
    val iters  = ctrs.map{_ => boundVar[ICTR] }
    val cchain = CounterChain(ctrs)
    cchain.counters.zip(iters).foreach{case (ctr, i) => i.counter = IndexCounterInfo(ctr, Seq.tabulate(ctr.ctrParOr1){i => i}) }
    stageWithFlow(OpForeach(Set.empty, cchain, stageBlock{ func(iters); void }, iters, opt.stopWhen)){pipe =>
      opt.set(pipe)
    }
  }

}
