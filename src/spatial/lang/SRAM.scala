package spatial.lang

import argon._
import forge.tags._
import utils.implicits.collections._
import spatial.node._
import spatial.lang.types._
import spatial.metadata.memory._

abstract class SRAM[A:Bits,C[T]](implicit val evMem: C[A] <:< SRAM[A,C]) extends LocalMem[A,C] with TensorMem[A] {
  val A: Bits[A] = Bits[A]
  protected def M1: Type[SRAM1[A]] = implicitly[Type[SRAM1[A]]]
  protected def M2: Type[SRAM2[A]] = implicitly[Type[SRAM2[A]]]
  protected def M3: Type[SRAM3[A]] = implicitly[Type[SRAM3[A]]]
  protected def M4: Type[SRAM4[A]] = implicitly[Type[SRAM4[A]]]
  protected def M5: Type[SRAM5[A]] = implicitly[Type[SRAM5[A]]]

  def rank: Int
  @api def dims: Seq[I32] = Seq.tabulate(rank){d => stage(MemDim(this,d)) }

  /** Creates an alias of this SRAM with parallel access in the last dimension. */
  @api def par(p: I32): C[A] = {
    implicit val C: Type[C[A]] = this.selfType
    val ds = this.dims
    val ranges: Seq[Series[I32]] = ds.dropRight(1).map{i => Series(I32(0),i,I32(1),I32(1)) } :+ (ds.last par p)
    stage(MemDenseAlias(me,ranges))
  }

  /** Returns the value at `addr`.
    * The number of indices should match the SRAM's rank.
    * NOTE: Use the apply method if the SRAM's rank is statically known.
    */
  @api def read(addr: Seq[ICTR], ens: Set[Bit] = Set.empty): A = {
    checkDims(addr.length)
    stage(SRAMRead[A,C](me,addr,ens))
  }

  /** Updates the value at `addr` to `data`.
    * The number of indices should match the SRAM's rank.
    * NOTE: Use the update method if the SRAM's rank is statically known.
    */
  @api def write(data: A, addr: Seq[ICTR], ens: Set[Bit] = Set.empty): Void = {
    checkDims(addr.length)
    stage(SRAMWrite[A,C](me,data,addr,ens))
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
  /** Only attempt to bank memory hierarchically */
  def hierarchical: C[A] = { this.isNoFlatBank = true; me }
  /** Only attempt to bank memory in a flattened manner */
  def flat: C[A] = { this.isNoHierarchicalBank = true; me }
  /** Guarantee that it is safe to merge different duplicates. 
    * Only use this if you know exactly what you are doing! 
    */
  def mustmerge: C[A] = { this.isMustMerge = true; me }

  def dualportedread: C[A] = { this.isDualPortedRead = true; me}
  def dualportedwrite: C[A] = { throw new Exception(s"Memories with Dual Write Ports are currently not supported.  They can be implemented pretty easily, but we have not needed them yet.")}

  def nohierarchical: C[A] = {throw new Exception(s".nohierarchical has been deprecated.  Please use .flat instead")}
  def noflat: C[A] = {throw new Exception(s".noflat has been deprecated.  Please use .hierarchical instead")}
  def nobank: C[A] = {throw new Exception(s".nobank has been deprecated.  Please use .fullfission instead")}
  /** Only attempt to bank with N's from the "pow2" category */
  def nPow2: C[A] = {throw new Exception(s".nPow2 has been deprecated.  Please use .nBest instead")}
  /** Only attempt to bank with alphas from the "pow2" category */
  def alphaPow2: C[A] = {throw new Exception(s".alphaPow2 has been deprecated.  Please use .alphaBest instead")}

  /** Do not attempt to bank memory at all, and only use bank-by-duplication for all lanes of all readers */
  def fullfission: C[A] = { this.isFullFission = true; me }
  /** Attempt to duplicate on the provided axes groups.  
    *   i.e. To try either no-duplication, full-duplication, or duplication
    *   along the axes with dimensions 32 and 64 for SRAM(32,8,64), use the flag
    *   .duplicateaxes( List( List(), List(0,2), List(0,1,2) ) )
    */
  @stateful def axesfission(opts: Seq[Seq[Int]]): C[A] = {this.bankingEffort = 3.max(this.bankingEffort); this.duplicateOnAxes = opts; me }
  /** Number of valid schemes to find before quitting a region */
//  def quitAfter(x: Int): C[A] = { this.quitAfter = x; me }
  /** Do not attempt to bank memory by duplication */
  def nofission: C[A] = { this.isNoFission = true; me }
  /** Only attempt to bank with N's from the "likely" category */
  def nBest: C[A] = { this.nConstraints = this.nConstraints :+ NBestGuess; me }
  /** Only attempt to bank with N's from the "relaxed" category */
  def nRelaxed: C[A] = { this.nConstraints = this.nConstraints :+ NRelaxed; me }
  /** Only attempt to bank with alphas from the "likely" category */
  def alphaBest: C[A] = { this.alphaConstraints = this.alphaConstraints :+ AlphaBestGuess; me }
  /** Only attempt to bank with alphas from the "relaxed" category */
  def alphaRelaxed: C[A] = { this.alphaConstraints = this.alphaConstraints :+ AlphaRelaxed; me }
  /** Do not attempt to bank memory with block-cyclic schemes */
  def noblockcyclic: C[A] = { this.noBlockCyclic = true; me }
  /** Only attempt to bank memory with block-cyclic schemes */
  def onlyblockcyclic: C[A] = { this.onlyBlockCyclic = true; me }
  /** Set search range bs to search for */
  def blockcyclic_Bs(bs:Seq[Int]): C[A] = { this.blockCyclicBs = bs; me }
  /** Specify banking search effort for this memory */
  def effort(e: Int): C[A] = { this.bankingEffort = e; me }
  /** Allow "unsafe" banking, where two writes can technically happen simultaneously and one will be dropped.
    * Use in cases where writes may happen in parallel but you are either sure that two writes won't happen simultaneously
    * due to data-dependent control flow or that you don't care if one write gets dropped
    */
  def conflictable: C[A] = { this.shouldIgnoreConflicts = true; me }
  /** Provide explicit banking scheme that you want to use.  If this scheme is unsafe, it will crash. It will also assume only one duplicate */
  def bank(N: Seq[Int], B: Seq[Int], alpha: Seq[Int], P: Option[Seq[Int]] = None): C[A] = { this.explicitBanking = (N, B, alpha, P); me }
  /** Provide explicit banking scheme that you want to use.  If this scheme is unsafe, it will NOT crash. It will also assume only one duplicate */
  def forcebank(N: Seq[Int], B: Seq[Int], alpha: Seq[Int], P: Option[Seq[Int]] = None): C[A] = { this.explicitBanking = (N, B, alpha, P); this.forceExplicitBanking = true; me }

  def coalesce: C[A] = { this.shouldCoalesce = true; me }

  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = read(addr.map(_.asInstanceOf[ICTR]), ens)
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void = write(data, addr.map(_.asInstanceOf[ICTR]), ens)
  @rig def __reset(ens: Set[Bit]): Void = void
}
object SRAM {
  /** Allocates a 1-dimensional [[SRAM1]] with capacity of `length` elements of type A. */
  @api def apply[A:Bits](length: I32): SRAM1[A] = stage(SRAMNew[A,SRAM1](Seq(length)))

  /** Allocates a 2-dimensional [[SRAM2]] with `rows` x `cols` elements of type A. */
  @api def apply[A:Bits](rows: I32, cols: I32): SRAM2[A] = stage(SRAMNew[A,SRAM2](Seq(rows,cols)))

  /** Allocates a 3-dimensional [[SRAM3]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32): SRAM3[A] = stage(SRAMNew[A,SRAM3](Seq(d0,d1,d2)))

  /** Allocates a 4-dimensional [[SRAM4]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32, d3: I32): SRAM4[A] = stage(SRAMNew[A,SRAM4](Seq(d0,d1,d2,d3)))

  /** Allocates a 5-dimensional [[SRAM5]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32, d3: I32, d4: I32): SRAM5[A] = stage(SRAMNew[A,SRAM5](Seq(d0,d1,d2,d3,d4)))
}

/** A 1-dimensional SRAM with elements of type A. */
@ref class SRAM1[A:Bits]
      extends SRAM[A,SRAM1]
         with LocalMem1[A,SRAM1]
         with Mem1[A,SRAM1]
         with ReadMem1[A]
         with Ref[Array[Any],SRAM1[A]] {

  def rank: Int = 1
  @api def length: I32 = dims.head
  @api override def size: I32 = dims.head

  /** Returns the value at `pos`. */
  @api def apply(pos: ICTR): A = stage(SRAMRead(this,Seq(pos),Set.empty))

  /** Updates the value at `pos` to `data`. */
  @api def update(pos: ICTR, data: A): Void = stage(SRAMWrite(this,data,Seq(pos),Set.empty))

}

/** A 2-dimensional SRAM with elements of type A. */
@ref class SRAM2[A:Bits]
      extends SRAM[A,SRAM2]
         with LocalMem2[A,SRAM2]
         with Mem2[A,SRAM1,SRAM2]
         with ReadMem2[A]
         with Ref[Array[Any],SRAM2[A]] {
  def rank: Int = 2
  @api def rows: I32 = dims.head
  @api def cols: I32 = dim1

  /** Returns the value at (`row`, `col`). */
  @api def apply(row: ICTR, col: ICTR): A = stage(SRAMRead(this,Seq(row,col),Set.empty))

  /** Updates the value at (`row`,`col`) to `data`. */
  @api def update(row: ICTR, col: ICTR, data: A): Void = stage(SRAMWrite(this, data, Seq(row,col), Set.empty))

}

/** A 3-dimensional SRAM with elements of type A. */
@ref class SRAM3[A:Bits]
      extends SRAM[A,SRAM3]
         with LocalMem3[A,SRAM3]
         with ReadMem3[A]
         with Mem3[A,SRAM1,SRAM2,SRAM3]
         with Ref[Array[Any],SRAM3[A]] {

  def rank: Int = 3

  /** Returns the value at (`d0`,`d1`,`d2`). */
  @api def apply(d0: ICTR, d1: ICTR, d2: ICTR): A = stage(SRAMRead(this,Seq(d0,d1,d2),Set.empty))

  /** Updates the value at (`d0`,`d1`,`d2`) to `data`. */
  @api def update(d0: ICTR, d1: ICTR, d2: ICTR, data: A): Void = stage(SRAMWrite(this,data,Seq(d0,d1,d2), Set.empty))


}

/** A 4-dimensional SRAM with elements of type A. */
@ref class SRAM4[A:Bits]
      extends SRAM[A,SRAM4]
         with LocalMem4[A,SRAM4]
         with ReadMem4[A]
         with Mem4[A,SRAM1,SRAM2,SRAM3,SRAM4]
         with Ref[Array[Any],SRAM4[A]] {

  def rank: Int = 4

  /** Returns the value at (`d0`,`d1`,`d2`,`d3`). */
  @api def apply(d0: ICTR, d1: ICTR, d2: ICTR, d3: ICTR): A = stage(SRAMRead(this,Seq(d0,d1,d2,d3),Set.empty))

  /** Updates the value at (`d0`,`d1`,`d2`,`d3`) to `data`. */
  @api def update(d0: ICTR, d1: ICTR, d2: ICTR, d3: ICTR, data: A): Void = stage(SRAMWrite(this, data, Seq(d0,d1,d2,d3), Set.empty))

}

/** A 5-dimensional SRAM with elements of type A. */
@ref class SRAM5[A:Bits]
      extends SRAM[A,SRAM5]
         with LocalMem5[A,SRAM5]
         with ReadMem5[A]
         with Mem5[A,SRAM1,SRAM2,SRAM3,SRAM4,SRAM5]
         with Ref[Array[Any],SRAM5[A]] {

  def rank: Int = 5

  /** Returns the value at (`d0`,`d1`,`d2`,`d3`,`d4`). */
  @api def apply(d0: ICTR, d1: ICTR, d2: ICTR, d3: ICTR, d4: ICTR): A = stage(SRAMRead(this,Seq(d0,d1,d2,d3,d4),Set.empty))

  /** Updates the value at (`d0`,`d1`,`d2`,`d3`,`d4`) to `data`. */
  @api def update(d0: ICTR, d1: ICTR, d2: ICTR, d3: ICTR, d4: ICTR, data: A): Void = stage(SRAMWrite(this, data, Seq(d0,d1,d2,d3,d4), Set.empty))

}




