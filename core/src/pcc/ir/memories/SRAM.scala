package pcc
package ir
package memories

import forge.api
import pcc.data.Effects

import scala.collection.mutable

/** Types **/
case class SRAM[A](eid: Int, tA: Bits[A]) extends LocalMem[A,SRAM](eid) {
  type AI = tA.I
  override type I = Array[AI]

  override def fresh(id: Int): SRAM[A] = new SRAM[A](id, tA)
  override def stagedClass: Class[SRAM[A]] = classOf[SRAM[A]]
  override def typeArguments: List[Sym[_]] = List(tA)
}
object SRAM {
  private def apply[A](eid: Int, tA: Bits[A]): SRAM[A] = new SRAM[A](eid,tA)

  private lazy val types = new mutable.HashMap[Bits[_],SRAM[_]]()
  implicit def sram[A:Bits]: SRAM[A] = types.getOrElseUpdate(bits[A], new SRAM[A](-1,bits[A])).asInstanceOf[SRAM[A]]

  @api def apply[A:Bits](dims: I32*): SRAM[A] = stage(SRAMAlloc[A](dims))
}


/** Nodes **/
case class SRAMAlloc[A:Bits](dims: Seq[I32]) extends Op[SRAM[A]] {
  override def effects: Effects = Effects.Mutable
}
