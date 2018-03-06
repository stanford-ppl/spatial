package spatial.lang

import core._
import forge.tags._
import utils.implicits.collections._

import spatial.node._

/** An N-dimensional register file **/
abstract class RegFile[A:Bits,C[T]](implicit val evMem: C[A] <:< RegFile[A,C]) extends LocalMem[A,C] {
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
    * The number of indices should match the rank of this RegFile.
    */
  @api def apply(pos: Idx): A = applyAddr(Seq(pos))
  @api def apply(row: Idx, col: Idx): A = applyAddr(Seq(row,col))
  @api def apply(d0: Idx, d1: Idx, d2: Idx): A = applyAddr(Seq(d0,d1,d2))
  @api def apply(d0: Idx, d1: Idx, d2: Idx, d3: Idx): A = applyAddr(Seq(d0,d1,d2,d3))
  @api def apply(d0: Idx, d1: Idx, d2: Idx, d3: Idx, d4: Idx): A = applyAddr(Seq(d0,d1,d2,d3,d4))


  /** Updates the value at `addr` to `data`.
    * The number of indices should match the rank of this RegFile.
    */
  @api def update(pos: Idx, data: A): Void = updateAddr(Seq(pos), data)
  @api def update(row: Idx, col: Idx, data: A): Void = updateAddr(Seq(row,col), data)
  @api def update(d0: Idx, d1: Idx, d2: Idx, data: A): Void = updateAddr(Seq(d0,d1,d2), data)
  @api def update(d0: Idx, d1: Idx, d2: Idx, d3: Idx, data: A): Void = updateAddr(Seq(d0,d1,d2,d3), data)
  @api def update(d0: Idx, d1: Idx, d2: Idx, d3: Idx, d4: Idx, data: A): Void = updateAddr(Seq(d0,d1,d2,d3,d4), data)


  /** Resets this RegFile to its initial values (or zeros, if unspecified). **/
  @api def reset: Void = stage(RegFileReset(this, Set.empty))

  /** Conditionally resets this RegFile based on `cond` to its inital values (or zeros if unspecified). **/
  @api def reset(cond: Bit): Void = stage(RegFileReset(this, Set(cond)))

  @rig private def applyAddr(addr: Seq[Idx]): A = {
    checkDims(addr.length)
    stage(RegFileRead(this,addr,Set.empty))
  }

  @rig private def updateAddr(addr: Seq[Idx], data: A): Void = {
    checkDims(addr.length)
    stage(RegFileWrite(this,data,addr,Set.empty))
  }

  @rig private def checkDims(given: Int): Unit = {
    if (given != rank) {
      error(ctx, s"Expected a $rank-dimensional address, got a $given-dimensional address.")
      error(ctx)
    }
  }
}
object RegFile {
  /** Allocates a [[RegFile1]] with capacity for `length` elements of type [[A]]. **/
  @api def apply[A:Bits](length: I32): RegFile1[A] = stage(RegFileNew[A,RegFile1](Seq(length),None))

  /** Allocates a [[RegFile2]] with size `rows` x `cols` and elements of type [[A]]. **/
  @api def apply[A:Bits](rows: I32, cols: I32): RegFile2[A] = stage(RegFileNew[A,RegFile2](Seq(rows,cols),None))

  /** Allocates a [[RegFile3]] with the given dimensions and elements of type [[A]]. **/
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32): RegFile3[A] = stage(RegFileNew[A,RegFile3](Seq(d0,d1,d2),None))
}


/** A 1-dimensional register file ([[RegFile]]) with elements of type [[A]] **/
@ref class RegFile1[A:Bits] extends RegFile[A,RegFile1] with Ref[Array[Any],RegFile1[A]] {
  def rank: Int = 1
  //val evMem = implicitly[RegFile1[A] <:< LocalMem[A,RegFile1]]

  /** Shifts in `data` into the first register, shifting all other values over by one position. **/
  @api def <<=(data: A): Void = stage(RegFileShiftIn(this,data,Seq(I32(0)),Set.empty,0))

  /** Shifts in `data` into the first N registers, where N is the size of the given [[Vec]].
    * All other elements are shifted by N positions.
    */
  //@api def <<=(data: Vec[A]): Void = stage(RegFileVectorShiftIn(this, data, Seq(I32(0)), Set.empty, 0))

  /** Creates a dense, burst load from the specified region of DRAM to this on-chip memory. **/
  //@api def load(dram: DRAM1[T]): MUnit = DRAMTransfers.dense_transfer(dram.toTile(ranges), this, isLoad = true)

  /** Creates a dense, burst load from the specified region of DRAM to this on-chip memory. **/
  //@api def load(dram: DRAMDenseTile1[T]): MUnit = DRAMTransfers.dense_transfer(dram, this, isLoad = true)
}


/** A 2-dimensional register file ([[RegFile]]) with elements of type [[A]] **/
@ref class RegFile2[A:Bits] extends RegFile[A,RegFile2] with Ref[Array[Any],RegFile2[A]] {
  def rank: Int = 2

}


/** A 3-dimensional register file ([[RegFile]]) with elements of type [[A]] **/
@ref class RegFile3[A:Bits] extends RegFile[A,RegFile3] with Ref[Array[Any],RegFile3[A]] {
  def rank: Int = 3
}

