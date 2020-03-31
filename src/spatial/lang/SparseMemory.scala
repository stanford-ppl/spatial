package spatial.lang

import argon._
import argon.node._
import emul.FixedPoint
import forge.tags._
import utils.implicits.collections._
import spatial.node._
import spatial.lang.types._
import spatial.metadata.memory._


abstract class SparseSRAM[A:Bits,C[T]](implicit val evMem: C[A] <:< SparseSRAM[A,C]) extends LocalMem[A,C] {
  val A: Bits[A] = Bits[A]
  protected def M1: Type[SparseSRAM1[A]] = implicitly[Type[SparseSRAM1[A]]]
  def rank: Int
  /** Returns the total capacity (in elements) of this SparseSRAM. */
  @api def size: I32 = product(dims:_*)
  /** Returns the dimensions of this SparseSRAM as a Sequence. */
  @api def dims: Seq[I32] = Seq.tabulate(rank){d => stage(MemDim(this,d)) }
  /** Returns dim0 of this DRAM, or else 1 if SparseSRAM is lower dimensional */
  @api def dim0: I32 = dims.indexOrElse(0, I32(1))
  /** Returns dim1 of this DRAM, or else 1 if SparseSRAM is lower dimensional */
  @api def dim1: I32 = dims.indexOrElse(1, I32(1))
  /** Returns dim2 of this DRAM, or else 1 if SparseSRAM is lower dimensional */
  @api def dim2: I32 = dims.indexOrElse(2, I32(1))
  /** Returns dim3 of this DRAM, or else 1 if SparseSRAM is lower dimensional */
  @api def dim3: I32 = dims.indexOrElse(3, I32(1))
  /** Returns dim4 of this DRAM, or else 1 if SparseSRAM is lower dimensional */
  @api def dim4: I32 = dims.indexOrElse(4, I32(1))

  /** Creates an alias of this SparseSRAM with parallel access in the last dimension. */
  @api def par(p: I32): C[A] = {
    implicit val C: Type[C[A]] = this.selfType
    val ds = this.dims
    val ranges: Seq[Series[I32]] = ds.dropRight(1).map{i => Series(I32(0),i,I32(1),I32(1)) } :+ (ds.last par p)
    stage(MemDenseAlias(me,ranges))
  }

  /** Returns the value at `addr`.
    * The number of indices should match the SparseSRAM's rank.
    * NOTE: Use the apply method if the SparseSRAM's rank is statically known.
    */
  @api def read(addr: Seq[Idx], bs: Seq[BarrierTransaction] = Seq(), ens: Set[Bit] = Set.empty): A = {
    checkDims(addr.length)
    stage(SparseSRAMRead[A,C](me,addr,bs,ens))
  }

  /** Updates the value at `addr` to `data`.
    * The number of indices should match the SparseSRAM's rank.
    * NOTE: Use the update method if the SparseSRAM's rank is statically known.
    */
  @api def write(data: A, addr: Seq[Idx], bs: Seq[BarrierTransaction] = Seq(), ens: Set[Bit] = Set.empty): Void = {
    checkDims(addr.length)
    stage(SparseSRAMWrite[A,C](me,data,addr,bs,ens))
  }

  @rig private def checkDims(given: Int): Unit = {
    if (given != rank) {
      error(ctx, s"Expected a $rank-dimensional address for $this (${this.name}), got a $given-dimensional address.")
      error(ctx)
    }
  }

  /** Indicate that the memory should be buffered and ignore
    * potential situation where result from running sequentially
    * does not match with resurt from running pipelined
    */
  def buffer: C[A] = { this.isWriteBuffer = true; me }
  /** Do not buffer memory */
  def nonbuffer: C[A] = { this.isNonBuffer = true; me }
  def mustmerge: C[A] = { this.isMustMerge = true; me }

  /** Do not remove this memory or accesses to this memory, even if anything appears unused*/
  def dontTouch: C[A] = { this.keepUnused = true; me }

  def effort(e: Int): C[A] = { this.bankingEffort = e; me }
  /** Allow "unsafe" banking, where two writes can technically happen simultaneously and one will be dropped.
    * Use in cases where writes may happen in parallel but you are either sure that two writes won't happen simultaneously
    * due to data-dependent control flow or that you don't care if one write gets dropped
    */
  def conflictable: C[A] = { this.shouldIgnoreConflicts = true; me }


  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = read(addr, Seq(), ens)
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void = write(data, addr, Seq(), ens)
  @rig def __reset(ens: Set[Bit]): Void = void
}
object SparseSRAM {
  /** Allocates a 1-dimensional [[SparseSRAM1]] with capacity of `length` elements of type A. */
  @api def apply[A:Bits](length: I32): SparseSRAM1[A] = 
    stage(SparseSRAMNew[A,SparseSRAM1](Seq(length))).conflictable.mustmerge
}

object SparseDRAM {
  @api def apply[A:Bits](par:scala.Int)(length: I32): SparseSRAM1[A] = 
    stage(SparseDRAMNew[A,SparseSRAM1](Seq(length), par)).conflictable.mustmerge
}

/** A 1-dimensional SparseSRAM with elements of type A. */
@ref class SparseSRAM1[A:Bits]
      extends SparseSRAM[A,SparseSRAM1]
         with LocalMem1[A,SparseSRAM1]
         with Mem1[A,SparseSRAM1]
         with ReadMem1[A]
         with Ref[Array[Any],SparseSRAM1[A]] {

  def rank: Int = 1
  @api def length: I32 = dims.head
  @api override def size: I32 = dims.head

  /** Returns the value at `pos`. */
  @api def apply(pos: I32): A = stage(SparseSRAMRead(this,Seq(pos),Seq(),Set.empty))
  @api def barrierRead(pos: I32, bs: Seq[BarrierTransaction]): A = stage(SparseSRAMRead(this,Seq(pos),bs,Set.empty))

  @api def RMW(pos: I32, data: A, op: String, order: String, bs: Seq[BarrierTransaction] = Seq()): A = {
    warn(s"Currently no syntax for input token to RMW, but it can be added.")
    stage(SparseSRAMRMW(this,data,Seq(pos),op,order,bs,Set.empty))
  }
  /** Updates the value at `pos` to `data`. */
  @api def update(pos: I32, data: A): Void = stage(SparseSRAMWrite(this,data,Seq(pos),Seq(), Set.empty))
  @api def barrierWrite(pos: I32, data: A, bs: Seq[BarrierTransaction]): Void = stage(SparseSRAMWrite(this, data, Seq(pos), bs, Set.empty))

}
