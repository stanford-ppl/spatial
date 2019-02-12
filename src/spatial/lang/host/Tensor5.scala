package spatial.lang
package host

import argon._
import forge.tags._

/** A 5-dimensional tensor on the host */
@ref class Tensor5[A:Type] extends Struct[Tensor5[A]] with Ref[scala.Array[Any],Tensor5[A]] {
  override val box = implicitly[Tensor5[A] <:< Struct[Tensor5[A]]]
  val A: Type[A] = Type[A]
  def fields = Seq(
    "data" -> Type[Array[A]],
    "dim0" -> Type[I32],
    "dim1" -> Type[I32],
    "dim2" -> Type[I32],
    "dim3" -> Type[I32],
    "dim4" -> Type[I32]
  )

  @rig def data: Array[A] = field[Array[A]]("data")

  /** Returns the dimensions of this Tensor5. */
  @api def dims: Seq[I32] = Seq(field[I32]("dim0"), field[I32]("dim1"), field[I32]("dim2"), field[I32]("dim3"), field[I32]("dim4"))
  /** Returns the first dimension of this Tensor5. */
  @api def dim0: I32 = field[I32]("dim0")
  /** Returns the second dimension of this Tensor5. */
  @api def dim1: I32 = field[I32]("dim1")
  /** Returns the third dimension of this Tensor5. */
  @api def dim2: I32 = field[I32]("dim2")
  /** Returns the fourth dimension of this Tensor5. */
  @api def dim3: I32 = field[I32]("dim3")
  /** Returns the fifth dimension of this Tensor5. */
  @api def dim4: I32 = field[I32]("dim4")
  /** Returns the element in this Tensor5 at the given 5-dimensional addreess. */
  @api def apply(i: I32, j: I32, k: I32, l: I32, m: I32): A = {
    data.apply(i*dim1*dim2*dim3*dim4 + j*dim2*dim3*dim4 + k*dim3*dim4 + l*dim4 + m)
  }
  /** Updates the element at the given 5-dimensional address to `elem`. */
  @api def update(i: I32, j: I32, k: I32, l: I32, m: I32, elem: A): Void = {
    data.update(i*dim1*dim2*dim3*dim4 + j*dim2*dim3*dim4 + k*dim3*dim4 + l*dim4 + m, elem)
  }
  /** Returns a flattened, immutable @Array view of this Tensor5's data. */
  @api def flatten: Array[A] = data
  /** Returns the number of elements in the Tensor5. */
  @api def length: I32 = dim0 * dim1 * dim2 * dim3 * dim4

  /** Applies the function `func` on each element in this Tensor5. */
  @api def foreach(func: A => Void): Void = data.foreach(func)
  /** Returns a new Tensor5 created using the mapping `func` over each element in this Tensor5. */
  @api def map[B:Type](func: A => B): Tensor5[B] = Tensor5(data.map(func), dim0, dim1, dim2, dim3, dim4)
  /** Returns a new Tensor5 created using the pairwise mapping `func` over each element in this Tensor5
    * and the corresponding element in `that`.
    */
  @api def zip[B:Type,C:Type](b: Tensor5[B])(func: (A,B) => C): Tensor5[C] = {
    Tensor5(data.zip(b.data)(func), dim0, dim1, dim2, dim3, dim4)
  }

  /** Reduces the elements in this Tensor5 into a single element using associative function `rfunc`. */
  @api def reduce(rfunc: (A,A) => A): A = data.reduce(rfunc)

  /** Reorders the Tensor5 based on given ordering (i.e.- reorder(0,1,2,3,4) does nothing) */
  @api def reorder(ordering: Seq[scala.Int]): Tensor5[A] = {
    (0::dims.apply(ordering(0)), 0::dims.apply(ordering(1)), 0::dims.apply(ordering(2)), 0::dims.apply(ordering(3)), 0::dims.apply(ordering(4))){(a,b,c,d,e) => 
      val i = List(a,b,c,d,e)
      apply(i(ordering(0)), i(ordering(1)), i(ordering(2)), i(ordering(3)), i(ordering(4)))
    }
  }

  /** Returns true if this Tensor5 and `that` contain the same elements, false otherwise. */
  @api override def neql(that: Tensor5[A]): Bit = data !== that.data

  /** Returns false if this Tensor5 and `that` differ by at least one element, true otherwise. */
  @api override def eql(that: Tensor5[A]): Bit = data === that.data

}
object Tensor5 {
  @api def apply[A:Type](data: Array[A], dim0: I32, dim1: I32, dim2: I32, dim3: I32, dim4: I32): Tensor5[A] = {
    Struct[Tensor5[A]]("data" -> data, "dim0" -> dim0, "dim1" -> dim1, "dim2" -> dim2, "dim3" -> dim3, "dim4" -> dim4)
  }

  /** Returns an immutable Tensor5 with the given dimensions and elements defined by `func`. */
  @api def tabulate[T:Type](dim0: I32, dim1: I32, dim2: I32, dim3: I32, dim4: I32)(func: (I32, I32, I32, I32, I32) => T): Tensor5[T] = {
    val data = Array.tabulate(dim0*dim1*dim2*dim3*dim4){x =>
      val i0 = x / (dim1*dim2*dim3*dim4)
      val i1 = (x / (dim2*dim3*dim4)) % dim1
      val i2 = (x / (dim3*dim4)) % dim2
      val i3 = (x / (dim4)) % dim3
      val i4 = x % dim4

      func(i0,i1,i2,i3,i4)
    }
    Tensor5(data, dim0, dim1, dim2, dim3, dim4)
  }
  /**
    * Returns an immutable Tensor5 with the given dimensions and elements defined by `func`.
    * Note that while `func` does not depend on the index, it is still executed multiple times.
    */
  @api def fill[T:Type](dim0: I32, dim1: I32, dim2: I32, dim3: I32, dim4: I32)(func: => T): Tensor5[T]
    = Tensor5.tabulate(dim0,dim1,dim2,dim3,dim4){(_,_,_,_,_) => func}

}