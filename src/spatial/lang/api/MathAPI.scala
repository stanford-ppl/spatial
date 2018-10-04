package spatial.lang.api

import argon._
import forge.tags._
import utils.math.ReduceTree

trait MathAPI { this: Implicits with MuxAPI =>

  /** Creates a sum reduction tree of a fixed number of numeric elements. */
  @api def sum[T:Num](xs: T*): T = if (xs.isEmpty) Num[T].zero else reduce(xs:_*){_+_}

  /** Creates a product reduction tree for a fixed number of numeric elements. */
  @api def product[T:Num](xs: T*): T = if (xs.isEmpty) Num[T].one else reduce(xs:_*){_*_}

  /** Creates a reduction tree for a fixed number of elements. */
  @api def reduce[T](xs: T*)(reduce: (T,T) => T): T = ReduceTree(xs:_*)(reduce)

  @api def min[A:Num](a: Sym[A], b: Sym[A]): A = Num[A].min(a.unbox,b.unbox)
  @api def min[A:Num](a: Sym[A], b: Literal): A = Num[A].min(a.unbox,Num[A].from(b.value))
  @api def min[A:Num](a: Literal, b: Sym[A]): A = Num[A].min(Num[A].from(a.value),b.unbox)
  @api def min[A:Num](a: Lift[A], b: Lift[A]): A = Num[A].min(a.unbox, b.unbox)

  @api def max[A:Num](a: Sym[A], b: Sym[A]): A = Num[A].max(a.unbox,b.unbox)
  @api def max[A:Num,B](a: A, b: Literal): A = Num[A].max(a,Num[A].from(b.value))
  @api def max[A:Num,B](a: Literal, b: A): A = Num[A].max(Num[A].from(a.value),b)
  @api def max[A:Num](a: Lift[A], b: Lift[A]): A = Num[A].max(a.unbox, b.unbox)

  @api def pow[A:Num](b: Sym[A], e: Sym[A]): A = Num[A].pow(b.unbox,e.unbox)
  @api def pow[A:Num](a: A, b: Literal): A = Num[A].pow(a,Num[A].from(b.value))
  @api def pow[A:Num](a: Literal, b: A): A = Num[A].pow(Num[A].from(a.value),b)
  @api def pow[A:Num](b: Lift[A], e: Lift[A]): A = Num[A].pow(b.unbox,e.unbox)

  @api def abs[A:Num](a: Sym[A]): A = Num[A].abs(a.unbox)
  @api def ceil[A:Num](a: Sym[A]): A = Num[A].ceil(a.unbox)
  @api def floor[A:Num](a: Sym[A]): A = Num[A].floor(a.unbox)
  @api def exp[A:Num](a: Sym[A]): A = Num[A].exp(a.unbox)
  @api def ln[A:Num](a: Sym[A]): A = Num[A].ln(a.unbox)
  @api def sqrt[A:Num](a: Sym[A]): A = Num[A].sqrt(a.unbox)
  @api def sin[A:Num](a: Sym[A]): A = Num[A].sin(a.unbox)
  @api def cos[A:Num](a: Sym[A]): A = Num[A].cos(a.unbox)
  @api def tan[A:Num](a: Sym[A]): A = Num[A].tan(a.unbox)
  @api def sinh[A:Num](a: Sym[A]): A = Num[A].sinh(a.unbox)
  @api def cosh[A:Num](a: Sym[A]): A = Num[A].cosh(a.unbox)
  @api def tanh[A:Num](a: Sym[A]): A = Num[A].tanh(a.unbox)
  @api def asin[A:Num](a: Sym[A]): A = Num[A].asin(a.unbox)
  @api def acos[A:Num](a: Sym[A]): A = Num[A].acos(a.unbox)
  @api def atan[A:Num](a: Sym[A]): A = Num[A].atan(a.unbox)
  @api def sigmoid[A:Num](a: Sym[A]): A = Num[A].sigmoid(a.unbox)

  @api def abs[A:Num](a: Lift[A]): A = Num[A].abs(a.unbox)
  @api def ceil[A:Num](a: Lift[A]): A = Num[A].ceil(a.unbox)
  @api def floor[A:Num](a: Lift[A]): A = Num[A].floor(a.unbox)
  @api def exp[A:Num](a: Lift[A]): A = Num[A].exp(a.unbox)
  @api def ln[A:Num](a: Lift[A]): A = Num[A].ln(a.unbox)
  @api def sqrt[A:Num](a: Lift[A]): A = Num[A].sqrt(a.unbox)
  @api def sin[A:Num](a: Lift[A]): A = Num[A].sin(a.unbox)
  @api def cos[A:Num](a: Lift[A]): A = Num[A].cos(a.unbox)
  @api def tan[A:Num](a: Lift[A]): A = Num[A].tan(a.unbox)
  @api def sinh[A:Num](a: Lift[A]): A = Num[A].sinh(a.unbox)
  @api def cosh[A:Num](a: Lift[A]): A = Num[A].cosh(a.unbox)
  @api def tanh[A:Num](a: Lift[A]): A = Num[A].tanh(a.unbox)
  @api def asin[A:Num](a: Lift[A]): A = Num[A].asin(a.unbox)
  @api def acos[A:Num](a: Lift[A]): A = Num[A].acos(a.unbox)
  @api def atan[A:Num](a: Lift[A]): A = Num[A].atan(a.unbox)
  @api def sigmoid[A:Num](a: Lift[A]): A = Num[A].sigmoid(a.unbox)



  implicit class SeqMathOps[A](xs: Seq[A]) {
    @api def reduceTree(f: (A,A) => A): A = reduce(xs:_*)(f)
    @api def sumTree(implicit A: Num[A]): A = sum(xs:_*)
    @api def prodTree(implicit A: Num[A]): A = product(xs:_*)
  }

  implicit class SeqBitOps(xs: Seq[Bit]) {
    @api def andTree: Bit = xs.reduceTree(_&&_)
    @api def orTree: Bit = xs.reduceTree(_||_)
  }

  /** Taylor expansion for natural exponential**/
  @api def exp_taylor[T:Num](x: T): T = {
    mux(x < -3.5.toUnchecked[T], 0.toUnchecked[T],
      mux(x < -1.2.toUnchecked[T], x*0.1.toUnchecked[T] + 0.35.toUnchecked[T],
        1.to[T] + x + x*x/2.to[T] + x*x*x/6.to[T] + x*x*x*x/24.to[T] + x*x*x*x*x/120.to[T]))
  }

  /** Taylor expansion for natural log to third degree. **/
  @api def log_taylor[T:Num](x: T): T = {
    val xx = x - 1.to[T]
    xx - xx*xx/2.to[T] + xx*xx*xx/3.to[T] - xx*xx*xx*xx/4.to[T]
  }

  /** Taylor expansion for sin from -pi to pi **/
  @api def sin_taylor[T:Type:Num](x: T): T = {
    x - x*x*x/6.to[T] + x*x*x*x*x/120.to[T] //- x*x*x*x*x*x*x/5040
  }


  /** Taylor expansion for cos from -pi to pi **/
  @api def cos_taylor[T:Num](x: T): T = {
    1.to[T] - x*x/2.to[T] + x*x*x*x/24.to[T] //- x*x*x*x*x*x/720
  }

  @api def sqrt_approx[T:Num](x: T): T = {
    // I don't care how inefficient this is, it is just a placeholder for backprop until we implement floats
    mux(x < 2.to[T], 1.to[T] + (x-1.to[T])/2.to[T] -(x-1.to[T])*(x-1.to[T])/8.to[T]+(x-1.to[T])*(x-1.to[T])*(x-1.to[T])/16.to[T], // 3rd order taylor for values up to 2
      mux(x < 10.to[T], x*0.22.toUnchecked[T] + 1.to[T], // Linearize
        mux( x < 100.to[T], x*0.08.toUnchecked[T] + 2.5.to[T], // Linearize
          mux( x < 1000.to[T], x*0.028.toUnchecked[T] + 8.to[T], // Linearize
            mux( x < 10000.to[T], x*0.008.toUnchecked[T] + 20.to[T], x*0.0002.toUnchecked[T] + 300.to[T]))))) // Linearize
  }

}
