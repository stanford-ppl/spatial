package spatial.lang

import core._
import forge.tags._
import spatial.node._

@ref class SRAM[A:Bits] extends Top[SRAM[A]] with LocalMem[A,SRAM] with Ref[Array[Any],SRAM[A]] {
  @api def rows: I32 = SRAM.dim(this,0)
  @api def cols: I32 = SRAM.dim(this,1)
  @api def dim(d: Int): I32 = SRAM.dim(this,d)
  @api def rank: I32 = SRAM.rank(this)

  @api def apply(addr: I32*): A = SRAM.read(this,addr,Nil)
  @api def update(i: I32, data: A): Void = SRAM.write(this,data,Seq(i),Nil)
  @api def update(i: I32, j: I32, data: A): Void = SRAM.write(this,data,Seq(i,j),Nil)
  @api def update(i: I32, j: I32, k: I32, data: A): Void = SRAM.write(this,data,Seq(i,j,k),Nil)
  @api def update(i: I32, j: I32, k: I32, l: I32, data: A): Void = SRAM.write(this,data,Seq(i,j,k,l),Nil)

  // --- Typeclass Methods
  val tA: Bits[A] = Bits[A]
  override val ev: SRAM[A] <:< LocalMem[A,SRAM] = implicitly[SRAM[A] <:< LocalMem[A,SRAM]]
}
object SRAM {
  @api def apply[A:Bits](dims: I32*): SRAM[A] = stage(SRAMNew(dims))

  @rig def dim(sram: SRAM[_], d: Int): I32 = stage(SRAMDim(sram, d))
  @rig def rank(sram: SRAM[_]): I32 = stage(SRAMRank(sram))

  @rig def read[A:Bits](sram: SRAM[A], addr: Seq[I32], ens: Seq[Bit]): A = stage(SRAMRead(sram,addr,ens))
  @rig def write[A:Bits](sram: SRAM[A], data: A, addr: Seq[I32], ens: Seq[Bit]): Void = stage(SRAMWrite(sram,data,addr,ens))
}

