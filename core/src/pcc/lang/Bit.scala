package pcc.lang

import forge._
import pcc.core._
import pcc.node._

case class Bit() extends Bits[Bit] {
  override type I = Boolean
  def bits = 1

  override def fresh: Bit = new Bit

  @api def zero: Bit = Bit.c(false)
  @api def one: Bit = Bit.c(true)

  @api def unary_!(): Bit = stage(Not(me))
  @api def &(that: Bit): Bit = stage(And(this,that))
  @api def &&(that: Bit): Bit = this & that
  @api def |(that: Bit): Bit = stage(Or(this,that))
  @api def ||(that: Bit): Bit = this | that
}
object Bit {
  implicit val tp: Bit = (new Bit).asType
  def c(b: Boolean): Bit = const[Bit](b)
}
