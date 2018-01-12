package pcc.lang

import forge._
import pcc.core._
import pcc.node._

case class Bit(eid: Int) extends Bits[Bit](eid) {
  override type I = Boolean
  def bits = 1

  override def fresh(id: Int): Bit = Bit(id)
  override def stagedClass: Class[Bit] = classOf[Bit]

  @api def zero: Bit = Bit.c(false)
  @api def one: Bit = Bit.c(true)

  @api def unary_!(): Bit = stage(Not(me))
  @api def &&(that: Bit): Bit = stage(And(this,that))
  @api def ||(that: Bit): Bit = stage(Or(this,that))
}
object Bit {
  implicit val tp: Bit = Bit(-1)
  @api def c(b: Boolean): Bit = const[Bit](b)
}
