package pcc.lang

import forge._
import pcc.core._
import pcc.node._

abstract class Fix[A](id: Int, intBits: Int, fracBits: Int)(implicit ev: A<:<Fix[A]) extends Num[A](id) {
  final override def bits: Int = intBits + fracBits
  private implicit val tA: Fix[A] = this
  def iBits: Int = intBits
  def fBits: Int = fracBits

  @api def unary_-(): A = stage(FixNeg(me))
  @api def +(that: A): A = stage(FixAdd(me,that))
  @api def -(that: A): A = stage(FixSub(me,that))
  @api def *(that: A): A = stage(FixMul(me,that))
  @api def /(that: A): A = stage(FixDiv(me,that))
  @api def %(that: A): A = stage(FixMod(me,that))
  @api def <<(that: A): A = stage(FixSLA(me,that))
  @api def >>(that: A): A = stage(FixSRA(me,that))

  @api def <(that: A): Bit = stage(FixLst(me,that))
  @api def <=(that: A): Bit = stage(FixLeq(me,that))
  @api def >(that: A): Bit = stage(FixLst(that,me))
  @api def >=(that: A): Bit = stage(FixLeq(that,me))
}

case class I32(eid: Int) extends Fix[I32](eid, 32, 0) {
  override type I = Int

  def ::(start: I32): Series = Series(start,this,None,None,isUnit = false)

  @api def par(p: I32): Series = Series(I32.c(0),this,None,Some(p),isUnit=false)
  @api def by(step: I32): Series = Series(I32.c(0),this,Some(step),None, isUnit = false)
  def until(end: I32): Series = Series(this, end, None, None, isUnit = false)

  @api def zero: I32 = I32.c(0)
  @api def one: I32 = I32.c(1)

  override def fresh(id: Int): I32 = I32(id)
  override def stagedClass: Class[I32] = classOf[I32]
}
object I32 {
  implicit val tp: I32 = I32(-1)

  @api def c(x: Int): I32 = const[I32](x)
  @api def p(x: Int): I32 = param[I32](x)
}


case class I16(eid: Int) extends Fix[I16](eid, 16, 0) {
  override type I = Short

  override def fresh(id: Int): I16 = I16(id)
  override def stagedClass: Class[I16] = classOf[I16]

  @api def zero: I16 = I16.c(0)
  @api def one: I16 = I16.c(1)
}
object I16 {
  implicit val tp: I16 = I16(-1)
  @api def c(x: Short): I16 = const[I16](x)
}



case class I8(eid: Int) extends Fix[I8](eid, 8, 0) {
  override type I = Byte

  override def fresh(id: Int): I8 = I8(id)
  override def stagedClass: Class[I8] = classOf[I8]

  @api def zero: I8 = I8.c(0)
  @api def one: I8 = I8.c(1)
}
object I8 {
  implicit val tp: I8 = I8(-1)
  @api def c(x: Byte): I8 = const[I8](x)
}


