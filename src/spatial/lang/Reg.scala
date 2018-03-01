package spatial.lang

import core._
import forge.{Ptr,VarLike}
import forge.tags._
import spatial.lang
import spatial.node._

import scala.collection.mutable

@ref class Reg[A:Bits] extends Top[Reg[A]] with LocalMem[A,Reg] with StagedVarLike[A] with Ref[Ptr[Any],Reg[A]] {
  val tA: Bits[A] = Bits[A]
  private implicit val evA: A <:< Bits[A] = Bits[A].box

  // --- Infix Methods
  @api def :=(data: A): spatial.lang.Void = Reg.write(this, data, Nil)
  @api def value: A = Reg.read(this)

  @rig def __sread(): A = Reg.read(this)
  @rig def __sassign(x: A): Unit = Reg.write(this, x)

  // --- Typeclass Methods
  override val ev: Reg[A] <:< LocalMem[A,Reg] = implicitly[Reg[A] <:< LocalMem[A,Reg]]
}
object Reg {
  @api def apply[A:Bits]: Reg[A] = Reg.alloc[A](zero[A])
  @api def apply[A:Bits](reset: A): Reg[A] = Reg.alloc[A](reset)

  @rig def alloc[A:Bits](reset: A): Reg[A] = stage(RegNew[A](reset))
  @rig def read[A](reg: Reg[A]): A = {
    implicit val tA: Bits[A] = reg.tA
    stage(RegRead(reg))
  }
  @rig def write[A](reg: Reg[A], data: Bits[A], en: Seq[Bit] = Nil): Void = {
    implicit val tA: Bits[A] = reg.tA
    stage(RegWrite(reg,data,en))
  }
}

object ArgIn {
  @api def apply[A:Bits]: Reg[A] = stage(ArgInNew(Bits[A].zero))
}

object ArgOut {
  @api def apply[A:Bits]: Reg[A] = stage(ArgOutNew(zero[A]))
}


