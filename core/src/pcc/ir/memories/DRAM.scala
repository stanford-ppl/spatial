package pcc
package ir
package memories

import forge.api
import pcc.data.Effects

import scala.collection.mutable

/** Types **/
case class DRAM[A](eid: Int, tA: Bits[A]) extends RemoteMem[A,DRAM](eid) {
  type AI = tA.I
  override type I = Array[AI]

  override def fresh(id: Int): DRAM[A] = new DRAM[A](id,tA)
  override def stagedClass: Class[DRAM[A]] = classOf[DRAM[A]]
  override def typeArguments: List[Sym[_]] = List(tA)
}
object DRAM {
  private def apply[A](eid: Int, tA: Bits[A]): DRAM[A] = new DRAM[A](eid,tA)

  private lazy val types = new mutable.HashMap[Bits[_],DRAM[_]]()
  implicit def dram[A:Bits]: DRAM[A] = types.getOrElseUpdate(bits[A], new DRAM[A](-1,bits[A])).asInstanceOf[DRAM[A]]

  @api def apply[A:Bits](dims: I32*): DRAM[A] = stage(DRAMAlloc[A](dims))
}


/** Nodes **/
case class DRAMAlloc[A:Bits](dims: Seq[I32]) extends Alloc[DRAM[A]] {
  override def effects: Effects = Effects.Mutable
  def mirror(f:Tx) = DRAM.apply(f(dims):_*)
}
