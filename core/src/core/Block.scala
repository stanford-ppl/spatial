package core

import Freq._

case class Block[R](
  inputs:  Seq[Sym[_]], // External inputs to this block
  stms:    Seq[Sym[_]], // All statements in this scope (linearized graph)
  result:  Sym[R],      // The symbolic result of this block
  effects: Effects,     // All external effects of this block
  options: BlockOptions // Other settings for this block
) {
  def temp: Freq = options.temp

  def nestedStms: Set[Sym[_]] = stms.flatMap{s =>
    s.op.map{o => o.blocks.flatMap(_.nestedStms) }.getOrElse(Nil)
  }.toSet

  def nestedInputs: Set[Sym[_]] = nestedStmsAndInputs._2

  def nestedStmsAndInputs: (Set[Sym[_]], Set[Sym[_]]) = {
    val stms = this.nestedStms
    val used = stms.flatMap(_.inputs) ++ inputs
    val made = stms.flatMap{s => s +: s.op.map(_.binds).getOrElse(Nil) }
    val ins  = (used diff made).filterNot(_.isValue)
    (stms, ins)
  }

  override def toString: String = {
    if (inputs.isEmpty) s"Block($result)" else s"""Block(${inputs.mkString("(", ",", ")")} => $result)"""
  }
}
