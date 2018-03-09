package core

import forge.tags._

/** A binary operation; takes two inputs of type R and returns a value of type R. */
trait Binary[C,R<:Exp[C,R]] extends Op[R] {
  private implicit val tp: Type[R] = R
  assert(this.productIterator.length == 2, s"Binary op $productPrefix has ${this.productIterator.length} != 2 inputs!")
  def a: Exp[C,R] // First input
  def b: Exp[C,R] // Second input

  /** The value which, when combined with another value using this operation,
    * always returns this value.
    */
  def absorber: Option[R] = None

  /** The value which, when combined with another value using this operation,
    * always returns the other value.
    */
  def identity: Option[R] = None

  /** Returns true if `x` is an absorber for this operation. */
  def isAbsorber(x: R): Boolean = absorber.contains(x)

  /** Returns true if `x` is an identity for this operation. */
  def isIdentity(x: R): Boolean = identity.contains(x)

  /** True if this operation is associative. */
  def isAssociative: Boolean = false

  /** An optional reference to an unstaged implementation of this op. */
  def unstaged: (C, C) => C = null

  @rig override def rewrite: R = (a,b) match {
    case (Const(a),Const(b)) if unstaged != null => R.from(unstaged(a,b))
    case (_, b) if isAbsorber(b.unbox) => b.unbox
    case (a, _) if isAbsorber(a.unbox) => a.unbox
    case (a, b) if isIdentity(a.unbox) => b.unbox
    case (a, b) if isIdentity(b.unbox) => a.unbox
    case _ => super.rewrite
  }
}
