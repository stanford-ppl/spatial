package spatial.lang
package host

import argon._
import forge.tags._
import spatial.node._

/** A two-dimensional matrix on the host */
@ref class Matrix[A:Type] extends Struct[Matrix[A]] with Ref[scala.Array[Any],Matrix[A]] {
  override val box = implicitly[Matrix[A] <:< Struct[Matrix[A]]]
  val A: Type[A] = Type[A]
  def fields = Seq(
    "data" -> Type[Array[A]],
    "rows" -> Type[I32],
    "cols" -> Type[I32]
  )

  @rig def data: Array[A] = field[Array[A]]("data")

  /** Returns the number of rows in this Matrix. */
  @api def rows: I32 = field[I32]("rows")

  /** Returns the number of columns in this Matrix. */
  @api def cols: I32 = field[I32]("cols")

  /** Returns the element in this Matrix at the given 2-dimensional address (`i`, `j`). */
  @api def apply(i: I32, j: I32): A = data.apply(i*cols + j)

  /** Updates the element at the given two dimensional address to `elem`. */
  @api def update(i: I32, j: I32, elem: A): Void = data.update(i*cols + j, elem)

  /** Returns a flattened, immutable @Array view of this Matrix's data. */
  @api def flatten: Array[A] = data

  /** Returns the number of elements in the Matrix. */
  @api def length: I32 = rows * cols

  /** Applies the function `func` on each element in this Matrix. */
  @api def foreach(func: A => Void): Void = data.foreach(func)

  /** Returns a new Matrix created using the mapping `func` over each element in this Matrix. */
  @api def map[R:Type](func: A => R): Matrix[R] = Matrix(data.map(func), rows, cols)

  /** Returns a new Matrix created using the pairwise mapping `func` over each element in this Matrix
    * and the corresponding element in `that`.
    */
  @api def zip[S:Type,R:Type](that: Matrix[S])(func: (A,S) => R): Matrix[R] = Matrix(data.zip(that.data)(func), rows, cols)

  /** Reduces the elements in this Matrix into a single element using associative function `rfunc`. */
  @api def reduce(rfunc: (A,A) => A): A = data.reduce(rfunc)

  /** Returns the transpose of this Matrix. */
  @api def t: Matrix[A] = Matrix.tabulate(cols, rows){(i,j) => apply(j,i) }
}
object Matrix {
  @api def apply[A:Type](data: Array[A], rows: I32, cols: I32): Matrix[A] = {
    Struct[Matrix[A]]("data" -> data, "rows" -> rows, "cols" -> cols)
  }

  /** Returns an immutable Matrix with the given `rows` and `cols` and elements defined by `func`. */
  @api def tabulate[A:Type](rows: I32, cols: I32)(func: (I32, I32) => A): Matrix[A] = {
    val data = Array.tabulate(rows*cols){x =>
      val i = x / cols
      val j = x % cols
      func(i,j)
    }
    Matrix(data, rows, cols)
  }

  /**
    * Returns an immutable Matrix with the given `rows` and `cols` and elements defined by `func`.
    * Note that while `func` does not depend on the index, it is still executed `rows`*`cols` times.
    */
  @api def fill[A:Type](rows: I32, cols: I32)(func: => A): Matrix[A] = {
    Matrix.tabulate(rows, cols){(_,_) => func}
  }
}
