package pcc.lang

import forge._
import pcc.core._

case class Void() extends Bits[Void]() {
  override type I = Unit
  def bits = 0

  override def fresh: Void = new Void

  @api def zero: Void = Void.c
  @api def one: Void = Void.c
}
object Void {
  implicit val tp: Void = (new Void).asType
  def c: Void = const[Void](())
}
