package nova
package lang
package memories

import forge.tags._
import nova.core._
import nova.node._

import scala.collection.mutable

case class Reg[A:Bits]() extends LocalMem[A,Reg] {
  override type I = Array[AI]
  override def fresh: Reg[A] = new Reg[A]

  @api def :=(data: A): nova.lang.Void = Reg.write(me, data, Nil)
  @api def value: A = Reg.read(me)

  @api override def !==(that: Reg[A]): Bit = this.value.view[Bits] nEql that.value.view[Bits]
  @api override def ===(that: Reg[A]): Bit = this.value.view[Bits] isEql that.value.view[Bits]
}
object Reg {
  private lazy val types = new mutable.HashMap[Bits[_],Reg[_]]()
  implicit def tp[A:Bits]: Reg[A] = types.getOrElseUpdate(tbits[A], (new Reg[A]).asType).asInstanceOf[Reg[A]]

  @api def apply[A:Bits]: Reg[A] = Reg.alloc[A](zero[A])
  @api def apply[A:Bits](reset: A): Reg[A] = Reg.alloc[A](reset)

  @rig def alloc[A:Bits](reset: A)(implicit ctx: SrcCtx, state: State): Reg[A] = stage(RegNew(reset.view[Bits]))
  @rig def read[A:Bits](reg: Reg[A]): A = stage(RegRead(reg))
  @rig def write[A:Bits](reg: Reg[A], data: A, en: Seq[Bit] = Nil): Void = stage(RegWrite(reg,data.view[Bits],en))
}

object ArgIn {
  @api def apply[A:Bits]: Reg[A] = stage(ArgInNew(zero[A].view[Bits]))
}

object ArgOut {
  @api def apply[A:Bits]: Reg[A] = stage(ArgOutNew(zero[A].view[Bits]))
}


