package pcc.lang

import forge._
import pcc.core._
import pcc.node._

abstract class Flt[A](id: Int, sigBits: Int, expBits: Int)(implicit ev: A<:<Flt[A]) extends Num[A](id) {
  final override def bits: Int = sigBits + expBits
  private implicit val tA: Flt[A] = this
  def sBits: Int = sigBits
  def eBits: Int = expBits

  @api def unary_-(): A = stage(FltNeg(me))
  @api def +(that: A): A = stage(FltAdd(me,that))
  @api def -(that: A): A = stage(FltSub(me,that))
  @api def *(that: A): A = stage(FltMul(me,that))
  @api def /(that: A): A = stage(FltDiv(me,that))

  @api def <(that: A): Bit = stage(FltLst(me,that))
  @api def <=(that: A): Bit = stage(FltLeq(me,that))
  @api def >(that: A): Bit = stage(FltLst(that,me))
  @api def >=(that: A): Bit = stage(FltLeq(that,me))
}

case class F32(eid: Int) extends Flt[F32](eid, 24, 8) {
  override type I = Float

  override def fresh(id: Int): F32 = F32(id)
  override def stagedClass: Class[F32] = classOf[F32]

  @api def zero: F32 = F32.c(0)
  @api def one: F32 = F32.c(1)
}
object F32 {
  implicit val tp: F32 = F32(-1)
  @api def c(x: Float): F32 = const[F32](x)
}


case class F16(eid: Int) extends Flt[F16](eid, 11, 5) {
  override type I = Float // TODO

  override def fresh(id: Int): F16 = F16(id)
  override def stagedClass: Class[F16] = classOf[F16]

  @api def zero: F16 = F16.c(0)
  @api def one: F16 = F16.c(0)
}
object F16 {
  implicit val tp: F16 = F16(-1)
  @api def c(x: Float): F16 = const[F16](x)
}
