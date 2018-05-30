package spatial.node

import argon._
import forge.tags._

import spatial.lang._

@op case class IfThenElse[T:Type](cond: Bit, thenBlk: Block[T], elseBlk: Block[T]) extends Control[T] {

  override def aliases: Set[Sym[_]] = syms(thenBlk.result, elseBlk.result)

  override def iters = Nil
  override def cchains = Nil
  override def bodies = Seq(PseudoStage(Nil, Seq(thenBlk,elseBlk)))
}
