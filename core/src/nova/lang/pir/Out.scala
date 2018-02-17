package nova.lang
package pir

import forge.tags._
import forge.Ptr

import nova.core._
import nova.node.pir._

case class Out[A:Bits]() extends Bits[Out[A]] {
  val tA: Bits[A] = tbits[A]
  override type I = Ptr[Any]

  override def fresh: Out[A] = new Out[A]
  override def nBits: Int = tA.nBits

  @rig def zero: Out[A] = Out.c(Ptr(tA.zero))
  @rig def one: Out[A] = Out.c(Ptr(tA.one))
  @rig def random(max: Option[Out[A]]): Out[A] = undefinedOp("random")

  @api def :=(x: A): Void = stage(WriteOut(this, x.asInstanceOf[Bits[A]]))

  @api def -->(in: In[A]): Void = { stage(ScalarBus(this,in)); void }
  @api def ==>(in: In[A]): Void = { stage(VectorBus(this,in)); void }
}

object Out {
  implicit def tp[A:Bits]: Out[A] = (new Out[A]).asType
  def c[A:Bits](values: Ptr[Any]): Out[A] = const[Out[A]](values)
}
