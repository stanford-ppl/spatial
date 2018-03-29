package spatial.lang

import argon._
import forge.tags._
import forge.Ptr

import spatial.node._

@ref class ArgOut[A:Bits] extends LocalMem0[A,ArgOut] with RemoteMem[A,ArgOut] with Ref[Ptr[Any],ArgOut[A]] {
  val A: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem = implicitly[ArgOut[A] <:< (LocalMem[A,ArgOut] with RemoteMem[A,ArgOut])]

  @api def :=(data: A): Void = stage(ArgOutWrite(this,data,Set.empty))

  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = {
    error(ctx, "Cannot write to an ArgOut")
    error(ctx)
    err[A]("Cannot write to an ArgOut")
  }
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit] ): Void = stage(ArgOutWrite(this,data,ens))
  @rig def __reset(ens: Set[Bit]): Void = void
}
object ArgOut {
  @api def apply[A:Bits]: ArgOut[A] = stage(ArgOutNew(Bits[A].zero))
}
