package pcc
package ir

import forge._

abstract class Flt[A](id: Int, sigBits: Int, expBits: Int)(implicit ev: A<:<Flt[A]) extends Num[A](id) {
  final override def bits: Int = sigBits + expBits
  private implicit val tA: Flt[A] = this
  def sBits: Int = sigBits
  def eBits: Int = expBits

  @api def unary_-(): A = Flt.neg(me)
  @api def +(that: A): A = Flt.add(me,that)
  @api def -(that: A): A = Flt.sub(me,that)
  @api def *(that: A): A = Flt.mul(me,that)
  @api def /(that: A): A = Flt.div(me,that)         

  @api def <(that: A): Bit = Flt.lst(me,that)
  @api def <=(that: A): Bit = Flt.leq(me,that)
  @api def >(that: A): Bit = Flt.lst(that,me)
  @api def >=(that: A): Bit = Flt.leq(that,me)
}
object Flt {
  @api def neg[A:Flt](a: A): A = stage(FltNeg(a))
  @api def add[A:Flt](a: A, b: A): A = stage(FltAdd(a,b))
  @api def sub[A:Flt](a: A, b: A): A = stage(FltSub(a,b))
  @api def mul[A:Flt](a: A, b: A): A = stage(FltMul(a,b))
  @api def div[A:Flt](a: A, b: A): A = stage(FltDiv(a,b))

  @api def lst[A:Flt](a: A, b: A): Bit = stage(FltLst(a,b))
  @api def leq[A:Flt](a: A, b: A): Bit = stage(FltLeq(a,b))
  @api def eql[A:Flt](a: A, b: A): Bit = stage(FltEql(a,b))
  @api def neq[A:Flt](a: A, b: A): Bit = stage(FltNeq(a,b))
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

/** Nodes **/

/** Floating point negation of a (-a) **/
case class FltNeg[T:Flt](a: T) extends Op[T] { def mirror(f:Tx) = Flt.neg(f(a))  }
/** Floating point addition of a and b (a + b) **/
case class FltAdd[T:Flt](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Flt.add(f(a),f(b)) }
/** Floating point subtraction of b from a (a - b) **/
case class FltSub[T:Flt](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Flt.sub(f(a),f(b)) }
/** Floating point multiplication of a and b (a * b) **/
case class FltMul[T:Flt](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Flt.mul(f(a),f(b)) }
/** Floating point division of a by b (a / b) **/
case class FltDiv[T:Flt](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Flt.div(f(a),f(b)) }

/** Floating point less than comparison of a and b (a < b) **/
case class FltLst[T:Flt](a: T, b: T) extends Op[Bit] { def mirror(f:Tx) = Flt.lst(f(a),f(b)) }
/** Floating point less than or equal comparison of a and b (a <= b) **/
case class FltLeq[T:Flt](a: T, b: T) extends Op[Bit] { def mirror(f:Tx) = Flt.leq(f(a),f(b)) }
/** Floating point equality comparison of a and b (a == b) **/
case class FltEql[T:Flt](a: T, b: T) extends Op[Bit] { def mirror(f:Tx) = Flt.eql(f(a),f(b)) }
/** Floating point inequality comparison of a and b (a != b) **/
case class FltNeq[T:Flt](a: T, b: T) extends Op[Bit] { def mirror(f:Tx) = Flt.neq(f(a),f(b)) }

