package nova.lang

import emul.FixedPoint
import forge.tags._
import nova.core._
import nova.node._

abstract class Fix[A](intBits: Int, fracBits: Int)(implicit ev: A<:<Fix[A]) extends Num[A] {
  private implicit lazy val tA: Fix[A] = this.tp.view(this)

  final override type I = FixedPoint
  final override def nBits: Int = intBits + fracBits
  def iBits: Int = intBits
  def fBits: Int = fracBits

  @api def unary_-(): A = stage(FixNeg(me))
  @api def unary_~(): A = stage(FixInv(me))
  @api def &(that: A): A = stage(FixAnd(me,that))
  @api def |(that: A): A = stage(FixOr(me,that))
  @api def ^(that: A): A = stage(FixXor(me,that))

  @api def +(that: A): A = {
    stage(FixAdd(me,that))
  }
  @api def -(that: A): A = stage(FixSub(me,that))
  @api def *(that: A): A = stage(FixMul(me,that))
  @api def /(that: A): A = stage(FixDiv(me,that))
  @api def %(that: A): A = stage(FixMod(me,that))
  @api def <<(that: A): A = stage(FixSLA(me,that))
  @api def >>(that: A): A = stage(FixSRA(me,that))
  @api def >>>(that: A): A = stage(FixSRU(me,that))

  @api def <(that: A): Bit = stage(FixLst(me,that))
  @api def <=(that: A): Bit = stage(FixLeq(me,that))
  @api override def !==(that: A): Bit = stage(FixNeq(me,that))
  @api override def ===(that: A): Bit = stage(FixEql(me,that))

  @rig def random(max: Option[A]): A = stage(FixRandom(max))

  @rig def min(a: A, b: A): A = stage(FixMin(a,b))
  @rig def max(a: A, b: A): A = stage(FixMax(a,b))

  @rig def abs(a: A): A = stage(FixAbs(a))
  @rig def ceil(a: A): A = stage(FixCeil(a))
  @rig def floor(a: A): A = stage(FixFloor(a))
  @rig def pow(b: A, e: A): A = stage(FixPow(b,e))
  @rig def exp(a: A): A = stage(FixExp(a))
  @rig def ln(a: A): A = stage(FixLn(a))
  @rig def sqrt(a: A): A = stage(FixSqrt(a))
  @rig def sin(a: A): A = stage(FixSin(a))
  @rig def cos(a: A): A = stage(FixCos(a))
  @rig def tan(a: A): A = stage(FixTan(a))
  @rig def sinh(a: A): A = stage(FixSinh(a))
  @rig def cosh(a: A): A = stage(FixCosh(a))
  @rig def tanh(a: A): A = stage(FixTanh(a))
  @rig def asin(a: A): A = stage(FixAsin(a))
  @rig def acos(a: A): A = stage(FixAcos(a))
  @rig def atan(a: A): A = stage(FixAtan(a))
  @rig def sigmoid(a: A): A = stage(FixSigmoid(a))
}

case class I32() extends Fix[I32](32, 0) {
  def ::(start: I32): Series = Series(start,this,I32.c(1),I32.c(1),isUnit = false)

  @api def par(p: I32): Series = Series(I32.c(0),this,I32.c(1),p,isUnit=false)
  @api def by(step: I32): Series = Series(I32.c(0),this,step,I32.c(1), isUnit = false)
  def until(end: I32): Series = Series(this, end, I32.c(1), I32.c(1), isUnit = false)

  @api def zero: I32 = I32.c(0)
  @api def one: I32 = I32.c(1)

  override def fresh: I32 = I32()
}
object I32 {
  implicit val tp: I32 = (new I32).asType
  def c(x: Int): I32 = const[I32](FixedPoint.fromInt(x))

  @api def p(x: Int): I32 = param[I32](FixedPoint.fromInt(x))
}


case class I16() extends Fix[I16](16, 0) {
  override def fresh: I16 = new I16

  @api def zero: I16 = I16.c(0)
  @api def one: I16 = I16.c(1)
}
object I16 {
  implicit val tp: I16 = (new I16).asType
  def c(x: Short): I16 = const[I16](FixedPoint.fromShort(x))
}



case class I8() extends Fix[I8](8, 0) {
  override def fresh: I8 = new I8

  @api def zero: I8 = I8.c(0)
  @api def one: I8 = I8.c(1)
}
object I8 {
  implicit val tp: I8 = (new I8).asType
  def c(x: Byte): I8 = const[I8](FixedPoint.fromByte(x))
}


