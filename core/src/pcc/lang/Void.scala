package pcc.lang

import forge._
import pcc.core._

case class Void() extends Bits[Void]() {
  override type I = Unit
  def nBits = 0

  override def fresh: Void = new Void

  @rig def zero: Void = Void.c
  @rig def one: Void = Void.c
  @rig def random(max: Option[Void]): Void = Void.c

  @api override def !==(that: Void): Bit = false
  @api override def ===(that: Void): Bit = true
}
object Void {
  implicit val tp: Void = (new Void).asType
  def c: Void = const[Void](())
}
