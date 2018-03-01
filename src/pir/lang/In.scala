package pir.lang

import core._
import forge.Ptr
import forge.tags._

import pir.node.ReadIn
import spatial.lang._

import scala.collection.mutable

@ref class In[A:Bits]() extends Bits[In[A]] with Ref[Ptr[Any],In[A]] {
  val tA: Bits[A] = Bits[A]
  val box: In[A] <:< Bits[In[A]] = implicitly[In[A] <:< Bits[In[A]]]
  override def isPrimitive: Boolean = true
  override def nbits: Int = tA.nbits

  @rig def zero: In[A] = In.c(new Ptr(tA.zero))
  @rig def one: In[A] = In.c(new Ptr(tA.one))
  @rig def random(max: Option[In[A]]): In[A] = undefinedOp("random")

  @api def read: A = stage(ReadIn(this))
}

object In {
  def c[A:Bits](values: Ptr[Any]): In[A] = const[In[A]](values)
}
