package pcc.lang
package memories

import forge.api
import pcc.core._
import pcc.node._

import scala.collection.mutable

case class DRAM[A](eid: Int, tA: Bits[A], dummy: String) extends RemoteMem[A,DRAM](eid) {
  type AI = tA.I
  override type I = Array[AI]

  override def fresh(id: Int): DRAM[A] = new DRAM[A](id,tA,dummy)
  override def stagedClass: Class[DRAM[A]] = classOf[DRAM[A]]
  override def typeArguments: List[Sym[_]] = List(tA)
}
object DRAM {
  private lazy val types = new mutable.HashMap[Bits[_],DRAM[_]]()
  implicit def tp[A:Bits]: DRAM[A] = types.getOrElseUpdate(bits[A], new DRAM[A](-1,bits[A],null)).asInstanceOf[DRAM[A]]

  @api def apply[A:Bits](dims: I32*): DRAM[A] = stage(DRAMNew(dims))
}
