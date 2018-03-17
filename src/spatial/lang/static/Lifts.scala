package spatial.lang
package static

import core._
import forge.tags._
import utils.Overloads._
import spatial.data.rangeOf

trait LiftsPriority3 { this: Lifts =>
  import scala.collection.immutable.WrappedString
  implicit def stringToWrappedString(x: String): WrappedString = new WrappedString(x)
}

trait LiftsPriority2 extends LiftsPriority3 { this: Lifts =>
  import scala.runtime.{RichInt,RichByte,RichBoolean,RichShort,RichLong}
  import scala.collection.immutable.StringOps
  implicit def boolean2RichBoolean(x: Boolean): RichBoolean = new RichBoolean(x)
  implicit def byte2RichByte(x: Byte): RichByte = new RichByte(x)
  implicit def short2RichShort(x: Short): RichShort = new RichShort(x)
  implicit def int2RichInt(x: Int): RichInt = new RichInt(x)
  implicit def long2RichLong(x: Long): RichLong = new RichLong(x)
  implicit def stringToStringOps(x: String): StringOps = new StringOps(x)

  // Using Lift[A] is always lowest priority
  @rig implicit def liftBoolean(b: Boolean): Lift[Bit] = new Lift(b,b.to[Bit])
  @rig implicit def liftByte(b: Byte): Lift[I8] = new Lift[I8](b,b.to[I8])
  @rig implicit def liftShort(b: Short): Lift[I16] = new Lift[I16](b,b.to[I16])
  @rig implicit def liftInt(b: Int): Lift[I32] = new Lift[I32](b,b.to[I32])
  @rig implicit def liftLong(b: Long): Lift[I64] = new Lift[I64](b,b.to[I64])
  @rig implicit def liftFloat(b: Float): Lift[F32] = new Lift[F32](b,b.to[F32])
  @rig implicit def liftDouble(b: Double): Lift[F64] = new Lift[F64](b,b.to[F64])

}

trait LiftsPriority1 extends LiftsPriority2 { this: Lifts =>

  @api implicit def SeriesFromFix[S:BOOL,I:INT,F:INT](x: Fix[S,I,F]): Series[Fix[S,I,F]] = x.toSeries

  // Shadows Predef method
  @api implicit def wrapString(x: String): Text = Text.c(x)

  @api implicit def FixFromInt[S:BOOL,I:INT,F:INT](c: Int): Fix[S,I,F] = c.to[Fix[S,I,F]]
}

trait Lifts extends LiftsPriority1 {
  // Ways to lift type U to type S:
  //   1. Implicit lifting:  Implicit defs
  //        a + 1
  //   2. Lifting on infix methods: Use wrappers
  //        0 until 10
  //   3. Lifting with no evidence: Lift[U,S]
  //        if (c) 0 else 1: Use
  //   4. Explicit lifting: Cast[U,S]
  //        1.to[I32]


  // --- Implicits defs

  @api implicit def FixFromFloat[S:BOOL,I:INT,F:INT](c: Float): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def FixFromDouble[S:BOOL,I:INT,F:INT](c: Double): Fix[S,I,F] = c.to[Fix[S,I,F]]

  @api implicit def FltFromByte[M:INT,E:INT](c: Byte): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def FltFromShort[M:INT,E:INT](c: Short): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def FltFromLong[M:INT,E:INT](c: Long): Flt[M,E] = c.to[Flt[M,E]]

  @api implicit def BitFromBoolean(c: Boolean): Bit = c.to[Bit]

  @api implicit def FixFromByte[S:BOOL,I:INT,F:INT](c: Byte): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def FixFromShort[S:BOOL,I:INT,F:INT](c: Short): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def FixFromLong[S:BOOL,I:INT,F:INT](c: Long): Fix[S,I,F] = c.to[Fix[S,I,F]]

  @api implicit def FltFromFloat[M:INT,E:INT](c: Float): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def FltFromDouble[M:INT,E:INT](c: Double): Flt[M,E] = c.to[Flt[M,E]]

  @api implicit def VoidFromUnit(c: Unit): Void = Void.c

  @api implicit def SeriesToCounter[S:BOOL,I:INT,F:INT](x: Series[Fix[S,I,F]]): Counter[Fix[S,I,F]] = Counter.from(x)


  @rig def createParam(default: Int, start: Int, stride: Int, end: Int): I32 = {
    val p = I32.p(default)
    rangeOf(p) = (start, stride, end)
    p
  }

  // Note: Naming is important here to override the names in Predef.scala
  // Note: Need the ctx and state at the implicit class to avoid issues with currying
  class BooleanWrapper(b: Boolean)(implicit ctx: SrcCtx, state: State) {
    def to[B](implicit cast: Cast[Boolean,B]): B = cast(b)
  }
  @rig implicit def booleanWrapper(b: Boolean): BooleanWrapper = new BooleanWrapper(b)

  class ByteWrapper(b: Byte)(implicit ctx: SrcCtx, state: State) {
    def to[B](implicit cast: Cast[Byte,B]): B = cast(b)
  }
  @rig implicit def byteWrapper(b: Byte): ByteWrapper = new ByteWrapper(b)

  class ShortWrapper(b: Short)(implicit ctx: SrcCtx, state: State) {
    def to[B](implicit cast: Cast[Short,B]): B = cast(b)
  }
  @rig implicit def ShortWrapper(b: Short): ShortWrapper = new ShortWrapper(b)

  class IntWrapper(v: Int)(implicit ctx: SrcCtx, state: State) {
    def until(end: I32): Series[I32] = Series[I32](I32(v), end, I32(1), I32(1))
    def by(step: I32): Series[I32] = Series[I32](1, v, step, 1)
    def par(p: I32): Series[I32] = Series[I32](1, v, 1, p)

    def until(end: Int): Series[I32] = Series[I32](v, end, 1, 1)
    def by(step: Int): Series[I32] = Series[I32](1, v, step, 1)
    def par(p: Int): Series[I32] = Series[I32](1, v, 1, p)

    def ::(start: I32): Series[I32] = Series[I32](start, v, 1, 1)
    def ::(start: Int): Series[I32] = Series[I32](start, v, 1, 1)

    /**
      * Creates a parameter with this value as the default, and the given range with a stride of 1.
      *
      * ``1 (1 -> 5)``
      * creates a parameter with a default of 1 with a range [1,5].
      */
    def apply(range: (Int, Int))(implicit ov1: Overload0): I32 = createParam(v, range._1, 1, range._2)
    /**
      * Creates a parameter with this value as the default, and the given strided range.
      *
      * ``1 (1 -> 2 -> 8)``
      * creates a parameter with a default of 1 with a range in [2,8] with step of 4.
      */
    def apply(range: ((Int, Int), Int))(implicit ov2: Overload1): I32 = createParam(v, range._1._1, range._1._2, range._2)

    def to(end: Int): Series[I32] = Series[I32](v, end+1, 1, 1)
    def to(end: I32): Series[I32] = Series[I32](v, end+1, 1, 1)
    def to[B](implicit cast: Cast[Int,B]): B = cast(v)

    def x: I32 = this.to[I32]
  }
  @rig implicit def intWrapper(v: Int): IntWrapper = new IntWrapper(v)

  class LongWrapper(b: Long)(implicit ctx: SrcCtx, state: State) {
    def to[B](implicit cast: Cast[Long,B]): B = cast(b)
  }
  @rig implicit def longWrapper(b: Long): LongWrapper = new LongWrapper(b)

  class FloatWrapper(b: Float)(implicit ctx: SrcCtx, state: State) {
    def to[B](implicit cast: Cast[Float,B]): B = cast(b)
  }
  @rig implicit def floatWrapper(b: Float): FloatWrapper = new FloatWrapper(b)

  class DoubleWrapper(b: Double)(implicit ctx: SrcCtx, state: State) {
    def to[B](implicit cast: Cast[Double,B]): B = cast(b)
  }
  @rig implicit def doubleWrapper(b: Double): DoubleWrapper = new DoubleWrapper(b)

  @api implicit def augmentString(x: String): Text = Text.c(x)

  // --- Boolean
  implicit lazy val castBooleanToBit: Cast[Boolean,Bit] = Right(new Lifter[Boolean,Bit])
  implicit def CastBooleanToFix[S:BOOL,I:INT,F:INT]: Cast[Boolean,Fix[S,I,F]] = Right(new Lifter[Boolean,Fix[S,I,F]])
  implicit def CastBooleanToFlt[M:INT,E:INT]: Cast[Boolean,Flt[M,E]] = Right(new Lifter[Boolean,Flt[M,E]])
  implicit def CastBooleanToNum[A:Num]: Cast[Boolean,A] = Right(new Lifter[Boolean,A])

  // --- Byte
  implicit lazy val castByteToBit: Cast[Byte,Bit] = Right(new Lifter[Byte,Bit])
  implicit def CastByteToFix[S:BOOL,I:INT,F:INT]: Cast[Byte,Fix[S,I,F]] = Right(new Lifter[Byte,Fix[S,I,F]])
  implicit def CastByteToFlt[M:INT,E:INT]: Cast[Byte,Flt[M,E]] = Right(new Lifter[Byte,Flt[M,E]])
  implicit def CastByteToNum[A:Num]: Cast[Byte,A] = Right(new Lifter[Byte,A])

  // --- Short
  implicit lazy val castShortToBit: Cast[Short,Bit] = Right(new Lifter[Short,Bit])
  implicit def CastShortToFix[S:BOOL,I:INT,F:INT]: Cast[Short,Fix[S,I,F]] = Right(new Lifter[Short,Fix[S,I,F]])
  implicit def CastShortToFlt[M:INT,E:INT]: Cast[Short,Flt[M,E]] = Right(new Lifter[Short,Flt[M,E]])
  implicit def CastShortToNum[A:Num]: Cast[Short,A] = Right(new Lifter[Short,A])

  // --- Int
  implicit lazy val castIntToBit: Cast[Int,Bit] = Right(new Lifter[Int,Bit])
  implicit def CastIntToFix[S:BOOL,I:INT,F:INT]: Cast[Int,Fix[S,I,F]] = Right(new Lifter[Int,Fix[S,I,F]])
  implicit def CastIntToFlt[M:INT,E:INT]: Cast[Int,Flt[M,E]] = Right(new Lifter[Int,Flt[M,E]])
  implicit def CastIntToNum[A:Num]: Cast[Int,A] = Right(new Lifter[Int,A])

  // --- Long
  implicit lazy val castLongToBit: Cast[Long,Bit] = Right(new Lifter[Long,Bit])
  implicit def CastLongToFix[S:BOOL,I:INT,F:INT]: Cast[Long,Fix[S,I,F]] = Right(new Lifter[Long,Fix[S,I,F]])
  implicit def CastLongToFlt[M:INT,E:INT]: Cast[Long,Flt[M,E]] = Right(new Lifter[Long,Flt[M,E]])
  implicit def CastLongToNum[A:Num]: Cast[Long,A] = Right(new Lifter[Long,A])

  // --- Float
  implicit lazy val castFloatToBit: Cast[Float,Bit] = Right(new Lifter[Float,Bit])
  implicit def CastFloatToFix[S:BOOL,I:INT,F:INT]: Cast[Float,Fix[S,I,F]] = Right(new Lifter[Float,Fix[S,I,F]])
  implicit def CastFloatToFlt[M:INT,E:INT]: Cast[Float,Flt[M,E]] = Right(new Lifter[Float,Flt[M,E]])
  implicit def CastFloatToNum[A:Num]: Cast[Float,A] = Right(new Lifter[Float,A])

  // --- Double
  implicit lazy val castDoubleToBit: Cast[Double,Bit] = Right(new Lifter[Double,Bit])
  implicit def CastDoubleToFix[S:BOOL,I:INT,F:INT]: Cast[Double,Fix[S,I,F]] = Right(new Lifter[Double,Fix[S,I,F]])
  implicit def CastDoubleToFlt[M:INT,E:INT]: Cast[Double,Flt[M,E]] = Right(new Lifter[Double,Flt[M,E]])
  implicit def CastDoubleToNum[A:Num]: Cast[Double,A] = Right(new Lifter[Double,A])

}
