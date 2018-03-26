package spatial.lang
package types

import forge.tags._
import argon._

trait Num[A] extends Order[A] with Arith[A] with Bits[A] {
  // Fancy Scala trick: Promise evidence of A being a subclass of Num[A], use implicitly here
  // Fill in later using implicitly[A <:< Num[A]] on the concrete subclass
  val box: A <:< Num[A]
  private implicit val evv: A <:< Num[A] = box
  private implicit lazy val tA: Num[A] = this.selfType
  override protected val __isPrimitive: Boolean = true

  @rig def one: A = this.from(1)
  @rig def zero: A = this.from(0)

  @rig def pow(b: A, e: A): A
  @rig def exp(a: A): A
  @rig def ln(a: A): A
  @rig def sqrt(a: A): A
  @rig def sin(a: A): A
  @rig def cos(a: A): A
  @rig def tan(a: A): A
  @rig def sinh(a: A): A
  @rig def cosh(a: A): A
  @rig def tanh(a: A): A
  @rig def asin(a: A): A
  @rig def acos(a: A): A
  @rig def atan(a: A): A
  @rig def sigmoid(a: A): A
}
object Num {
  def apply[A:Num]: Num[A] = implicitly[Num[A]]
  def m[A,B](n: Num[A]): Num[B] = n.asInstanceOf[Num[B]]

  def unapply[A](x: Type[A]): Option[Num[A]] = x match {
    case b: Num[_] => Some(b.asInstanceOf[Num[A]])
    case _ => None
  }
}

