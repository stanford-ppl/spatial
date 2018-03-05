package spatial.lang

import core._
import forge.tags._
import utils.implicits.collections._

import spatial.node._

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
  @api def apply(addr: Idx*): A = stage(RegFileRead(this,addr,Set.empty))


  /** Updates the value at `addr` to `data`.
    * The number of indices should match the rank of this RegFile.
    */
  @curriedUpdate
  def update(addr: Idx*)(data: A)(implicit ctx: SrcCtx, state: State): Void = {
    stage(RegFileWrite(this,addr,data,Set.empty))
  }
}

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

  /** Resets this RegFile to its initial values (or zeros, if unspecified). **/
  @api def reset: Void = stage(RegFileReset(this, Set.empty))

  /** Conditionally resets this RegFile based on `cond` to its inital values (or zeros if unspecified). **/
  @api def reset(cond: Bit): Void = stage(RegFileReset(this, Set(cond)))
}