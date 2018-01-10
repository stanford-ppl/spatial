package pcc
package ir

import forge._

abstract class Fix[A](id: Int, intBits: Int, fracBits: Int)(implicit ev: A<:<Fix[A]) extends Num[A](id) {
  final override def bits: Int = intBits + fracBits
  private implicit val tA: Fix[A] = this
  def iBits: Int = intBits
  def fBits: Int = fracBits

  @api def unary_-(): A = Fix.neg(me)
  @api def +(that: A): A = Fix.add(me,that)
  @api def -(that: A): A = Fix.sub(me,that)
  @api def *(that: A): A = Fix.mul(me,that)
  @api def /(that: A): A = Fix.div(me,that)
  @api def %(that: A): A = Fix.mod(me,that)

  @api def <(that: A): Bit = Fix.lst(me,that)
  @api def <=(that: A): Bit = Fix.leq(me,that)
  @api def >(that: A): Bit = Fix.lst(that,me)
  @api def >=(that: A): Bit = Fix.leq(that,me)

  @api def <<(that: A): A = Fix.sla(me,that)
  @api def >>(that: A): A = Fix.sra(me,that)
}
object Fix {
  // TODO: Shortcircuit versions
  @api def neg[A:Fix](a: A): A = stage(FixNeg(a))
  @api def add[A:Fix](a: A, b: A): A = stage(FixAdd(a,b))
  @api def sub[A:Fix](a: A, b: A): A = stage(FixSub(a,b))
  @api def mul[A:Fix](a: A, b: A): A = stage(FixMul(a,b))
  @api def div[A:Fix](a: A, b: A): A = stage(FixDiv(a,b))
  @api def mod[A:Fix](a: A, b: A): A = stage(FixMod(a,b))
  @api def lst[A:Fix](a: A, b: A): Bit = stage(FixLst(a,b))
  @api def leq[A:Fix](a: A, b: A): Bit = stage(FixLeq(a,b))
  @api def eql[A:Fix](a: A, b: A): Bit = stage(FixEql(a,b))
  @api def neq[A:Fix](a: A, b: A): Bit = stage(FixNeq(a,b))
  @api def sla[A:Fix](a: A, b: A): A = stage(FixSLA(a,b))
  @api def sra[A:Fix](a: A, b: A): A = stage(FixSRA(a,b))

  @api def sig[A:Fix](a: A): A = stage(FixSig(a))
  @api def exp[A:Fix](a: A): A = stage(FixExp(a))
  @api def log[A:Fix](a: A): A = stage(FixLog(a))
  @api def sqt[A:Fix](a: A): A = stage(FixSqt(a))
  @api def abs[A:Fix](a: A): A = stage(FixAbs(a))
}

case class I32(eid: Int) extends Fix[I32](eid, 32, 0) {
  override type I = Int

  def ::(start: I32): Series = Series(start,this,None,None,isUnit = false)

  @api def par(p: I32): Series = Series(I32.c(0),this,None,Some(p),isUnit=false)
  @api def by(step: I32): Series = Series(I32.c(0),this,Some(step),None, isUnit = false)
  def until(end: I32): Series = Series(this, end, None, None, isUnit = false)

  override def fresh(id: Int): I32 = I32(id)
  override def stagedClass: Class[I32] = classOf[I32]
}
object I32 {
  implicit val i32: I32 = I32(-1)

  @api def c(x: Int): I32 = const[I32](x)
  @api def p(x: Int): I32 = param[I32](x)
}


case class I16(eid: Int) extends Fix[I16](eid, 16, 0) {
  override type I = Short

  override def fresh(id: Int): I16 = I16(id)
  override def stagedClass: Class[I16] = classOf[I16]
}
object I16 {
  implicit val i16: I16 = I16(-1)
}



case class I8(eid: Int) extends Fix[I8](eid, 8, 0) {
  override type I = Byte

  override def fresh(id: Int): I8 = I8(id)
  override def stagedClass: Class[I8] = classOf[I8]
}
object I8 {
  implicit val i8: I8 = I8(-1)
}


/** Nodes **/

/** Fixed point negation of a **/
case class FixNeg[T:Fix](a: T) extends Op[T] { def mirror(f:Tx) = Fix.neg(f(a)) }
/** Fixed point addition of a and b**/
case class FixAdd[T:Fix](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Fix.add(f(a),f(b)) }
/** Fixed point subtraction of b from a **/
case class FixSub[T:Fix](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Fix.sub(f(a),f(b)) }
/** Fixed point multiplication of a and b **/
case class FixMul[T:Fix](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Fix.mul(f(a),f(b)) }
/** Fixed point division of a by b **/
case class FixDiv[T:Fix](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Fix.div(f(a),f(b)) }
/** Fixed point modulus of a by b **/
case class FixMod[T:Fix](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Fix.mod(f(a),f(b)) }
/** Fixed point less than comparison between a and b (a < b) **/
case class FixLst[T:Fix](a: T, b: T) extends Op[Bit] { def mirror(f:Tx) = Fix.lst(f(a),f(b)) }
/** Fixed point less than or equal to comparison between a and b (a <= b) **/
case class FixLeq[T:Fix](a: T, b: T) extends Op[Bit] { def mirror(f:Tx) = Fix.leq(f(a),f(b)) }
/** Fixed point equality comparison between a and b (a == b) **/
case class FixEql[T:Fix](a: T, b: T) extends Op[Bit] { def mirror(f:Tx) = Fix.eql(f(a),f(b)) }
/** Fixed point inequality comparison between a and b (a != b) **/
case class FixNeq[T:Fix](a: T, b: T) extends Op[Bit] { def mirror(f:Tx) = Fix.neq(f(a),f(b)) }
/** Fixed point Shift Left Arithmetic (a << b) **/
case class FixSLA[T:Fix](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Fix.sla(f(a),f(b)) }
/** Fixed point Shift Right Arithmetic (a >> b) **/
case class FixSRA[T:Fix](a: T, b: T) extends Op[T] { def mirror(f:Tx) = Fix.sra(f(a),f(b)) }
