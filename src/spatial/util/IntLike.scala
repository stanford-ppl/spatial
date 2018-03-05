package spatial.util

import emul.FixedPoint
import forge.tags._
import spatial.lang.{Fix,I32}

abstract class IntLike[A] {
  @api def plus(a: A, b: A): A
  @api def minus(a: A, b: A): A
  @api def times(a: A, b: A): A
  @api def divide(a: A, b: A): A
  @api def modulus(a: A, b: A): A
  def fromInt(a: Int): A
  final def zero: A = fromInt(0)
  final def one: A = fromInt(1)
}
object IntLike {

  implicit object IntIsIntLike extends IntLike[Int] {
    @api def plus(a: Int, b: Int): Int = a + b
    @api def minus(a: Int, b: Int): Int = a - b
    @api def times(a: Int, b: Int): Int = a * b
    @api def divide(a: Int, b: Int): Int = a / b
    @api def modulus(a: Int, b: Int): Int = a % b
    def fromInt(a: Int): Int = a
  }

  implicit object I32IsIntLike extends IntLike[I32] {
    @api def plus(a: I32, b: I32): I32 = a + b
    @api def minus(a: I32, b: I32): I32 = a - b
    @api def times(a: I32, b: I32): I32 = a * b
    @api def divide(a: I32, b: I32): I32 = a / b
    @api def modulus(a: I32, b: I32): I32 = a % b
    def fromInt(a: Int): I32 = I32(a)
  }

  implicit object FixedPointIsIntLike extends IntLike[FixedPoint] {
    @api def plus(a: FixedPoint, b: FixedPoint): FixedPoint = a + b
    @api def minus(a: FixedPoint, b: FixedPoint): FixedPoint = a - b
    @api def times(a: FixedPoint, b: FixedPoint): FixedPoint = a * b
    @api def divide(a: FixedPoint, b: FixedPoint): FixedPoint = a / b
    @api def modulus(a: FixedPoint, b: FixedPoint): FixedPoint = a % b
    def fromInt(a: Int): FixedPoint = FixedPoint.fromInt(a)
  }

  implicit class IntLikeOps[A](a: A) {
    @api def +(b: Int)(implicit int: IntLike[A]): A = int.plus(a,int.fromInt(b))
    @api def -(b: Int)(implicit int: IntLike[A]): A = int.minus(a,int.fromInt(b))
    @api def *(b: Int)(implicit int: IntLike[A]): A = int.times(a,int.fromInt(b))
    @api def /(b: Int)(implicit int: IntLike[A]): A = int.divide(a,int.fromInt(b))
    @api def %(b: Int)(implicit int: IntLike[A]): A = int.modulus(a,int.fromInt(b))
    @api def +(b: A)(implicit int: IntLike[A]): A = int.plus(a,b)
    @api def -(b: A)(implicit int: IntLike[A]): A = int.minus(a,b)
    @api def *(b: A)(implicit int: IntLike[A]): A = int.times(a,b)
    @api def /(b: A)(implicit int: IntLike[A]): A = int.divide(a,b)
    @api def %(b: A)(implicit int: IntLike[A]): A = int.modulus(a,b)
  }

  implicit class SeqIntLike[A:IntLike](xs: Seq[A]) {
    import utils.math.ReduceTree
    @api def prodTree: A = ReduceTree(xs:_*){_*_}
    @api def sumTree: A = ReduceTree(xs:_*){_+_}
  }

  implicit class IntOps(a: Int) {
    @api def +[A](b: A)(implicit int: IntLike[A]): A = int.plus(int.fromInt(a),b)
    @api def -[A](b: A)(implicit int: IntLike[A]): A = int.minus(int.fromInt(a),b)
    @api def *[A](b: A)(implicit int: IntLike[A]): A = int.times(int.fromInt(a),b)
    @api def /[A](b: A)(implicit int: IntLike[A]): A = int.divide(int.fromInt(a),b)
    @api def %[A](b: A)(implicit int: IntLike[A]): A = int.modulus(int.fromInt(a),b)
  }

  def zeroI[A](implicit int: IntLike[A]): A = int.zero
  def oneI[A](implicit int: IntLike[A]): A = int.one
}
