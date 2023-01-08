package argon

import argon.lang.Bit
import forge.tags._

/** A binary operation; takes two inputs of type R and returns a value of type R. */
trait Comparison[C,R<:Exp[C,R]] extends Op[Bit] {
  assert(this.productIterator.length == 2, s"Binary op $productPrefix has ${this.productIterator.length} != 2 inputs!")
  def a: Exp[C,R] // First input
  def b: Exp[C,R] // Second input

  /** An optional reference to an unstaged implementation of this op. */
  def unstaged: (C, C) => emul.Bool = null

  @rig override def rewrite: Bit = (a,b) match {
    case (Const(a),Const(b)) if unstaged != null => R.from(unstaged(a,b))
    case _ => super.rewrite
  }
}
