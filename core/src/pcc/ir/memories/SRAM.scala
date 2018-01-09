package pcc
package ir
package memories

import forge._
import pcc.data.Effects
import pcc.core._

import scala.collection.mutable

/** Types **/
case class SRAM[A](eid: Int, tA: Bits[A]) extends LocalMem[A,SRAM](eid) {
  type AI = tA.I
  override type I = Array[AI]
  private implicit val bA: Bits[A] = tA

  override def fresh(id: Int): SRAM[A] = new SRAM[A](id, tA)
  override def stagedClass: Class[SRAM[A]] = classOf[SRAM[A]]
  override def typeArguments: List[Sym[_]] = List(tA)

  @api def rows: I32 = SRAM.dim(this,0)
  @api def cols: I32 = SRAM.dim(this,1)
  @api def dim(d: Int): I32 = SRAM.dim(this,d)
  @api def rank: I32 = SRAM.rank(this)

  @api def apply(addr: I32*): A = stage(SRAMRead(this,addr))
  @api def update(i: I32, data: A): Void = stage(SRAMWrite(this,Seq(i),data))
  @api def update(i: I32, j: I32, data: A): Void = stage(SRAMWrite(this,Seq(i,j),data))
  @api def update(i: I32, j: I32, k: I32, data: A): Void = stage(SRAMWrite(this,Seq(i,j,k),data))
  @api def update(i: I32, j: I32, k: I32, l: I32, data: A): Void = stage(SRAMWrite(this,Seq(i,j,k,l),data))
}
object SRAM {
  private def apply[A](eid: Int, tA: Bits[A]): SRAM[A] = new SRAM[A](eid,tA)

  private lazy val types = new mutable.HashMap[Bits[_],SRAM[_]]()
  implicit def sram[A:Bits]: SRAM[A] = types.getOrElseUpdate(bits[A], new SRAM[A](-1,bits[A])).asInstanceOf[SRAM[A]]

  @api def apply[A:Bits](dims: I32*): SRAM[A] = stage(SRAMAlloc[A](dims))

  @internal def dim(sram: SRAM[_], d: Int): I32 = sram match {
    case Op(SRAMAlloc(dims)) if d < dims.length => dims(d)
    case _ => stage(SRAMDim(sram, d))
  }
  @internal def rank(sram: SRAM[_]): I32 = sram match {
    case Op(SRAMAlloc(dims)) => I32.c(dims.length)
    case _ => stage(SRAMRank(sram))
  }
}


/** Nodes **/
case class SRAMAlloc[A:Bits](dims: Seq[I32]) extends Op[SRAM[A]] {
  override def effects: Effects = Effects.Mutable
}
case class SRAMRead[A:Bits](sram: SRAM[A], addr: Seq[I32]) extends Op[A]
case class SRAMWrite[A:Bits](sram: SRAM[A], addr: Seq[I32], data: A) extends Op[Void] {
  override def effects: Effects = Effects.Writes(sram)
}

case class SRAMDim(sram: SRAM[_], d: Int) extends Op[I32]
case class SRAMRank(sram: SRAM[_]) extends Op[I32]