package pcc
package ir
package memories

import forge.api
import pcc.data.Effects

import scala.collection.mutable

/** Types **/
case class FIFO[A](eid: Int, tA: Bits[A]) extends LocalMem[A,FIFO](eid) {
  type AI = tA.I
  override type I = Array[AI]

  override def fresh(id: Int): FIFO[A] = FIFO[A](id,tA)
  override def stagedClass: Class[FIFO[A]] = classOf[FIFO[A]]
  override def typeArguments: List[Sym[_]] = List(tA)
}
object FIFO {
  private lazy val types = new mutable.HashMap[Bits[_],FIFO[_]]()
  implicit def fifo[A:Bits]: FIFO[A] = types.getOrElseUpdate(bits[A], FIFO[A](-1,bits[A])).asInstanceOf[FIFO[A]]

  @api def apply[A:Bits](depth: I32): FIFO[A] = stage(FIFOAlloc[A](depth))
}


/** Nodes **/
case class FIFOAlloc[A:Bits](depth: I32) extends Alloc[FIFO[A]] {
  override def effects: Effects = Effects.Mutable
  def mirror(f:Tx) = FIFO.apply(f(depth))
}
