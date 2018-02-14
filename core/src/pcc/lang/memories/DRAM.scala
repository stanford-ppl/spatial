package pcc.lang
package memories

import forge.api
import pcc.core._
import pcc.node._

import scala.collection.mutable

case class DRAM[A:Bits]() extends RemoteMem[A,DRAM] {
  override type I = Array[AI]

  override def fresh: DRAM[A] = new DRAM[A]
}
object DRAM {
  private lazy val types = new mutable.HashMap[Bits[_],DRAM[_]]()
  implicit def tp[A:Bits]: DRAM[A] = types.getOrElseUpdate(tbits[A], (new DRAM[A]).asType).asInstanceOf[DRAM[A]]

  @api def apply[A:Bits](dims: I32*): DRAM[A] = stage(DRAMNew(dims))
}
