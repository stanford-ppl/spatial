package pcc.lang
package pir

import forge._
import pcc.core._
import pcc.node.pir._
import pcc.util.Ptr

import scala.collection.mutable

case class In[A:Bits]() extends Bits[In[A]] {
  val tA: Bits[A] = tbits[A]
  override type I = Ptr[Any]

  override def fresh: In[A] = new In[A]
  override def nBits: Int = tA.nBits

  @rig def zero: In[A] = In.c(Ptr(tA.zero))
  @rig def one: In[A] = In.c(Ptr(tA.one))
  @rig def random(max: Option[In[A]]): In[A] = undefinedOp("random")

  @api def read: A = stage(ReadIn(this))
}

object In {
  private lazy val types = new mutable.HashMap[Bits[_],In[_]]()

  implicit def tp[A:Bits]: In[A] = types.getOrElseUpdate(tbits[A],(new In[A]).asType).asInstanceOf[In[A]]
  def c[A:Bits](values: Ptr[Any]): In[A] = const[In[A]](values)
}
