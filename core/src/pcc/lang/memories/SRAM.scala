package pcc.lang
package memories

import forge._
import pcc.core._
import pcc.node._

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

  @api def apply(addr: I32*): A = SRAM.read(this,addr,Nil)
  @api def update(i: I32, data: A): pcc.lang.Void = SRAM.write(this,data,Seq(i),Nil)
  @api def update(i: I32, j: I32, data: A): pcc.lang.Void = SRAM.write(this,data,Seq(i,j),Nil)
  @api def update(i: I32, j: I32, k: I32, data: A): pcc.lang.Void = SRAM.write(this,data,Seq(i,j,k),Nil)
  @api def update(i: I32, j: I32, k: I32, l: I32, data: A): pcc.lang.Void = SRAM.write(this,data,Seq(i,j,k,l),Nil)
}
object SRAM {
  private def apply[A](eid: Int, tA: Bits[A]): SRAM[A] = new SRAM[A](eid,tA)

  private lazy val types = new mutable.HashMap[Bits[_],SRAM[_]]()
  implicit def sram[A:Bits]: SRAM[A] = types.getOrElseUpdate(bits[A], new SRAM[A](-1,bits[A])).asInstanceOf[SRAM[A]]

  @api def apply[A:Bits](dims: I32*): SRAM[A] = stage(SRAMAlloc(dims))

  @internal def dim(sram: SRAM[_], d: Int): I32 = stage(SRAMDim(sram, d))
  @internal def rank(sram: SRAM[_]): I32 = stage(SRAMRank(sram))

  @internal def read[A:Bits](sram: SRAM[A], addr: Seq[I32], ens: Seq[Bit]): A = stage(SRAMRead(sram,addr,ens))
  @internal def write[A:Bits](sram: SRAM[A], data: A, addr: Seq[I32], ens: Seq[Bit]): Void = stage(SRAMWrite(sram,data,addr,ens))
}

