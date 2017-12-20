package pcc
package ir

import forge._

abstract class Flt[A](id: Int, sigBits: Int, expBits: Int)(implicit ev: A<:<Flt[A]) extends Num[A](id) {
  final override def bits: Int = sigBits + expBits
  def sBits: Int = sigBits
  def eBits: Int = expBits
}

case class F32(eid: Int) extends Flt[F32](eid, 24, 8) {
  override type I = Float

  override def fresh(id: Int): F32 = F32(id)
  override def stagedClass: Class[F32] = classOf[F32]
}
object F32 {
  implicit val f32: F32 = F32(-1)
}


case class F16(eid: Int) extends Flt[F16](eid, 11, 5) {
  override type I = Float // TODO

  override def fresh(id: Int): F16 = F16(id)
  override def stagedClass: Class[F16] = classOf[F16]
}
object F16 {
  implicit val f16: F16 = F16(-1)
}
