package spatial.lang
package host

import argon._
import forge.tags._
import spatial.node._
import spatial.internal._

/** A one-dimensional array on the host. */
@ref class Array[A:Type] extends Top[Array[A]] with Ref[scala.Array[Any],Array[A]] {
  val A: Type[A] = Type[A]
  override val __isPrimitive = false

  /** Returns the size of this Array. */
  @api def length: I32 = stage(ArrayLength(this))

  /** Returns the element at index `i`. */
  @api def apply(i: I32): A = stage(ArrayApply(this, i))

  /** Updates the element at index `i` to data. */
  @api def update(i: I32, data: A): Void = stage(ArrayUpdate(this, i, data))

  /** Applies the function **func** on each element in the Array. */
  @api def foreach(func: A => Void): Void = {
    val i   = boundVar[I32]
    val applyBlk = stageLambda2(this,i){ this(i) }
    val funcBlk  = stageLambda1(applyBlk.result){ func(applyBlk.result.unbox) }
    stage(ArrayForeach(this, applyBlk, funcBlk))
  }

  /** Returns a new Array created using the mapping `func` over each element in this Array. */
  @api def map[B:Type](func: A => B): Array[B] = {
    val i   = boundVar[I32]
    val applyBlk = stageLambda2(this,i){ this(i) }
    val funcBlk  = stageLambda1(applyBlk.result){ func(applyBlk.result.unbox) }
    stage(ArrayMap(this,applyBlk,funcBlk))
  }

  /** Returns a new Array created using the pairwise mapping `func` over each element in this Array
    * and the corresponding element in `that`.
    */
  @api def zip[B:Type,C:Type](that: Array[B])(func: (A,B) => C): Array[C] = {
    val i    = boundVar[I32]
    val applyA = stageLambda2(this,i){ this(i) }
    val applyB = stageLambda2(that,i){ that(i) }
    val funcBlk = stageLambda2(applyA.result,applyB.result){ func(applyA.result.unbox,applyB.result.unbox) }
    stage(ArrayZip(this,that,applyA,applyB,funcBlk))
  }

  /** Reduces the elements in this Array into a single element using associative function `rfunc`. */
  @api def reduce(rfunc: (A,A) => A): A = {
    val i   = boundVar[I32]
    val a1  = boundVar[A]
    val a2  = boundVar[A]
    val applyBlk  = stageLambda2(this,i){ this(i) }
    val reduceBlk = stageLambda2(a1,a2){ rfunc(a1,a2) }
    stage(ArrayReduce(this,applyBlk,reduceBlk))
  }

  @api def sum(implicit aA: Num[A]): A = this.fold(aA.zero){(a,b) => a + b }
  @api def product(implicit aA: Num[A]): A = this.fold(aA.one){(a,b) => a * b }

  /**
    * Reduces the elements in this Array and the given initial value into a single element
    * using associative function `rfunc`.
    */
  @api def fold(init: A)(rfunc: (A,A) => A): A = {
    val i   = boundVar[I32]
    val a1  = boundVar[A]
    val a2  = boundVar[A]
    val applyBlk  = stageLambda2(this,i){ this(i) }
    val reduceBlk = stageLambda2(a1,a2){ rfunc(a1,a2) }
    stage(ArrayFold(this,init,applyBlk,reduceBlk))
  }

  /** Returns a new Array with all elements in this Array which satisfy the given predicate `cond`. */
  @api def filter(cond: A => Bit): Array[A] = {
    val i   = boundVar[I32]
    val applyBlk = stageLambda2(this,i){ this(i) }
    val condBlk  = stageLambda1(applyBlk.result){ cond(applyBlk.result.unbox) }
    stage(ArrayFilter(this,applyBlk,condBlk))
  }

  /** Returns a new Array created by concatenating the results of `func` applied to all elements in this Array. */
  @api def flatMap[B:Type](func: A => Array[B]): Array[B] = {
    val i   = boundVar[I32]
    val applyBlk = stageLambda2(this,i){ this(i) }
    val funcBlk  = stageLambda1(applyBlk.result){ func(applyBlk.result.unbox) }
    stage(ArrayFlatMap(this,applyBlk,funcBlk))
  }

  @api def forall(func: A => Bit): Bit = this.map(func).reduce{_&&_}
  @api def exists(func: A => Bit): Bit = this.map(func).reduce{_||_}

  /** Returns a string representation using the given `delimeter`. */
  @api def mkString(delimeter: Text): Text = this.mkString("", delimeter, "")

  /** Returns a string representation using the given `delimeter`, bracketed by `start` and `stop`. */
  @api def mkString(start: Text, delimeter: Text, stop: Text): Text = {
    stage(ArrayMkString(this,start,delimeter,stop))
  }

  /** Returns an immutable view of the data in this Array as a Matrix with given `rows` and `cols`. **/
  @virtualize
  @api def reshape(rows: I32, cols: I32): Tensor2[A] = {
    assertIf(rows*cols === this.length, "Number of elements in vector ("+this.length.toText+") must match number of elements in matrix ("+rows.toText+"x"+cols.toText+")")
    Tensor2(this, rows, cols)
  }
  /** Returns an immutable view of the data in this Array as a Tensor3 with given dimensions. **/
  @virtualize
  @api def reshape(dim0: I32, dim1: I32, dim2: I32): Tensor3[A] = {
    assertIf(dim0*dim1*dim2 === this.length, "Number of elements in vector ("+this.length.toText+") must match number of elements in matrix ("+dim0.toText+"x"+dim1.toText+"x"+dim2.toText+")")
    Tensor3(this, dim0, dim1, dim2)
  }
  /** Returns an immutable view of the data in this Array as a Tensor4 with given dimensions. **/
  @virtualize
  @api def reshape(dim0: I32, dim1: I32, dim2: I32, dim3: I32): Tensor4[A] = {
    assertIf(dim0*dim1*dim2*dim3 === this.length, "Number of elements in vector ("+this.length.toText+") must match number of elements in matrix ("+dim0.toText+"x"+dim1.toText+"x"+dim2.toText+"x"+dim3.toText+")")
    Tensor4(this, dim0, dim1, dim2, dim3)
  }
  /** Returns an immutable view of the data in this Array as a Tensor5 with given dimensions. **/
  @virtualize
  @api def reshape(dim0: I32, dim1: I32, dim2: I32, dim3: I32, dim4: I32): Tensor5[A] = {
    assertIf(dim0*dim1*dim2*dim3*dim4 === this.length, "Number of elements in vector ("+this.length.toText+") must match number of elements in matrix ("+dim0.toText+"x"+dim1.toText+"x"+dim2.toText+"x"+dim3.toText+"x"+dim4.toText+")")
    Tensor5(this, dim0, dim1, dim2, dim3, dim4)
  }

  @virtualize
  @api def toeplitz(filterdim0: I32, filterdim1: I32, imgdim0: I32, imgdim1: I32, stride0: I32, stride1: I32)(implicit num: Num[A]): Tensor2[A] = {
    // TODO: Incorporate stride
    val pad0 = filterdim0 - 1 - (stride0-1)
    val pad1 = filterdim1 - 1 - (stride1-1)
    val out_rows = ((imgdim0+pad0-filterdim0+stride0) * (imgdim1+pad1-filterdim1+stride1)) / (stride0 * stride1)
    val out_cols = (imgdim0+pad0)*(imgdim1+pad1)

    val data = Tensor1.tabulate(out_rows * out_cols){k =>
      val i = k / out_cols
      val j = k % out_cols
      val current_slide_row = i / (imgdim1/stride1)
      val current_slide_col = i % (imgdim1/stride1)
      val rows_correction = current_slide_row * stride0 * (imgdim1+pad1)
      val cols_correction = current_slide_col * stride1
      val filter_base = j - rows_correction - cols_correction
      val filter_i = filter_base / (imgdim1+pad1)
      val filter_j = filter_base % (imgdim1+pad1)
      if (filter_base >= 0 && filter_j < filterdim1 && filter_j >= 0 && filter_i < filterdim0 && filter_i >= 0)
        this.apply(filter_i * filterdim1 + filter_j)
      else
        0.to[A]
    }
    Matrix(data, out_rows, out_cols)
  }



  /** Returns true if this Array and `that` contain the same elements, false otherwise. */
  @api override def neql(that: Array[A]): Bit = this.zip(that){(x,y) => x !== y }.exists(x => x)

  /** Returns false if this Array and `that` differ by at least one element, true otherwise. */
  @api override def eql(that: Array[A]): Bit = this.zip(that){(x,y) => x === y }.forall(x => x)

  @api override def toText: Text = this.mkString(", ")
}

object Array {
  /** Returns an immutable Array with the given `size` and elements defined by `func`. */
  @api def tabulate[A:Type](size: I32)(func: I32 => A): Array[A] = {
    val i = boundVar[I32]
    val funcBlk = stageLambda1(i){ func(i) }
    stage(MapIndices(size, funcBlk))
  }

  /**
    * Returns an immutable Array with the given `size` and elements defined by `func`.
    * Note that while `func` does not depend on the index, it is still executed `size` times.
    */
  @api def fill[A:Type](size: I32)(func: => A): Array[A] = this.tabulate(size){ _ => func}

  /** Returns an empty, mutable Array with the given `size`. */
  @api def empty[A:Type](size: I32): Array[A] = stage(ArrayNew[A](size))

  /** Returns an immutable Array with the given elements. */
  @api def apply[A:Type](elements: A*): Array[A] = stage(ArrayFromSeq[A](elements.map{s => box(s)}))

}