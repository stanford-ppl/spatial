package spatial.lang

import core._
import forge.tags._
import utils.implicits.collections._

import spatial.node._

abstract class SRAM[A:Bits,C[T]](implicit val evMem: C[A] <:< SRAM[A,C]) extends LocalMem[A,C] {
  val tA: Bits[A] = Bits[A]
  def rank: Int
  @api def size: I32 = product(dims:_*)
  @api def dims: Seq[I32] = Seq.tabulate(rank){d => stage(MemDim(this,d)) }
  @api def rows: I32 = dim0
  @api def cols: I32 = dim1
  @api def dim0: I32 = dims.head
  @api def dim1: I32 = dims.indexOrElse(1, I32(1))
  @api def dim2: I32 = dims.indexOrElse(2, I32(1))

  /** Returns the value at `addr`.
    * The number of indices should match the rank of this SRAM.
    */
  @api def apply(pos: Idx): A = applyAddr(Seq(pos))
  @api def apply(row: Idx, col: Idx): A = applyAddr(Seq(row,col))
  @api def apply(d0: Idx, d1: Idx, d2: Idx): A = applyAddr(Seq(d0,d1,d2))
  @api def apply(d0: Idx, d1: Idx, d2: Idx, d3: Idx): A = applyAddr(Seq(d0,d1,d2,d3))
  @api def apply(d0: Idx, d1: Idx, d2: Idx, d3: Idx, d4: Idx): A = applyAddr(Seq(d0,d1,d2,d3,d4))


  /** Updates the value at `addr` to `data`.
    * The number of indices should match the rank of this SRAM.
    */
  @api def update(pos: Idx, data: A): Void = updateAddr(Seq(pos), data)
  @api def update(row: Idx, col: Idx, data: A): Void = updateAddr(Seq(row,col), data)
  @api def update(d0: Idx, d1: Idx, d2: Idx, data: A): Void = updateAddr(Seq(d0,d1,d2), data)
  @api def update(d0: Idx, d1: Idx, d2: Idx, d3: Idx, data: A): Void = updateAddr(Seq(d0,d1,d2,d3), data)
  @api def update(d0: Idx, d1: Idx, d2: Idx, d3: Idx, d4: Idx, data: A): Void = updateAddr(Seq(d0,d1,d2,d3,d4), data)


  @rig private def applyAddr(addr: Seq[Idx]): A = {
    checkDims(addr.length)
    stage(SRAMRead(this,addr,Set.empty))
  }

  @rig private def updateAddr(addr: Seq[Idx], data: A): Void = {
    checkDims(addr.length)
    stage(SRAMWrite(this,data,addr,Set.empty))
  }

  @rig private def checkDims(given: Int): Unit = {
    if (given != rank) {
      error(ctx, s"Expected a $rank-dimensional address, got a $given-dimensional address.")
      error(ctx)
    }
  }

  // --- Typeclass Methods
}
object SRAM {
  /** Allocates a 1-dimensional [[SRAM1]] with capacity of `length` elements of type [[A]]. **/
  @api def apply[A:Bits](length: I32): SRAM1[A] = stage(SRAMNew[A,SRAM1](Seq(length)))

  /** Allocates a 2-dimensional [[SRAM2]] with `rows` x `cols` elements of type [[A]]. **/
  @api def apply[A:Bits](rows: I32, cols: I32): SRAM2[A] = stage(SRAMNew[A,SRAM2](Seq(rows,cols)))

  /** Allocates a 3-dimensional [[SRAM3]] with the given dimensions and elements of type [[A]]. **/
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32): SRAM3[A] = stage(SRAMNew[A,SRAM3](Seq(d0,d1,d2)))

  /** Allocates a 4-dimensional [[SRAM4]] with the given dimensions and elements of type [[A]]. **/
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32, d3: I32): SRAM4[A] = stage(SRAMNew[A,SRAM4](Seq(d0,d1,d2,d3)))

  /** Allocates a 5-dimensional [[SRAM5]] with the given dimensions and elements of type [[A]]. **/
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32, d3: I32, d4: I32): SRAM5[A] = stage(SRAMNew[A,SRAM5](Seq(d0,d1,d2,d3,d4)))
}

/** A 1-dimensional [[SRAM]] with elements of type [[A]]. **/
@ref class SRAM1[A:Bits] extends SRAM[A,SRAM1] with Ref[Array[Any],SRAM1[A]] {
  def rank: Int = 1
}

/** A 2-dimensional [[SRAM]] with elements of type [[A]]. **/
@ref class SRAM2[A:Bits] extends SRAM[A,SRAM2] with Ref[Array[Any],SRAM2[A]] {
  def rank: Int = 2
}

/** A 3-dimensional [[SRAM]] with elements of type [[A]]. **/
@ref class SRAM3[A:Bits] extends SRAM[A,SRAM3] with Ref[Array[Any],SRAM3[A]] {
  def rank: Int = 3
}

/** A 4-dimensional [[SRAM]] with elements of type [[A]]. **/
@ref class SRAM4[A:Bits] extends SRAM[A,SRAM4] with Ref[Array[Any],SRAM4[A]] {
  def rank: Int = 4
}

/** A 5-dimensional [[SRAM]] with elements of type [[A]]. **/
@ref class SRAM5[A:Bits] extends SRAM[A,SRAM5] with Ref[Array[Any],SRAM5[A]] {
  def rank: Int = 5
}

