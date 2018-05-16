package spatial.lang
package types

import forge.tags._
import argon._

trait Num[A] extends Order[A] with Arith[A] with Bits[A] {
  // Fancy Scala trick: Promise evidence of A being a subclass of Num[A], use implicitly here
  // Fill in later using implicitly[A <:< Num[A]] on the concrete subclass
  def box: A <:< Num[A]
  private implicit def evv: A <:< Num[A] = box
  private implicit def A: Num[A] = this.selfType
  override protected val __neverMutable: Boolean = true

  @api def **(e: A): A = pow(me, e)

  @api def ::(start: A): Series[A]  = Series[A](start, me, 1, 1, isUnit = false)
  @api def par(p: I32): Series[A]   = Series[A](zero, me, one, p, isUnit = false)
  @api def by(step: A): Series[A]   = Series[A](zero, me, step, 1, isUnit = false)
  @api def until(end: A): Series[A] = Series[A](me, end, one, 1, isUnit = false)

  @api def toSeries: Series[A] = Series[A](me, me+1, 1, 1, isUnit=true)


  // --- Typeclass Methods
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

  @rig def __toFix[S:BOOL,I:INT,F:INT]: Fix[S,I,F]
  @rig def __toFlt[M:INT,E:INT]: Flt[M,E]
}
object Num {
  def apply[A:Num]: Num[A] = implicitly[Num[A]]
  def m[A,B](n: Num[A]): Num[B] = n.asInstanceOf[Num[B]]

  def unapply[A](x: Type[A]): Option[Num[A]] = x match {
    case b: Num[_] => Some(b.asInstanceOf[Num[A]])
    case _ => None
  }
}

