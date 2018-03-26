package spatial.lang
package types

import argon._
import forge.tags._

trait Arith[A] extends Top[A] with Ref[Any,A] {
  val box: A <:< Arith[A]
  private implicit val evv: A <:< Arith[A] = box
  private implicit lazy val tA: Arith[A] = this.selfType

  // --- Infix Methods
  @api def unary_-(): A
  @api def +(b: A): A
  @api def -(b: A): A
  @api def *(b: A): A
  @api def /(b: A): A
  @api def %(b: A): A

  // --- Typeclass Methods
  @rig def neg(a: A): A = -a
  @rig def add(a: A, b: A): A = a + b
  @rig def sub(a: A, b: A): A = a - b
  @rig def mul(a: A, b: A): A = a * b
  @rig def div(a: A, b: A): A = a / b
  @rig def mod(a: A, b: A): A = a % b

  @rig def abs(a: A): A
  @rig def ceil(a: A): A
  @rig def floor(a: A): A
}
object Arith {
  def apply[A:Arith]: Arith[A] = implicitly[Arith[A]]
  def m[A,B](n: Arith[A]): Arith[B] = n.asInstanceOf[Arith[B]]

  def unapply[A](x: Type[A]): Option[Arith[A]] = x match {
    case b: Arith[_] => Some(b.asInstanceOf[Arith[A]])
    case _ => None
  }
}
