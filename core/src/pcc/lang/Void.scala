package pcc.lang

import forge._
import pcc.core._

case class Void(eid: Int) extends Bits[Void](eid) {
  override type I = Unit
  def bits = 0

  override def fresh(id: Int): Void = Void(id)
  override def stagedClass: Class[Void] = classOf[Void]

  @api def zero: Void = Void.c
  @api def one: Void = Void.c
}
object Void {
  implicit val void: Void = Void(-1)
  @api def c: Void = const[Void](())
}
