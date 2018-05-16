package spatial.lang
package host

import argon._
import forge.tags._

/** A three-dimensional tensor on the host */
@ref class Tensor3[A:Type] extends Struct[Tensor3[A]] with Ref[scala.Array[Any],Tensor3[A]] {
  override val box = implicitly[Tensor3[A] <:< Struct[Tensor3[A]]]
  val A: Type[A] = Type[A]
  def fields = Seq(
    "data" -> Type[Array[A]],
    "dim0" -> Type[I32],
    "dim1" -> Type[I32],
    "dim2" -> Type[I32]
  )

  @rig def data: Array[A] = field[Array[A]]("data")

  /** Returns the first dimension of this Tensor3. */
  @api def dim0: I32 = field[I32]("dim0")
  /** Returns the second dimension of this Tensor3. */
  @api def dim1: I32 = field[I32]("dim1")
  /** Returns the third dimension of this Tensor3. */
  @api def dim2: I32 = field[I32]("dim2")
  /** Returns the element in this Tensor3 at the given 3-dimensional address. */
  @api def apply(i: I32, j: I32, k: I32): A = data.apply(i*dim1*dim2 + j*dim2 + k)
  /** Updates the element at the given 3-dimensional address to `elem`. */
  @api def update(i: I32, j: I32, k: I32, elem: A): Void = data.update(i*dim1*dim2 + j*dim1 + k, elem)
  /** Returns a flattened, immutable @Array view of this Tensor3's data. */
  @api def flatten: Array[A] = data
  /** Returns the number of elements in the Tensor3. */
  @api def length: I32 = dim0 * dim1 * dim2

  /** Applies the function `func` on each element in this Tensor3. */
  @api def foreach(func: A => Void): Void = data.foreach(func)
  /** Returns a new Tensor3 created using the mapping `func` over each element in this Tensor3. */
  @api def map[B:Type](func: A => B): Tensor3[B] = Tensor3(data.map(func), dim0, dim1, dim2)
  /** Returns a new Tensor3 created using the pairwise mapping `func` over each element in this Tensor3
    * and the corresponding element in `that`.
    */
  @api def zip[B:Type,C:Type](that: Tensor3[B])(func: (A,B) => C): Tensor3[C] = Tensor3(data.zip(that.data)(func), dim0, dim1, dim2)
  /** Reduces the elements in this Tensor3 into a single element using associative function `rfunc`. */
  @api def reduce(rfunc: (A,A) => A): A = data.reduce(rfunc)

  /** Returns true if this Tensor3 and `that` contain the same elements, false otherwise. */
  @api override def neql(that: Tensor3[A]): Bit = data !== that.data

  /** Returns false if this Tensor3 and `that` differ by at least one element, true otherwise. */
  @api override def eql(that: Tensor3[A]): Bit = data === that.data

}
object Tensor3 {
  @api def apply[A:Type](data: Array[A], dim0: I32, dim1: I32, dim2: I32): Tensor3[A] = {
    Struct[Tensor3[A]]("data" -> data, "dim0" -> dim0, "dim1" -> dim1, "dim2" -> dim2)
  }

  /** Returns an immutable Tensor3 with the given dimensions and elements defined by `func`. */
  @api def tabulate[T:Type](dim0: I32, dim1: I32, dim2: I32)(func: (I32, I32, I32) => T): Tensor3[T] = {
    val data = Array.tabulate(dim0*dim1*dim2){x =>
      val i = x / (dim1*dim2)     // Page
      val j = (x / dim2) % dim1   // Row
      val k = x % dim2            // Col
      func(i,j,k)
    }
    Tensor3(data, dim0, dim1, dim2)
  }
  /**
    * Returns an immutable Tensor3 with the given dimensions and elements defined by `func`.
    * Note that while `func` does not depend on the index, it is still executed multiple times.
    */
  @api def fill[T:Type](dim0: I32, dim1: I32, dim2: I32)(func: => T): Tensor3[T]
    = Tensor3.tabulate(dim0,dim1,dim2){(_,_,_) => func}
}



