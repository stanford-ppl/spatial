package spatial.node

import forge.tags._
import core._
import spatial.lang._

@op case class CounterNew(start: I32, end: I32, step: I32, par: I32) extends Alloc[Counter]
@op case class CounterChainNew(counters: Seq[Counter]) extends Alloc[CounterChain]

@op case class AccelScope(block: Block[Void]) extends Pipeline[Void] {
  def ens: Seq[Bit] = Nil
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
}

@op case class UnitPipe(ens: Seq[Bit], block: Block[Void]) extends Pipeline[Void] {
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
}

@op case class OpForeach(
  ens:    Seq[Bit],
  cchain: CounterChain,
  block:  Block[Void],
  iters:  Seq[I32]
) extends Loop[Void] {
  override def inputs = syms(ens) ++ syms(cchain) ++ syms(block)
  override def binds = super.binds ++ iters

  def cchains = Seq(cchain -> iters)
  def bodies = Seq(iters -> Seq(block))
}
