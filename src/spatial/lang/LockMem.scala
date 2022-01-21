package spatial.lang

import argon._
import forge.tags._
import utils.implicits.collections._
import spatial.node._
import spatial.lang.types._
import spatial.metadata.memory._


abstract class LockDRAM[A:Bits,C[T]](implicit val evMem: C[A] <:< LockDRAM[A,C]) extends Top[C[A]] with RemoteMem[A,C] {
  val A: Bits[A] = Bits[A]

  protected def M1: Type[LockDRAM1[A]] = implicitly[Type[LockDRAM1[A]]]
  def rank: Seq[Int]

  /** Returns the total capacity (in elements) of this DRAM. */
  @api def size: I32 = product(dims:_*)

  /**
    * Returns the dimensions of this DRAM as a Sequence.
    */
  @api def dims: Seq[I32] = Seq.tabulate(rank.length){d => stage(MemDim(this,rank(d))) }

  /** Returns dim0 of this DRAM, or else 1 if DRAM is lower dimensional */
  @api def dim0: I32 = dims.head

  /** Returns the 64-bit address of this DRAM */
  @api def address: I64 = stage(LockDRAMAddress(me))

  @api override def neql(that: C[A]): Bit = {
    error(this.ctx, "Native comparison of DRAMs is unsupported. Use getMem to extract data.")
    error(this.ctx)
    super.neql(that)
  }
  @api override def eql(that: C[A]): Bit = {
    error(this.ctx, "Native comparision of DRAMs is unsupported. Use getMem to extract data.")
    error(this.ctx)
    super.eql(that)
  }
}
object LockDRAM {
  /** Allocates a 1-dimensional [[LockDRAM1]] with capacity of `length` elements of type A. */
  @api def apply[A:Bits](length: I32): LockDRAM1[A] = stage(LockDRAMHostNew[A,LockDRAM1](Seq(length),zero[A]))
}

/** A 1-dimensional [[LockDRAM]] with elements of type A. */
@ref class LockDRAM1[A:Bits] extends LockDRAM[A,LockDRAM1] with Ref[Array[Any],LockDRAM1[A]] with Mem1[A,LockDRAM1] {
  def rank: Seq[Int] = Seq(0)
  @api def length: I32 = dims.head
  @api override def size: I32 = dims.head

  /** Returns the value at `pos`. */
  @api def apply(pos: I32): A = stage(LockDRAMRead(this,Seq(pos),None,Set.empty))
  @api def apply(pos: I32, lock: LockWithKeys[I32]): A = stage(LockDRAMRead(this,Seq(pos),Some(lock),Set.empty))

  /** Updates the value at `pos` to `data`. */
  @api def update(pos: I32, data: A): Void = stage(LockDRAMWrite(this,data,Seq(pos),None,Set.empty))
  @api def update(pos: I32, lock: LockWithKeys[I32], data: A): Void = stage(LockDRAMWrite(this,data,Seq(pos),Some(lock),Set.empty))

  @api def store[Local[T]<:LocalMem1[T,Local]](local: Local[A])(implicit tp: Type[Local[A]]): Void = throw new Exception(s"LockDRAM store needs to be implemented!")
  @api def store(local: SRAM1[A], len: I32): Void = throw new Exception(s"LockDRAM store needs to be implemented!")
}

abstract class LockSRAM[A:Bits,C[T]](implicit val evMem: C[A] <:< LockSRAM[A,C]) extends LocalMem[A,C] {
  val A: Bits[A] = Bits[A]
  protected def M1: Type[LockSRAM1[A]] = implicitly[Type[LockSRAM1[A]]]
  def rank: Int
  /** Returns the total capacity (in elements) of this LockSRAM. */
  @api def size: I32 = product(dims:_*)
  /** Returns the dimensions of this LockSRAM as a Sequence. */
  @api def dims: Seq[I32] = Seq.tabulate(rank){d => stage(MemDim(this,d)) }
  /** Returns dim0 of this DRAM, or else 1 if LockSRAM is lower dimensional */
  @api def dim0: I32 = dims.indexOrElse(0, I32(1))
  /** Returns dim1 of this DRAM, or else 1 if LockSRAM is lower dimensional */
  @api def dim1: I32 = dims.indexOrElse(1, I32(1))
  /** Returns dim2 of this DRAM, or else 1 if LockSRAM is lower dimensional */
  @api def dim2: I32 = dims.indexOrElse(2, I32(1))
  /** Returns dim3 of this DRAM, or else 1 if LockSRAM is lower dimensional */
  @api def dim3: I32 = dims.indexOrElse(3, I32(1))
  /** Returns dim4 of this DRAM, or else 1 if LockSRAM is lower dimensional */
  @api def dim4: I32 = dims.indexOrElse(4, I32(1))

  /** Creates an alias of this LockSRAM with parallel access in the last dimension. */
  @api def par(p: I32): C[A] = {
    implicit val C: Type[C[A]] = this.selfType
    val ds = this.dims
    val ranges: Seq[Series[I32]] = ds.dropRight(1).map{i => Series(I32(0),i,I32(1),I32(1)) } :+ (ds.last par p)
    stage(MemDenseAlias(me,ranges))
  }

  /** Returns the value at `addr`.
    * The number of indices should match the LockSRAM's rank.
    * NOTE: Use the apply method if the LockSRAM's rank is statically known.
    */
  @api def read(addr: Seq[Idx], lock: Option[LockWithKeys[I32]] = None, ens: Set[Bit] = Set.empty): A = {
    checkDims(addr.length)
    stage(LockSRAMRead[A,C](me,addr,lock,ens))
  }

  /** Updates the value at `addr` to `data`.
    * The number of indices should match the LockSRAM's rank.
    * NOTE: Use the update method if the LockSRAM's rank is statically known.
    */
  @api def write(data: A, addr: Seq[Idx], lock: Option[LockWithKeys[I32]], ens: Set[Bit] = Set.empty): Void = {
    checkDims(addr.length)
    stage(LockSRAMWrite[A,C](me,data,addr,lock,ens))
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

  def effort(e: Int): C[A] = { this.bankingEffort = e; me }
  /** Allow "unsafe" banking, where two writes can technically happen simultaneously and one will be dropped.
    * Use in cases where writes may happen in parallel but you are either sure that two writes won't happen simultaneously
    * due to data-dependent control flow or that you don't care if one write gets dropped
    */
  def conflictable: C[A] = { this.shouldIgnoreConflicts = Range(0, rank).toSet; me }


  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = read(addr, None, ens)
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void = write(data, addr, None, ens)
  @rig def __reset(ens: Set[Bit]): Void = void
}
object LockSRAM {
  /** Allocates a 1-dimensional [[LockSRAM1]] with capacity of `length` elements of type A. */
  @api def apply[A:Bits](length: I32): LockSRAM1[A] = stage(LockSRAMNew[A,LockSRAM1](Seq(length))).conflictable.mustmerge
}

/** A 1-dimensional LockSRAM with elements of type A. */
@ref class LockSRAM1[A:Bits]
      extends LockSRAM[A,LockSRAM1]
         with LocalMem1[A,LockSRAM1]
         with Mem1[A,LockSRAM1]
         with ReadMem1[A]
         with Ref[Array[Any],LockSRAM1[A]] {

  def rank: Int = 1
  @api def length: I32 = dims.head
  @api override def size: I32 = dims.head

  /** Returns the value at `pos`. */
  @api def apply(pos: I32): A = stage(LockSRAMRead(this,Seq(pos),None,Set.empty))
  @api def apply(pos: I32, lock: LockWithKeys[I32]): A = stage(LockSRAMRead(this,Seq(pos),Some(lock),Set.empty))

  /** Updates the value at `pos` to `data`. */
  @api def update(pos: I32, data: A): Void = stage(LockSRAMWrite(this,data,Seq(pos),None,Set.empty))
  @api def update(pos: I32, lock: LockWithKeys[I32], data: A): Void = stage(LockSRAMWrite(this,data,Seq(pos),Some(lock),Set.empty))

}


@ref class Lock[A:Bits] extends Top[Lock[A]] with Ref[scala.Array[Any],Lock[A]] {
  val A: Bits[A] = Bits[A]
  override val __neverMutable = true

  @api def lock(elements: A*): LockWithKeys[A] = stage(LockOnKeys[A](this, elements))
}

@ref class LockWithKeys[A:Bits] extends Top[LockWithKeys[A]] with Ref[scala.Array[Any],LockWithKeys[A]] {
  val A: Bits[A] = Bits[A]
  override val __neverMutable = true
}

object Lock {
  /** Allocates a Lock module */
  @api def apply[A:Bits](depth: I32): Lock[A] = stage(LockNew[A](depth))
}
