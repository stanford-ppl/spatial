package pcc
package ir

import forge._

object Foreach {
  @api def apply(ctr: Counter)(func: I32 => Void): Void = Foreach(Seq(ctr)){ctr => func(ctr.head) }

  @api def apply(ctrs: Seq[Counter])(func: Seq[I32] => Void): Void = {
    val iters  = ctrs.map{_ => bound[I32] }
    val block  = stageBlock{ func(iters) }
    val cchain = stage(CounterChainAlloc(ctrs))
    stage(OpForeach(Nil,cchain,block,iters))
  }
}

case class OpForeach(
  ens:    Seq[Bit],
  cchain: CounterChain,
  block:  Block[Void],
  iters:  Seq[I32]
) extends Op[Void] {
  override def inputs = syms(ens) ++ syms(cchain) ++ syms(block)
  override def binds  = super.binds ++ iters
}