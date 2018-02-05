package pcc.util

import forge._
import pcc.lang.{Math,I32}

trait IntLike[T] {
  @api def neg(a: T): T
  @api def add(a: T, b: T): T
  @api def sub(a: T, b: T): T
  @api def mul(a: T, b: T): T
  @api def div(a: T, b: T): T
  @api def mod(a: T, b: T): T
  @api def fromInt(a: Int): T
}

object IntLike {
  type IntLike[T] = pcc.util.IntLike[T]

  def int[T:IntLike]: IntLike[T] = implicitly[IntLike[T]]

  implicit class IntLikeOps[T:IntLike](a: T) {
    private val ev = int[T]
    @api def unary_-(): T = ev.neg(a)
    @api def +(b: T): T = ev.add(a,b)
    @api def -(b: T): T = ev.sub(a,b)
    @api def *(b: T): T = ev.mul(a,b)
    @api def /(b: T): T = ev.div(a,b)
    @api def %(b: T): T = ev.mod(a,b)
    @api def +(b: Int): T = ev.add(a,ev.fromInt(b))
    @api def -(b: Int): T = ev.sub(a,ev.fromInt(b))
    @api def *(b: Int): T = ev.mul(a,ev.fromInt(b))
    @api def /(b: Int): T = ev.div(a,ev.fromInt(b))
    @api def %(b: Int): T = ev.mod(a,ev.fromInt(b))
  }
  implicit class IntOnIntLikeOps(a: Int) {
    @api def +[T:IntLike](b: T): T = int[T].add(int[T].fromInt(a),b)
    @api def -[T:IntLike](b: T): T = int[T].sub(int[T].fromInt(a),b)
    @api def *[T:IntLike](b: T): T = int[T].mul(int[T].fromInt(a),b)
    @api def /[T:IntLike](b: T): T = int[T].div(int[T].fromInt(a),b)
    @api def %[T:IntLike](b: T): T = int[T].mod(int[T].fromInt(a),b)
  }
  @api def zero[T:IntLike]: T = int[T].fromInt(0)
  @api def one[T:IntLike]: T = int[T].fromInt(1)
  @api def product[T:IntLike](xs: Seq[T]): T = if (xs.isEmpty) one[T] else Math.reduce(xs){_*_}
  @api def sum[T:IntLike](xs: Seq[T]): T = if (xs.isEmpty) zero[T] else Math.reduce(xs){_+_}

  implicit object I32IsIntLike extends IntLike[I32] {
    @api def neg(a: I32): I32 = -a
    @api def add(a: I32, b: I32): I32 = a + b
    @api def sub(a: I32, b: I32): I32 = a - b
    @api def mul(a: I32, b: I32): I32 = a * b
    @api def div(a: I32, b: I32): I32 = a / b
    @api def mod(a: I32, b: I32): I32 = a % b
    @api def fromInt(a: Int): I32 = I32.c(a)
  }

  implicit object IntIsIntLike extends IntLike[Int] {
    @api def neg(a: Int): Int = -a
    @api def add(a: Int, b: Int): Int = a + b
    @api def sub(a: Int, b: Int): Int = a - b
    @api def mul(a: Int, b: Int): Int = a * b
    @api def div(a: Int, b: Int): Int = a / b
    @api def mod(a: Int, b: Int): Int = a % b
    @api def fromInt(a: Int): Int = a
  }

}
