package pir.lang

import argon._
import forge.Ptr
import forge.tags._
import pir.node.{ScalarBus, VectorBus, WriteOut}

import spatial.lang._

@ref class Out[A:Bits] extends Bits[Out[A]] with Ref[Ptr[Any],Out[A]] {
  // --- Infix methods
  @api def :=(x: A): Void = stage(WriteOut(this, x.asInstanceOf[Bits[A]]))

  @api def -->(in: In[A]): Void = { stage(ScalarBus(this,in)); void }
  @api def ==>(in: In[A]): Void = { stage(VectorBus(this,in)); void }

  // --- Typeclass Methods
  val tA: Bits[A] = Bits[A]
  val box: Out[A] <:< Bits[Out[A]] = implicitly[Out[A] <:< Bits[Out[A]]]
  override val __neverMutable: Boolean = true

  @rig def nbits: Int = tA.nbits
  @rig def zero: Out[A] = const[Out[A]](new Ptr(tA.zero))
  @rig def one: Out[A] = const[Out[A]](new Ptr(tA.one))
  @rig def random(max: Option[Out[A]]): Out[A] = undefinedOp("random")
}

object Out {

}
