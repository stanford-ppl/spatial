package pcc
package lang
package memories

import forge._
import pcc.core._
import pcc.node._

import scala.collection.mutable

case class Reg[A:Bits]() extends LocalMem[A,Reg] {
  override type I = Array[AI]
  override def fresh: Reg[A] = new Reg[A]

  @api def :=(data: A): pcc.lang.Void = Reg.write(me, data, Nil)
  @api def value: A = Reg.read(me)
}
object Reg {
  private lazy val types = new mutable.HashMap[Bits[_],Reg[_]]()
  implicit def tp[A:Bits]: Reg[A] = types.getOrElseUpdate(bits[A], (new Reg[A]).asType).asInstanceOf[Reg[A]]

  @api def apply[A:Bits]: Reg[A] = Reg.alloc[A](bits[A].zero)
  @api def apply[A:Bits](reset: A): Reg[A] = Reg.alloc[A](reset)

  @rig def alloc[A:Bits](reset: A)(implicit ctx: SrcCtx, state: State): Reg[A] = stage(RegNew(reset))
  @rig def read[A:Bits](reg: Reg[A]): A = stage(RegRead(reg))
  @rig def write[A:Bits](reg: Reg[A], data: A, en: Seq[Bit] = Nil): Void = stage(RegWrite(reg,data,en))
}

object ArgIn {
  @api def apply[A:Bits]: Reg[A] = stage(ArgInNew(bits[A].zero))
}

object ArgOut {
  @api def apply[A:Bits]: Reg[A] = stage(ArgOutNew(bits[A].zero))
}


