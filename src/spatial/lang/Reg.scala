package spatial.lang

import argon._
import forge.{Ptr,VarLike}
import forge.tags._
import spatial.lang
import spatial.node._

import scala.collection.mutable

@ref class Reg[A:Bits] extends LocalMem0[A,Reg] with StagedVarLike[A] with Ref[Ptr[Any],Reg[A]] {
  val A: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box
  override val evMem: Reg[A] <:< LocalMem[A,Reg] = implicitly[Reg[A] <:< LocalMem[A,Reg]]

  // --- Infix Methods
  @api def :=(data: A): Void = Reg.write(this, data)
  @api def value: A = Reg.read(this)

  @rig def __sread(): A = Reg.read(this)
  @rig def __sassign(x: A): Unit = Reg.write(this, x)

  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = this.value
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit] ): Void = Reg.write(this, data, ens)
  @rig def __reset(ens: Set[Bit]): Void = stage(RegReset(this,ens))

  @rig override def toText: Text = this.value.toText
}
object Reg {
  @api def apply[A:Bits]: Reg[A] = Reg.alloc[A](zero[A])
  @api def apply[A:Bits](reset: A): Reg[A] = Reg.alloc[A](reset)

  @rig def alloc[A:Bits](reset: A): Reg[A] = stage(RegNew[A](reset))
  @rig def read[A](reg: Reg[A]): A = {
    implicit val tA: Bits[A] = reg.A
    stage(RegRead(reg))
  }
  @rig def write[A](reg: Reg[A], data: Bits[A], ens: Set[Bit] = Set.empty): Void = {
    implicit val tA: Bits[A] = reg.A
    stage(RegWrite(reg,data,ens))
  }
}


