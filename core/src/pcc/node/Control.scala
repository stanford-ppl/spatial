package pcc.node

import forge.op
import pcc.core._
import pcc.lang._
import pcc.traversal.transform.Transformer

@op case class CounterNew(start: I32, end: I32, step: I32, par: I32) extends Alloc[Counter]
@op case class CounterChainNew(counters: Seq[Counter]) extends Alloc[CounterChain]

@op case class AccelScope(block: Block[Void]) extends Pipeline {
  def ens: Seq[Bit] = Nil
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
}

@op case class UnitPipe(ens: Seq[Bit], block: Block[Void]) extends Pipeline {
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
}

@op case class OpForeach(
  ens:    Seq[Bit],
  cchain: CounterChain,
  block:  Block[Void],
  iters:  Seq[I32]
) extends Loop {
  override def inputs = syms(ens) ++ syms(cchain) ++ syms(block)
  override def binds = super.binds ++ iters

  def cchains = Seq(cchain -> iters)
  def bodies = Seq(iters -> Seq(block))
}
