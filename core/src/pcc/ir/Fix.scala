package pcc
package ir

import forge._

abstract class Fix[A](id: Int, intBits: Int, fracBits: Int)(implicit ev: A<:<Fix[A]) extends Num[A](id) {
  final override def bits: Int = intBits + fracBits
  private implicit val tA: Fix[A] = this
  def iBits: Int = intBits
  def fBits: Int = fracBits

  @api def +(that: A): A = stage(FixAdd[A](me,that))
  @api def -(that: A): A = stage(FixSub[A](me,that))
  @api def *(that: A): A = stage(FixMul[A](me,that))
  @api def /(that: A): A = stage(FixDiv[A](me,that))
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
