package pcc
package lang
package memories

import forge._
import pcc.core._
import pcc.node._

import scala.collection.mutable

case class Reg[A](eid: Int, tA: Bits[A]) extends LocalMem[A,Reg](eid) {
  type AI = tA.I
  override type I = Array[AI]
  private implicit val bA: Bits[A] = tA

  override def fresh(id: Int): Reg[A] = new Reg[A](id, tA)
  override def stagedClass: Class[Reg[A]] = classOf[Reg[A]]
  override def typeArguments: List[Sym[_]] = List(tA)

  @api def :=(data: A): pcc.lang.Void = Reg.write(me, data, Nil)
  @api def value: A = Reg.read(me)
}
object Reg {
  private lazy val types = new mutable.HashMap[Bits[_],Reg[_]]()
  implicit def reg[A:Bits]: Reg[A] = types.getOrElseUpdate(bits[A], new Reg[A](-1,bits[A])).asInstanceOf[Reg[A]]

  @api def apply[T:Bits]: Reg[T] = Reg[T](bits[T].zero)
  @api def apply[T:Bits](reset: T): Reg[T] = stage(RegAlloc(reset))

  @api def read[T:Bits](reg: Reg[T]): T = stage(RegRead(reg))
  @api def write[T:Bits](reg: Reg[T], data: T, en: Seq[Bit]): Void = stage(RegWrite(reg,data,en))
}



