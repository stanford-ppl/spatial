package spatial.util

import argon._
import emul.FixedPoint
import forge.tags._
import spatial.lang._

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

  implicit object IdxIsIntLike extends IntLike[Idx] {
    @rig private def map[S,I](a: Idx, b: Idx)(func: (Fix[S,I,_0],Fix[S,I,_0]) => Fix[S,I,_0]): Idx = {
      val fmt = a.fmt merge b.fmt
      class IA extends INT[IA]{ val v: Int = a.fmt.ibits }
      class IB extends INT[IB]{ val v: Int = b.fmt.ibits }
      class IC extends INT[IC]{ val v: Int = fmt.ibits }
      class SA extends BOOL[SA]{ val v: Boolean = a.fmt.sign }
      class SB extends BOOL[SB]{ val v: Boolean = b.fmt.sign }
      class SC extends BOOL[SC]{ val v: Boolean = fmt.sign }

      implicit val SA: BOOL[SA] = new SA
      implicit val SB: BOOL[SB] = new SB
      implicit val SC: BOOL[SC] = new SC
      implicit val IA: INT[IA] = new IA
      implicit val IB: INT[IB] = new IB
      implicit val IC: INT[IC] = new IC
      implicit val A: Type[Fix[SA,IA,_0]] = new Fix[SA,IA,_0].asType
      implicit val B: Type[Fix[SB,IB,_0]] = new Fix[SB,IB,_0].asType
      val aa = a.asInstanceOf[Fix[SA,IA,_0]].to[Fix[SC,IC,_0]].asInstanceOf[Fix[S,I,_0]]
      val bb = b.asInstanceOf[Fix[SB,IB,_0]].to[Fix[SC,IC,_0]].asInstanceOf[Fix[S,I,_0]]
      func(aa,bb).asInstanceOf[Idx]
    }
    @api def plus(a: Idx, b: Idx): Idx =  map[Any,Any](a,b)(_+_)
    @api def minus(a: Idx, b: Idx): Idx = map[Any,Any](a,b)(_-_)
    @api def times(a: Idx, b: Idx): Idx = map[Any,Any](a,b)(_*_)
    @api def divide(a: Idx, b: Idx): Idx = map[Any,Any](a,b)(_/_)
    @api def modulus(a: Idx, b: Idx): Idx = map[Any,Any](a,b)(_%_)
    def fromInt(a: Int): Idx = I32(a)
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
