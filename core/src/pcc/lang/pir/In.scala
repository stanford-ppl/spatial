package pcc.lang
package pir

import forge._
import pcc.core._
import pcc.node.pir._
import pcc.util.Ptr

case class In[A:Bits]() extends Bits[In[A]] {
  val tA: Bits[A] = tbits[A]
  override type I = Ptr[Any]

  override def fresh: In[A] = new In[A]
  override def bits: Int = tA.bits

  @api def zero: In[A] = In.c(Ptr(tA.zero))
  @api def one: In[A] = In.c(Ptr(tA.one))

  @api def read: A = stage(ReadIn(this,tA))
}

object In {
  implicit def tp[A:Bits]: In[A] = (new In[A]).asType
  def c[A:Bits](values: Ptr[Any]): In[A] = const[In[A]](values)
}
