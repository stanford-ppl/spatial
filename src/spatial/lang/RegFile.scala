package spatial.lang

import argon._
import forge.tags._
import utils.implicits.collections._

import spatial.node._
import spatial.lang.types._

/** An N-dimensional register file */
abstract class RegFile[A:Bits,C[T]](implicit val evMem: C[A] <:< RegFile[A,C]) extends LocalMem[A,C] {
  val A: Bits[A] = Bits[A]
  protected def M1: Type[RegFile1[A]] = implicitly[Type[RegFile1[A]]]
  protected def M2: Type[RegFile2[A]] = implicitly[Type[RegFile2[A]]]
  protected def M3: Type[RegFile3[A]] = implicitly[Type[RegFile3[A]]]
  def rank: Int
  @api def size: I32 = product(dims:_*)
  @api def dims: Seq[I32] = Seq.tabulate(rank){d => stage(MemDim(this,d)) }
  @api def dim0: I32 = dims.head
  @api def dim1: I32 = dims.indexOrElse(1, I32(1))
  @api def dim2: I32 = dims.indexOrElse(2, I32(1))

  /** Resets this RegFile to its initial values (or zeros, if unspecified). */
  @api def reset: Void = stage(RegFileReset(this, Set.empty))

  /** Conditionally resets this RegFile based on `cond` to its inital values (or zeros if unspecified). */
  @api def reset(cond: Bit): Void = stage(RegFileReset(this, Set(cond)))

  /** Returns the value at `addr`.
    * The number of indices should match the RegFile's rank.
    */
  @api def read(addr: Seq[Idx], ens: Set[Bit] = Set.empty): A = {
    checkDims(addr.length)
    stage(RegFileRead[A,C](me,addr,Set.empty))
  }

  /** Updates the value at `addr` to `data`.
    * The number of indices should match the RegFile's rank.
    */
  @api def write(data: A, addr: Seq[Idx], ens: Set[Bit] = Set.empty): Void = {
    checkDims(addr.length)
    stage(RegFileWrite[A,C](me,data,addr,Set.empty))
  }

  @rig private def checkDims(given: Int): Unit = {
    if (given != rank) {
      error(ctx, s"Expected a $rank-dimensional address, got a $given-dimensional address.")
      error(ctx)
    }
  }

  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = read(addr, ens)
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void = write(data, addr, ens)
  @rig def __reset(ens: Set[Bit]): Void = void
}
object RegFile {
  /** Allocates a [[RegFile1]] with capacity for `length` elements of type A. */
  @api def apply[A:Bits](length: I32): RegFile1[A] = stage(RegFileNew[A,RegFile1](Seq(length),None))

  /** Allocates a [[RegFile2]] with size `rows` x `cols` and elements of type A. */
  @api def apply[A:Bits](rows: I32, cols: I32): RegFile2[A] = stage(RegFileNew[A,RegFile2](Seq(rows,cols),None))

  /** Allocates a [[RegFile3]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32): RegFile3[A] = stage(RegFileNew[A,RegFile3](Seq(d0,d1,d2),None))
}


/** A 1-dimensional register file (RegFile) with elements of type A */
@ref class RegFile1[A:Bits]
      extends RegFile[A,RegFile1]
         with LocalMem1[A,RegFile1]
         with Mem1[A,RegFile1]
         with Ref[Array[Any],RegFile1[A]] {

  def rank: Int = 1
  @api override def size: I32 = dims.head
  @api def length: I32 = dims.head

  /** Shifts in `data` into the first register, shifting all other values over by one position. */
  @api def <<=(data: A): Void = stage(RegFileShiftIn(this,data,Seq(I32(0)),Set.empty,0))

  /** Returns the value at `pos`. */
  @api def apply(pos: Idx): A = stage(RegFileRead(this,Seq(pos),Set.empty))

  /** Updates the value at `pos` to `data`. */
  @api def update(pos: Idx, data: A): Void = stage(RegFileWrite(this,data,Seq(pos),Set.empty))


  /** Shifts in `data` into the first N registers, where N is the size of the given [[Vec]].
    * All other elements are shifted by N positions.
    */
  //@api def <<=(data: Vec[A]): Void = stage(RegFileVectorShiftIn(this, data, Seq(I32(0)), Set.empty, 0))
}


/** A 2-dimensional register file (RegFile) with elements of type A */
@ref class RegFile2[A:Bits]
      extends RegFile[A,RegFile2]
         with LocalMem2[A,RegFile2]
         with Mem2[A,RegFile1,RegFile2]
         with Ref[Array[Any],RegFile2[A]] {

  def rank: Int = 2
  @api def rows: I32 = dims.head
  @api def cols: I32 = dim1

  /** Returns the value at (`row`, `col`). */
  @api def apply(row: Idx, col: Idx): A = stage(RegFileRead(this,Seq(row,col),Set.empty))

  /** Updates the value at (`row`,`col`) to `data`. */
  @api def update(row: Idx, col: Idx, data: A): Void = stage(RegFileWrite(this, data, Seq(row,col), Set.empty))

}


/** A 3-dimensional register file (RegFile) with elements of type A */
@ref class RegFile3[A:Bits]
      extends RegFile[A,RegFile3]
         with LocalMem3[A,RegFile3]
         with Mem3[A,RegFile1,RegFile2,RegFile3]
         with Ref[Array[Any],RegFile3[A]] {
  def rank: Int = 3

  /** Returns the value at (`d0`,`d1`,`d2`). */
  @api def apply(d0: Idx, d1: Idx, d2: Idx): A = stage(RegFileRead(this,Seq(d0,d1,d2),Set.empty))

  /** Updates the value at (`d0`,`d1`,`d2`) to `data`. */
  @api def update(d0: Idx, d1: Idx, d2: Idx, data: A): Void = stage(RegFileWrite(this,data,Seq(d0,d1,d2), Set.empty))

}

