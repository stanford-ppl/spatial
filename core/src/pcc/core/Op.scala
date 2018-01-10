package pcc.core

import pcc.data.Effects
import pcc.util.recursive
import pcc.util.Freq._
import Filters._

abstract class Op[T:Sym] extends Product with Serializable {
  type Tx = pcc.traversal.Transformer
  protected implicit var ctx: SrcCtx = SrcCtx.empty
  protected implicit var state: State = _
  def setCtx(c: SrcCtx): Unit = { ctx = c }

  def tR: Sym[T] = implicitly[Sym[T]]

  /** Scheduling dependencies -- used to calculate schedule for IR based on dependencies **/
  // Inputs: symbol dataflow dependencies for this Def.
  // Default: All symbols in the Def's case class constructor AND scope (block/lambda) results
  def inputs: Seq[Sym[_]] = recursive.collectSeqs(symsFunc)(productIterator)

  // Reads: symbols *dereferenced* by this Def.
  // Default: All symbol inputs
  def reads: Seq[Sym[_]] = inputs

  // Freqs: symbol frequency hints used in code motion - frequency is either Freq.Hot, Freq.Cold, or Freq.Normal
  // Code motion makes an attempt to schedule unbound "hot" symbols early (move out of blocks)
  // Default: All symbol inputs have a frequency of Freq.Normal, block dependencies depend on Block temp
  def freqs: Seq[(Sym[_],Freq)] = blocks.flatMap{blk => syms(blk).map(_ -> blk.temp)}

  // Scopes: scopes associated with this Def
  // Default: All blocks and lambdas in the Def's case class constructor
  def blocks: Seq[Block[_]] = recursive.collectSeq{case b: Block[_] => b}(productIterator)

  // Binds: symbols "bound" by this Def
  // Bound symbols define the start of scopes. Effectful symbols in a scope typically must be bound.
  // All dependents of bound syms up until but not including the binding Def make up the majority of a scope
  // NOTE: Tempting to use productIterator here too, but note that Bound values can be inputs
  // Default: All effects included in all scopes associated with this Def
  def binds: Seq[Sym[_]] = blocks.flatMap(_.impure)

  /** Alias hints -- used to check/disallow unsafe mutable aliasing **/
  // Aliases: inputs to this Def which *may* equal to the output of this Def
  // E.g. y = if (cond) a else b: aliases should return a and b
  // Default: All inputs which have the same type as an output
  def aliases: Seq[Sym[_]] = inputs.collect{case s if s <:> tR => s}

  // Contains: inputs which may be returned when dereferencing the output of this Def
  // E.g. y = Array(x): contains should return x
  // Default: no symbols
  def contains: Seq[Sym[_]] = Nil

  // Extracts: inputs which, when dereferenced, may return the output of this Def
  // E.g. y = ArrayApply(x): extracts should return x
  // Default: no symbols
  def extracts: Seq[Sym[_]] = Nil

  // Copies: inputs which, when dereferenced, may return the same pointer as dereferencing the output of this Def
  // E.g. y = ArrayCopy(x): copies should return x
  // Default: no symbols
  // TODO: In LMS, why is this different default than aliases?
  def copies: Seq[Sym[_]] = Nil

  /** Effects **/
  def effects: Effects = blocks.map(_.effects).fold(Effects.Pure){(a,b) => a andAlso b }

  def mirror(f: Tx): T

  final def mirrorNode(f: Tx)(implicit state: State): T = {
    this.state = state
    mirror(f)
  }
}

object Op {
  def unapply[A](x: Sym[A]): Option[Op[A]] = x.op
}

object Stm {
  def unapply[A](x: Sym[A]): Option[(Sym[A],Op[A])] = Op.unapply(x).map{rhs => (x,rhs) }
}