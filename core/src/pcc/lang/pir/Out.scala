package pcc.lang
package pir

import forge._
import pcc.core._
import pcc.node.pir._
import pcc.util.Ptr

case class Out[A](eid: Int, tA: Bits[A]) extends Bits[Out[A]](eid) {
  override type I = Ptr[Any]
  private implicit val bA: Bits[A] = tA

  override def fresh(id: Int): Out[A] = Out(id, tA)
  override def stagedClass: Class[Out[A]] = classOf[Out[A]]
  override def bits: Int = tA.bits

  @api def zero: Out[A] = Out.c(Ptr(tA.zero))
  @api def one: Out[A] = Out.c(Ptr(tA.one))
  @api def :=(x: A): Void = stage(WriteOut(this,tA))

  @api def -->(in: In[A]): Void = { stage(ScalarBus(this,in)); void }
  @api def ==>(in: In[A]): Void = { stage(VectorBus(this,in)); void }
}

object Out {
  implicit def tp[A:Bits]: Out[A] = Out(-1,bits[A])
  @api def c[A:Bits](values: Ptr[Any]): Out[A] = const[Out[A]](values)
}