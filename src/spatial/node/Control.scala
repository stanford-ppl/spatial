package spatial.node

import forge.tags._
import core._
import spatial.lang._

@op case class CounterNew[A:Num](start: Num[A], end: Num[A], step: Num[A], par: I32) extends Alloc[Counter[A]] {
  val nA: Num[A] = Num[A]
}
@op case class CounterChainNew(counters: Seq[Counter[_]]) extends Alloc[CounterChain]

@op case class AccelScope(block: Block[Void]) extends Pipeline[Void] {
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
  override var ens: Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = mirror(f)
}

@op case class UnitPipe(block: Block[Void], ens: Set[Bit]) extends Pipeline[Void] {
  override def iters = Nil
  override def bodies = Seq(Nil -> Seq(block))
  override def cchains = Nil
}

@op case class OpForeach(
  cchain: CounterChain,
  block:  Block[Void],
  iters:  Seq[I32],
  ens:    Set[Bit]
) extends Loop[Void] {
  override def inputs = syms(ens) ++ syms(cchain) ++ syms(block)
  override def binds = super.binds ++ iters

  def cchains = Seq(cchain -> iters)
  def bodies = Seq(iters -> Seq(block))
}
