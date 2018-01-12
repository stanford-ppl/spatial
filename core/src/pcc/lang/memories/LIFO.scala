package pcc.lang
package memories

import forge.api
import pcc.core._
import pcc.node._

import scala.collection.mutable

/** Types **/
case class LIFO[A:Bits](eid: Int, tA: Bits[A]) extends LocalMem[A,LIFO](eid) {
  type AI = tA.I
  override type I = Array[AI]

  override def fresh(id: Int): LIFO[A] = LIFO[A](id,tA)
  override def stagedClass: Class[LIFO[A]] = classOf[LIFO[A]]
  override def typeArguments: List[Sym[_]] = List(tA)
}
object LIFO {
  private lazy val types = new mutable.HashMap[Bits[_],LIFO[_]]()
  implicit def tp[A:Bits]: LIFO[A] = types.getOrElseUpdate(bits[A], LIFO[A](-1,bits[A])).asInstanceOf[LIFO[A]]

  @api def apply[A:Bits](depth: I32): LIFO[A] = stage(LIFOAlloc(depth))
}
