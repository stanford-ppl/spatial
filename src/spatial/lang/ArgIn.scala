package spatial.lang

import argon._
import forge.tags._
import forge.Ptr

import spatial.node._

@ref class ArgIn[A:Bits] extends LocalMem0[A,ArgIn] with RemoteMem[A,ArgIn] with Ref[Ptr[Any],ArgIn[A]] {
  val A: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem = implicitly[ArgIn[A] <:< (LocalMem[A,ArgIn] with RemoteMem[A,ArgIn])]

  @api def value: A = stage(ArgInRead(this))

  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = this.value
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit] ): Void = {
    error(ctx, "Cannot write to ArgIn")
    error(ctx)
    err[Void]("Cannot write to ArgIn")
  }
  @rig def __reset(ens: Set[Bit]): Void = void
}
object ArgIn {
  @api def apply[A:Bits]: ArgIn[A] = stage(ArgInNew(Bits[A].zero))
}
