package spatial.lang

import emul.Bool
import forge.tags._
import core._
import spatial.node._

case class Bit() extends Bits[Bit] {
  override type I = Bool
  def nBits = 1

  override def fresh: Bit = new Bit

  @rig def zero: Bit = Bit.c(false)
  @rig def one: Bit = Bit.c(true)
  @rig def random(max: Option[Bit]): Bit = stage(BitRandom(max))

  @api def unary_!(): Bit = stage(Not(me))
  @api def &(that: Bit): Bit = stage(And(this,that))
  @api def &&(that: Bit): Bit = this & that
  @api def |(that: Bit): Bit = stage(Or(this,that))
  @api def ||(that: Bit): Bit = this | that
  @api def ^(that: Bit): Bit = stage(Xor(this,that))

  @api override def !==(that: Bit): Bit = stage(Xor(this,that))
  @api override def ===(that: Bit): Bit = stage(Xnor(this,that))
}
object Bit {
  implicit val tp: Bit = (new Bit).asType
  def c(b: Boolean): Bit = const[Bit](Bool(b))
}
