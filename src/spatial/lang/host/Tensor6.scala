package spatial.lang
package host

import argon._
import forge.tags._

/** A 5-dimensional tensor on the host */
@ref class Tensor6[A:Type] extends Struct[Tensor6[A]] with Ref[scala.Array[Any],Tensor6[A]] {
  override val box = implicitly[Tensor6[A] <:< Struct[Tensor6[A]]]
  val A: Type[A] = Type[A]
  def fields = Seq(
    "data" -> Type[Array[A]],
    "dim0" -> Type[I32],
    "dim1" -> Type[I32],
    "dim2" -> Type[I32],
    "dim3" -> Type[I32],
    "dim4" -> Type[I32],
    "dim5" -> Type[I32]
  )

  @rig def data: Array[A] = field[Array[A]]("data")

  @api def dims: Seq[I32] = Seq(field[I32]("dim0"), field[I32]("dim1"), field[I32]("dim2"), field[I32]("dim3"), field[I32]("dim4"), field[I32]("dim5"))
  @api def dim0: I32 = field[I32]("dim0")
  @api def dim1: I32 = field[I32]("dim1")
  @api def dim2: I32 = field[I32]("dim2")
  @api def dim3: I32 = field[I32]("dim3")
  @api def dim4: I32 = field[I32]("dim4")
  @api def dim5: I32 = field[I32]("dim5")
  
  
  @api def apply(i: I32, j: I32, k: I32, l: I32, m: I32, n: I32): A = {
    data.apply(i*dim1*dim2*dim3*dim4*dim5 + j*dim2*dim3*dim4*dim5 + k*dim3*dim4*dim5 + l*dim4*dim5 + m*dim5 + n)
  }
  
  @api def update(i: I32, j: I32, k: I32, l: I32, m: I32, n: I32, elem: A): Void = {
    data.update(i*dim1*dim2*dim3*dim4*dim5 + j*dim2*dim3*dim4*dim5 + k*dim3*dim4*dim5 + l*dim4*dim5 + m*dim5 + n, elem)
  }
  
  @api def flatten: Array[A] = data
  
  @api def length: I32 = dim0 * dim1 * dim2 * dim3 * dim4 * dim5

  @api def foreach(func: A => Void): Void = data.foreach(func)
  
  @api def map[B:Type](func: A => B): Tensor6[B] = Tensor6(data.map(func), dim0, dim1, dim2, dim3, dim4, dim5)
  
  @api def zip[B:Type,C:Type](b: Tensor6[B])(func: (A,B) => C): Tensor6[C] = {
    Tensor6(data.zip(b.data)(func), dim0, dim1, dim2, dim3, dim4, dim5)
  }

  @api def reduce(rfunc: (A,A) => A): A = data.reduce(rfunc)

  @api def reorder(ordering: Seq[scala.Int]): Tensor6[A] = {
    (0::dims.apply(ordering(0)), 0::dims.apply(ordering(1)), 0::dims.apply(ordering(2)), 0::dims.apply(ordering(3)), 0::dims.apply(ordering(4)), 0::dims.apply(ordering(5))){(a,b,c,d,e,f) => 
      val i = List(a,b,c,d,e,f)
      apply(i(ordering(0)), i(ordering(1)), i(ordering(2)), i(ordering(3)), i(ordering(4)), i(ordering(5)))
    }
  }

  @api override def neql(that: Tensor6[A]): Bit = data !== that.data

  @api override def eql(that: Tensor6[A]): Bit = data === that.data

}
object Tensor6 {
  @api def apply[A:Type](data: Array[A], dim0: I32, dim1: I32, dim2: I32, dim3: I32, dim4: I32, dim5: I32): Tensor6[A] = {
    Struct[Tensor6[A]]("data" -> data, "dim0" -> dim0, "dim1" -> dim1, "dim2" -> dim2, "dim3" -> dim3, "dim4" -> dim4, "dim5" -> dim5)
  }


  @api def tabulate[T:Type](dim0: I32, dim1: I32, dim2: I32, dim3: I32, dim4: I32, dim5: I32)(func: (I32, I32, I32, I32, I32, I32) => T): Tensor6[T] = {
    val data = Array.tabulate(dim0*dim1*dim2*dim3*dim4*dim5){x =>
      val i0 = x / (dim1*dim2*dim3*dim4*dim5)
      val i1 = (x / (dim2*dim3*dim4*dim5)) % dim1
      val i2 = (x / (dim3*dim4*dim5)) % dim2
      val i3 = (x / (dim4*dim5)) % dim3
      val i4 = (x / (dim5)) % dim4
      val i5 = x % dim5
      

      func(i0,i1,i2,i3,i4,i5)
    }
    Tensor6(data, dim0, dim1, dim2, dim3, dim4, dim5)
  }
  
  @api def fill[T:Type](dim0: I32, dim1: I32, dim2: I32, dim3: I32, dim4: I32, dim5: I32)(func: => T): Tensor6[T]
    = Tensor6.tabulate(dim0,dim1,dim2,dim3,dim4,dim5){(_,_,_,_,_,_) => func}

}