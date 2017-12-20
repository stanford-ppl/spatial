package pcc
package ir

import forge._

case class Void(eid: Int) extends Bits[Void](eid) {
  override type I = Unit
  def bits = 0

  override def fresh(id: Int): Void = Void(id)
  override def stagedClass: Class[Void] = classOf[Void]
}
object Void {
  implicit val void: Void = Void(-1)
}
