package spatial.lang

import argon._
import forge.Ptr
import forge.tags._
import spatial.node._
import spatial.metadata.memory._

@ref class Reg[A:Bits] extends LocalMem0[A,Reg] with StagedVarLike[A] with Ref[Ptr[Any],Reg[A]] {
  val A: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem: Reg[A] <:< LocalMem[A,Reg] = implicitly[Reg[A] <:< LocalMem[A,Reg]]

  // --- Infix Methods
  @api def :=(data: A): Void = Reg.write(this, data)
  @api def value: A = Reg.read(this)
  @api def reset(): Void = stage(RegReset(this,Set.empty))
  @api def reset(en: Bit): Void = stage(RegReset(this,Set(en)))

  /** Indicate that the memory should be buffered and ignore
    * ignore potential situation where result from running sequentially
    * does not match with resurt from running pipelined
    */
  def buffer: Reg[A] = { this.isWriteBuffer = true; me }
  /** Do not buffer memory */
  def nonbuffer: Reg[A] = { this.isNonBuffer = true; me }

  // --- Typeclass Methods
  @rig def __sread(): A = Reg.read(this)
  @rig def __sassign(x: A): Unit = Reg.write(this, x)

  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = this.value
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit] ): Void = Reg.write(this, data, ens)
  @rig def __reset(ens: Set[Bit]): Void = stage(RegReset(this,ens))

  @rig override def toText: Text = this.value.toText
}

object Reg {
  @api def apply[A:Bits]: Reg[A] = Reg.alloc[A](zero[A])
  @api def apply[A:Bits](reset: A): Reg[A] = Reg.alloc[A](reset)

  @rig def alloc[A:Bits](reset: A): Reg[A] = stage(RegNew[A](Bits[A].box(reset)))
  @rig def read[A](reg: Reg[A]): A = {
    implicit val tA: Bits[A] = reg.A
    stage(RegRead(reg))
  }
  @rig def write[A](reg: Reg[A], data: Bits[A], ens: Set[Bit] = Set.empty): Void = {
    implicit val tA: Bits[A] = reg.A
    stage(RegWrite(reg,data,ens))
  }
}

@ref class FIFOReg[A:Bits] extends LocalMem0[A,FIFOReg] with StagedVarLike[A] with Ref[Ptr[Any],FIFOReg[A]] {
  val A: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem: FIFOReg[A] <:< LocalMem[A,FIFOReg] = implicitly[FIFOReg[A] <:< LocalMem[A,FIFOReg]]

  @api def value: A = FIFOReg.deq(this)

  // --- Typeclass Methods
  @rig def __sread(): A = FIFOReg.deq(this)
  @rig def __sassign(x: A): Unit = FIFOReg.enq(this, x)

  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = this.value
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit] ): Void = FIFOReg.enq(this, data, ens)
  @rig def __reset(ens: Set[Bit]): Void = void
}

object FIFOReg {
  @api def apply[A:Bits]: FIFOReg[A] = FIFOReg.alloc[A](zero[A])
  @api def apply[A:Bits](reset: A): FIFOReg[A] = FIFOReg.alloc[A](reset)

  @rig def alloc[A:Bits](reset: A): FIFOReg[A] = stage(FIFORegNew[A](Bits[A].box(reset)))
  @rig def deq[A](reg: FIFOReg[A]): A = {
    implicit val tA: Bits[A] = reg.A
    stage(FIFORegDeq(reg))
  }
  @rig def enq[A](reg: FIFOReg[A], data: Bits[A], ens: Set[Bit] = Set.empty): Void = {
    implicit val tA: Bits[A] = reg.A
    stage(FIFORegEnq(reg,data,ens))
  }
}

object ArgIn {
  @api def apply[A:Bits]: Reg[A] = stage(ArgInNew(Bits[A].zero))
}

object ArgOut {
  @api def apply[A:Bits]: Reg[A] = stage(ArgOutNew(Bits[A].zero))
}

object HostIO {
  @api def apply[A:Bits]: Reg[A] = stage(HostIONew(Bits[A].zero))
}



