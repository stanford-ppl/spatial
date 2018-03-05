package spatial.lang
package control

import forge.tags._
import core._
import spatial.node._

object Foreach {
  @api def apply(ctr: Counter[I32])(func: I32 => Void): Void = {
    Foreach(Seq(ctr)){is => func(is.head) }
  }

  @api def apply(ctr0: Counter[I32], ctr1: Counter[I32])(func: (I32,I32) => Void): Void = {
    Foreach(Seq(ctr0,ctr1)){is => func(is(0),is(1)) }
  }

  @api def apply(ctr0: Counter[I32], ctr1: Counter[I32], ctr2: Counter[I32])(func: (I32,I32,I32) => Void): Void = {
    Foreach(Seq(ctr0,ctr1,ctr2)){is => func(is(0),is(1),is(2)) }
  }

  @api def apply(ctrs: Seq[Counter[I32]])(func: Seq[I32] => Void): Void = {
    val iters  = ctrs.map{_ => bound[I32] }
    val cchain = CounterChain(ctrs:_*)
    stage(OpForeach(cchain, stageBlock{ func(iters) }, iters, Set.empty))
  }
}

