package spatial.lang
package memories

import forge.tags._
import core._
import spatial.node._

import nova.implicits.views._

import scala.collection.mutable

case class SRAM[A:Bits]() extends LocalMem[A,SRAM] {
  override type I = Array[AI]
  override def fresh: SRAM[A] = new SRAM[A]

  @api def rows: I32 = SRAM.dim(this,0)
  @api def cols: I32 = SRAM.dim(this,1)
  @api def dim(d: Int): I32 = SRAM.dim(this,d)
  @api def rank: I32 = SRAM.rank(this)

  @api def apply(addr: I32*): A = SRAM.read(this,addr,Nil)
  @api def update(i: I32, data: A): Void = SRAM.write(this,data,Seq(i),Nil)
  @api def update(i: I32, j: I32, data: A): Void = SRAM.write(this,data,Seq(i,j),Nil)
  @api def update(i: I32, j: I32, k: I32, data: A): Void = SRAM.write(this,data,Seq(i,j,k),Nil)
  @api def update(i: I32, j: I32, k: I32, l: I32, data: A): Void = SRAM.write(this,data,Seq(i,j,k,l),Nil)
}
object SRAM {
  private lazy val types = new mutable.HashMap[Bits[_],SRAM[_]]()
  implicit def tp[A:Bits]: SRAM[A] = types.getOrElseUpdate(tbits[A], (new SRAM[A]).asType).asInstanceOf[SRAM[A]]

  @api def apply[A:Bits](dims: I32*): SRAM[A] = stage(SRAMNew(dims))

  @rig def dim(sram: SRAM[_], d: Int): I32 = stage(SRAMDim(sram, d))
  @rig def rank(sram: SRAM[_]): I32 = stage(SRAMRank(sram))

  @rig def read[A:Bits](sram: SRAM[A], addr: Seq[I32], ens: Seq[Bit]): A = stage(SRAMRead(sram,addr,ens))
  @rig def write[A:Bits](sram: SRAM[A], data: A, addr: Seq[I32], ens: Seq[Bit]): Void = stage(SRAMWrite(sram,data.view[Bits],addr,ens))
}

