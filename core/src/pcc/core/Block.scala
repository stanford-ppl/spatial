package pcc.core

import pcc.util.Freq._
import pcc.data.Effects

case class Block[R](
  inputs:  Seq[Sym[_]], // External inputs to this block
  stms:    Seq[Sym[_]], // All statements in this scope (linearized graph)
  result:  Sym[R],      // The symbolic result of this block
  effects: Effects,     // All external effects of this block
  impure:  Seq[Sym[_]], // Symbols with effects in this block
  options: BlockOptions // Other settings for this block
) {
  def temp: Freq = options.temp

  override def toString: String = {
    if (inputs.isEmpty) s"Block($result)" else s"""Block(${inputs.mkString("(", ",", ")")} => $result)"""
  }

  // TODO: Use this if large blocks start to become a memory issue
  // (recomputes impure statements in the block each time)
  //def impure: Seq[Sym[_]] = stms.collect{case e@Effectful(eff,_) if !eff.isPure => e }
}
