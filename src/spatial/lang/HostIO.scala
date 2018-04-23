package spatial.lang

import argon._
import forge.tags._
import forge.Ptr

import spatial.node._

@ref class HostIO[A:Bits] extends LocalMem0[A,HostIO] with RemoteMem[A,HostIO] with Ref[Ptr[Any],HostIO[A]] {
  val A: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem = implicitly[HostIO[A] <:< (LocalMem[A,HostIO] with RemoteMem[A,HostIO])]

  @api def value: A = stage(HostIORead(this))
  @api def :=(data: A): Void = stage(HostIOWrite(this,data,Set.empty))

  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = this.value
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit] ): Void = {
    stage(HostIOWrite(this,data,ens))
  }
  @rig def __reset(ens: Set[Bit]): Void = void
}

object HostIO {
  @api def apply[A:Bits]: HostIO[A] = stage(HostIONew(Bits[A].zero))
}