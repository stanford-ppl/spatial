package pcc
package ir

import forge._

case class Bit(eid: Int) extends Bits[Bit](eid) {
  override type I = Boolean
  def bits = 1

  override def fresh(id: Int): Bit = Bit(id)
  override def stagedClass: Class[Bit] = classOf[Bit]

  @api def unary_!(): Bit = Bit.not(this)
  @api def &&(that: Bit): Bit = Bit.and(this,that)
  @api def ||(that: Bit): Bit = Bit.or(this,that)
}
object Bit {
  implicit val bit: Bit = Bit(-1)

  @api def not(a: Bit): Bit = stage(Not(a))
  @api def and(a: Bit, b: Bit): Bit = stage(And(a,b))
  @api def or(a: Bit, b: Bit): Bit = stage(Or(a,b))
}

/** Boolean NOT of a (!a) **/
case class Not(a: Bit) extends Op[Bit] { def mirror(f:Tx) = !f(a) }
/** Boolean AND of a and b (a && b) **/
case class And(a: Bit, b: Bit) extends Op[Bit] { def mirror(f:Tx) = f(a) && f(b) }
/** Boolean OR of a and b (a || b) **/
case class Or(a: Bit, b: Bit) extends Op[Bit] { def mirror(f:Tx) = f(a) || f(b) }