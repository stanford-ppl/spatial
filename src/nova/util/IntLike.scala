package nova.util

import emul.FixedPoint
import forge.tags._
import nova.util
import spatial.lang.I32

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
    def fromInt(a: Int): I32 = I32.c(a)
  }

  implicit object FixedPointIsIntLike extends IntLike[FixedPoint] {
    @api def plus(a: FixedPoint, b: FixedPoint): FixedPoint = a + b
    @api def minus(a: FixedPoint, b: FixedPoint): FixedPoint = a - b
    @api def times(a: FixedPoint, b: FixedPoint): FixedPoint = a * b
    @api def divide(a: FixedPoint, b: FixedPoint): FixedPoint = a / b
    @api def modulus(a: FixedPoint, b: FixedPoint): FixedPoint = a % b
    def fromInt(a: Int): FixedPoint = FixedPoint.fromInt(a)
  }
}
