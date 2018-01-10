package pcc
package ir
package control

import forge._

object Foreach {
  @api def apply(ctr: Counter)(func: I32 => Void): Void = Foreach(Seq(ctr)){is => func(is.head) }

  @api def apply(ctr0: Counter, ctr1: Counter)(func: (I32,I32) => Void): Void = {
    Foreach(Seq(ctr0,ctr1)){is => func(is(0),is(1)) }
  }

  @api def apply(ctr0: Counter, ctr1: Counter, ctr2: Counter)(func: (I32,I32,I32) => Void): Void = {
    Foreach(Seq(ctr0,ctr1,ctr2)){is => func(is(0),is(1),is(2)) }
  }

  @api def apply(ctrs: Seq[Counter])(func: Seq[I32] => Void): Void = {
    val iters  = ctrs.map{_ => bound[I32] }
    val block  = () => func(iters)
    val cchain = CounterChain(ctrs:_*)
    Foreach.staged(Nil, cchain, block, iters)
  }

  @internal def staged(ens: Seq[Bit], cchain: CounterChain, func: () => Void, iters: Seq[I32]): Void = {
    stage(OpForeach(ens,cchain,stageBlock{func()},iters))
  }
}

case class OpForeach(
  ens:    Seq[Bit],
  cchain: CounterChain,
  block:  Block[Void],
  iters:  Seq[I32]
) extends Loop {
  override def inputs = syms(ens) ++ syms(cchain) ++ syms(block)
  override def binds = super.binds ++ iters

  def mirror(f: Tx) = Foreach.staged(f(ens),f(cchain),f.tx(block),f(iters))
}