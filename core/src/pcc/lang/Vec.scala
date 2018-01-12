package pcc.lang

import forge._
import pcc.core._
import pcc.node._

case class Vec[A](eid: Int, var len: Int, tA: Bits[A]) extends Bits[Vec[A]](eid) {
  type AI = tA.I
  override type I = Array[AI]
  private implicit val bA: Bits[A] = tA

  override def fresh(id: Int): Vec[A] = Vec(id, 0, tA)
  override def stagedClass: Class[Vec[A]] = classOf[Vec[A]]

  override def bits: Int = tA.bits * len

  @api def zero: Vec[A] = Vec(Seq.fill(len){ tA.zero }:_*)
  @api def one: Vec[A] = Vec(Seq.fill(len-1){ tA.zero} :+ tA.one :_*)
}

object Vec {
  @internal def apply[T:Bits](elems: T*): Vec[T] = {
    implicit val tV: Vec[T] = Vec(-1, elems.length, bits[T])
    stage(VecAlloc(elems))
  }
}
