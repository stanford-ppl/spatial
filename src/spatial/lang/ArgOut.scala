package spatial.lang

import argon._
import forge.tags._
import forge.Ptr

import spatial.node._

@ref class ArgOut[A:Bits] extends LocalMem0[A,ArgOut] with RemoteMem[A,ArgOut] with Ref[Ptr[Any],ArgOut[A]] {
  val tA: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem = implicitly[ArgOut[A] <:< (LocalMem[A,ArgOut] with RemoteMem[A,ArgOut])]

  @api def :=(data: A): Void = stage(ArgOutWrite(this,data,Set.empty))
}
object ArgOut {
  @api def apply[A:Bits]: ArgOut[A] = stage(ArgOutNew(Bits[A].zero))
}
