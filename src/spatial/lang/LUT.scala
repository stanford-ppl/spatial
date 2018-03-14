package spatial.lang

import core._
import forge.tags._
import utils.implicits.collections._

import spatial.node._
import spatial.lang.types._

abstract class LUT[A:Bits,C[T]](implicit val evMem: C[A] <:< LUT[A,C]) extends LocalMem[A,C] {
  val tA: Bits[A] = Bits[A]
  protected def M1: Type[LUT1[A]] = implicitly[Type[LUT1[A]]]
  protected def M2: Type[LUT2[A]] = implicitly[Type[LUT2[A]]]
  protected def M3: Type[LUT3[A]] = implicitly[Type[LUT3[A]]]
  protected def M4: Type[LUT4[A]] = implicitly[Type[LUT4[A]]]
  protected def M5: Type[LUT5[A]] = implicitly[Type[LUT5[A]]]
  def rank: Int
  /** Returns the total capacity (in elements) of this LUT. */
  @api def size: I32 = product(dims:_*)

  /** Returns the dimensions of this LUT as a Sequence. */
  @api def dims: Seq[I32] = Seq.tabulate(rank){d => stage(MemDim(this,d)) }
  @api def dim0: I32 = dims.indexOrElse(0, I32(1))
  @api def dim1: I32 = dims.indexOrElse(1, I32(1))
  @api def dim2: I32 = dims.indexOrElse(2, I32(1))
  @api def dim3: I32 = dims.indexOrElse(3, I32(1))
  @api def dim4: I32 = dims.indexOrElse(4, I32(1))

  /** Returns the value at `addr`.
    * The number of indices should match the LUT's rank.
    * NOTE: Use the apply method if the LUT's rank is statically known.
    */
  @api def read(addr: Seq[Idx]): A = {
    checkDims(addr.length)
    stage(LUTRead[A,C](me,addr,Set.empty))
  }

  @rig private def checkDims(given: Int): Unit = {
    if (given != rank) {
      error(ctx, s"Expected a $rank-dimensional address, got a $given-dimensional address.")
      error(ctx)
    }
  }

  // --- Typeclass Methods
}
object LUT {
  /** Allocates a 1-dimensional [[LUT1]] with capacity of `length` elements of type A. */
  @api def apply[A:Bits](length: Int)(elems: Bits[A]*): LUT1[A] = {
    stage(LUTNew[A,LUT1](Seq(length),elems))
  }

  /** Allocates a 2-dimensional [[LUT2]] with `rows` x `cols` elements of type A. */
  @api def apply[A:Bits](rows: Int, cols: Int)(elems: Bits[A]*): LUT2[A] = {
    stage(LUTNew[A,LUT2](Seq(rows,cols), elems))
  }

  /** Allocates a 3-dimensional [[LUT3]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: Int, d1: Int, d2: Int)(elems: Bits[A]*): LUT3[A] = {
    stage(LUTNew[A,LUT3](Seq(d0,d1,d2), elems))
  }

  /** Allocates a 4-dimensional [[LUT4]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: Int, d1: Int, d2: Int, d3: Int)(elems: Bits[A]*): LUT4[A] = {
    stage(LUTNew[A,LUT4](Seq(d0,d1,d2,d3), elems))
  }

  /** Allocates a 5-dimensional [[LUT5]] with the given dimensions and elements of type A. */
  @api def apply[A:Bits](d0: Int, d1: Int, d2: Int, d3: Int, d4: Int)(elems: Bits[A]*): LUT5[A] = {
    stage(LUTNew[A,LUT5](Seq(d0,d1,d2,d3,d4), elems))
  }
}

/** A 1-dimensional LUT with elements of type A. */
@ref class LUT1[A:Bits]
  extends LUT[A,LUT1]
    with LocalMem1[A,LUT1]
    with Mem1[A,LUT1]
    with Ref[Array[Any],LUT1[A]] {

  def rank: Int = 1
  @api def length: I32 = dims.head
  @api override def size: I32 = dims.head

  /** Returns the value at `pos`. */
  @api def apply(pos: Idx): A = stage(LUTRead(this,Seq(pos),Set.empty))
}

/** A 2-dimensional LUT with elements of type A. */
@ref class LUT2[A:Bits]
  extends LUT[A,LUT2]
    with LocalMem2[A,LUT2]
    with Mem2[A,LUT1,LUT2]
    with Ref[Array[Any],LUT2[A]] {
  def rank: Int = 2
  @api def rows: I32 = dims.head
  @api def cols: I32 = dim1

  /** Returns the value at (`row`, `col`). */
  @api def apply(row: Idx, col: Idx): A = stage(LUTRead(this,Seq(row,col),Set.empty))
}

/** A 3-dimensional LUT with elements of type A. */
@ref class LUT3[A:Bits]
  extends LUT[A,LUT3]
    with LocalMem3[A,LUT3]
    with Mem3[A,LUT1,LUT2,LUT3]
    with Ref[Array[Any],LUT3[A]] {

  def rank: Int = 3

  /** Returns the value at (`d0`,`d1`,`d2`). */
  @api def apply(d0: Idx, d1: Idx, d2: Idx): A = stage(LUTRead(this,Seq(d0,d1,d2),Set.empty))
}

/** A 4-dimensional LUT with elements of type A. */
@ref class LUT4[A:Bits]
  extends LUT[A,LUT4]
    with LocalMem4[A,LUT4]
    with Mem4[A,LUT1,LUT2,LUT3,LUT4]
    with Ref[Array[Any],LUT4[A]] {

  def rank: Int = 4

  /** Returns the value at (`d0`,`d1`,`d2`,`d3`). */
  @api def apply(d0: Idx, d1: Idx, d2: Idx, d3: Idx): A = {
    stage(LUTRead(this,Seq(d0,d1,d2,d3),Set.empty))
  }
}

/** A 5-dimensional LUT with elements of type A. */
@ref class LUT5[A:Bits]
  extends LUT[A,LUT5]
    with LocalMem5[A,LUT5]
    with Mem5[A,LUT1,LUT2,LUT3,LUT4,LUT5]
    with Ref[Array[Any],LUT5[A]] {

  def rank: Int = 5

  /** Returns the value at (`d0`,`d1`,`d2`,`d3`,`d4`). */
  @api def apply(d0: Idx, d1: Idx, d2: Idx, d3: Idx, d4: Idx): A = {
    stage(LUTRead(this,Seq(d0,d1,d2,d3,d4),Set.empty))
  }
}




