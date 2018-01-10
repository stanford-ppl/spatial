package pcc
package ir

import forge._

case class Vec[A](eid: Int, var len: Int, tA: Bits[A]) extends Bits[Vec[A]](eid) {
  type AI = tA.I
  override type I = Array[AI]

  override def fresh(id: Int): Vec[A] = Vec(id, 0, tA)
  override def stagedClass: Class[Vec[A]] = classOf[Vec[A]]

  override def bits: Int = tA.bits * len
}

object Vec {
  @internal def apply[T:Bits](elems: T*): Vec[T] = {
    implicit val tV: Vec[T] = Vec(-1, elems.length, bits[T])
    stage(VecAlloc(elems))
  }

}


case class VecAlloc[T:Bits](elems: Seq[T])(implicit val tV: Vec[T]) extends Op[Vec[T]] {
  def mirror(f:Tx) = Vec.apply(f(elems):_*)
}
