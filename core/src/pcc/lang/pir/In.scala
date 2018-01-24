package pcc.lang
package pir

import forge._
import pcc.core._
import pcc.node.pir._
import pcc.util.Ptr

case class In[A](eid: Int, tA: Bits[A]) extends Bits[In[A]](eid) {
  override type I = Ptr[Any]
  private implicit val bA: Bits[A] = tA

  override def fresh(id: Int): In[A] = In(id, tA)
  override def stagedClass: Class[In[A]] = classOf[In[A]]
  override def bits: Int = tA.bits

  @api def zero: In[A] = In.c(Ptr(tA.zero))
  @api def one: In[A] = In.c(Ptr(tA.one))

  @api def read: A = stage(ReadIn(this,tA))
}

object In {
  implicit def tp[A:Bits]: In[A] = In(-1,bits[A])
  @api def c[A:Bits](values: Ptr[Any]): In[A] = const[In[A]](values)
}

