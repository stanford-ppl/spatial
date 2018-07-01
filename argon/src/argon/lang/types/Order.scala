package argon.lang.types

import argon._
import argon.lang.api._
import forge.tags._

trait Order[A] extends Top[A] with Ref[Any,A] {
  def box: A <:< Order[A]
  private implicit def evv: A <:< Order[A] = box
  private implicit def A: Order[A] = this.selfType

  // --- Infix Methods
  @api def <(b: A): Bit
  @api def <=(b: A): Bit
  @api def >(b: A): Bit = b < me
  @api def >=(b: A): Bit = b <= me

  // --- Typeclass Methods
  @api def lt(a: A, b: A): Bit = a < b
  @api def leq(a: A, b: A): Bit = a <= b
  @api def gt(a: A, b: A): Bit = a > b
  @api def geq(a: A, b: A): Bit = a >= b

  @rig def min(a: A, b: A): A
  @rig def max(a: A, b: A): A
}
object Order {
  def apply[A:Order]: Order[A] = implicitly[Order[A]]
  def m[A,B](n: Order[A]): Order[B] = n.asInstanceOf[Order[B]]

  def unapply[A](x: Type[A]): Option[Order[A]] = x match {
    case b: Order[_] => Some(b.asInstanceOf[Order[A]])
    case _ => None
  }
}