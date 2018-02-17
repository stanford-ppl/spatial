package nova.lang

import emul.FloatPoint
import forge.tags._
import nova.core._
import nova.node._

abstract class Flt[A](sigBits: Int, expBits: Int)(implicit ev: A<:<Flt[A]) extends Num[A] {
  private implicit lazy val tA: Flt[A] = this.tp.view(this)

  final override type I = FloatPoint
  final override def nBits: Int = sigBits + expBits
  def sBits: Int = sigBits
  def eBits: Int = expBits

  @api def unary_-(): A = stage(FltNeg(me))
  @api def +(that: A): A = stage(FltAdd(me,that))
  @api def -(that: A): A = stage(FltSub(me,that))
  @api def *(that: A): A = stage(FltMul(me,that))
  @api def /(that: A): A = stage(FltDiv(me,that))
  @api def %(that: A): A = stage(FltMod(me,that))

  @api def <(that: A): Bit = stage(FltLst(me,that))
  @api def <=(that: A): Bit = stage(FltLeq(me,that))
  @api override def !==(that: A): Bit = stage(FltNeq(that,me))
  @api override def ===(that: A): Bit = stage(FltEql(that,me))

  @rig def random(max: Option[A]): A = stage(FltRandom(max))

  @rig def min(a: A, b: A): A = stage(FltMin(a,b))
  @rig def max(a: A, b: A): A = stage(FltMax(a,b))

  @rig def abs(a: A): A = stage(FltAbs(a))
  @rig def ceil(a: A): A = stage(FltCeil(a))
  @rig def floor(a: A): A = stage(FltFloor(a))
  @rig def pow(b: A, e: A): A = stage(FltPow(b,e))
  @rig def exp(a: A): A = stage(FltExp(a))
  @rig def ln(a: A): A = stage(FltLn(a))
  @rig def sqrt(a: A): A = stage(FltSqrt(a))
  @rig def sin(a: A): A = stage(FltSin(a))
  @rig def cos(a: A): A = stage(FltCos(a))
  @rig def tan(a: A): A = stage(FltTan(a))
  @rig def sinh(a: A): A = stage(FltSinh(a))
  @rig def cosh(a: A): A = stage(FltCosh(a))
  @rig def tanh(a: A): A = stage(FltTanh(a))
  @rig def asin(a: A): A = stage(FltAsin(a))
  @rig def acos(a: A): A = stage(FltAcos(a))
  @rig def atan(a: A): A = stage(FltAtan(a))
  @rig def sigmoid(a: A): A = stage(FltSigmoid(a))

}

case class F32() extends Flt[F32](24, 8) {
  override def fresh: F32 = new F32

  @rig def zero: F32 = F32.c(0)
  @rig def one: F32 = F32.c(1)
}
object F32 {
  implicit val tp: F32 = (new F32).asType
  def c(x: Float): F32 = const[F32](FloatPoint.fromFloat(x))
}


case class F16() extends Flt[F16](11, 5) {
  override def fresh: F16 = new F16

  @rig def zero: F16 = F16.c(0)
  @rig def one: F16 = F16.c(0)
}
object F16 {
  implicit val tp: F16 = (new F16).asType
  def c(x: Float): F16 = const[F16](FloatPoint.fromHalf(x))
}
