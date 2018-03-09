package core

import Freq._
import Filters._
import core.transform.Transformer
import forge.tags.rig
import utils.recursive

/** Any staged operation.
  *
  * @tparam R The staged return type of the operation
  *
  * NOTE: Op is NOT covariant with R - strange things happen with pattern matching if it is.
  */
abstract class Op[R:Type] extends Serializable with Product {
  final type Tx = Transformer
  val R: Type[R] = Type[R]

  def expInputs: Seq[Sym[_]] = recursive.collectSeqs(expsFunc)(productIterator)

  /** Scheduling dependencies -- used to calculate schedule for IR based on dependencies */
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
  def binds: Seq[Sym[_]] = blocks.flatMap(_.effects.antiDeps.map(_.sym))

  /** Alias hints -- used to check/disallow unsafe mutable aliasing */
  // Aliases: inputs to this Def which *may* equal to the output of this Def
  // E.g. y = if (cond) a else b: aliases should return a and b
  // Default: All inputs which have the same type as an output
  def aliases: Seq[Sym[_]] = inputs.collect{case s if s.tp =:= R => s}

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
  def copies: Seq[Sym[_]] = Nil

  /** Effects */
  def effects: Effects = blocks.map(_.effects).fold(Effects.Pure){(a,b) => a andAlso b }

  @rig def rewrite: R = null.asInstanceOf[R]
  def mirror(f:Tx): Op[R] = throw new Exception(s"Use @op annotation or override mirror method for node class $productPrefix")
  def update(f:Tx): Unit  = throw new Exception(s"Use @op annotation or override update method for node class $productPrefix")
}

object Op {
  def unapply[A](x: Exp[_,A]): Option[Op[A]] = x.op
}

object Stm {
  def unapply[A](x: Exp[_,A]): Option[(Sym[A],Op[A])] = Op.unapply(x).map{rhs => (x,rhs) }
}
