package spatial.node

import core._
import forge.tags._

import spatial.lang._

@op case class SwitchCase[R:Type](body: Block[R]) extends Control[R] {
  def iters = Nil
  def cchains = Nil
  def bodies = Seq(Nil -> Seq(body))
}

@op case class Switch[R:Type](selects: Seq[Bit], body: Block[R]) extends Control[R] {
  def iters = Nil
  def cchains = Nil
  def bodies = Seq(Nil -> Seq(body))

  override def inputs = syms(selects) ++ syms(body)
}
