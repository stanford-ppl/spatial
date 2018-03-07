package core

import forge.tags._

/** A unary operation. Takes one input of type R and returns value of type R. **/
trait Unary[C,R<:Exp[C,R]] extends Op[R] {
  assert(this.productIterator.length == 1, s"Unary op $productPrefix has ${productIterator.length} != 1 inputs!")
  def a: Exp[C,R] // Input

  /** An optional reference to an unstaged implementation of this op. **/
  def unstaged: C => C = null

  @rig override def rewrite: R = a match {
    case Const(ac) if unstaged != null => R.from(unstaged(ac))
    case _ => super.rewrite
  }
}
