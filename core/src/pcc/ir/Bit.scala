package pcc
package ir

case class Bit(eid: Int) extends Bits[Bit](eid) {
  override type I = Boolean
  def bits = 1

  override def fresh(id: Int): Bit = Bit(id)
  override def stagedClass: Class[Bit] = classOf[Bit]
}
object Bit {
  implicit val bit: Bit = Bit(-1)
}
