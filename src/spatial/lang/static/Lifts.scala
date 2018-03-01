package spatial.lang
package static

import core._
import forge.tags._
import forge.util.overloads._
import nova.data.rangeOf

trait LiftsPriority3 { this: Lifts =>
  import scala.collection.immutable.WrappedString
  implicit def stringToWrappedString(x: String): WrappedString = new WrappedString(x)
}

trait LiftsPriority2 { this: Lifts =>
  import scala.runtime.{RichInt,RichByte,RichBoolean,RichShort,RichLong}
  import scala.collection.immutable.StringOps
  implicit def boolean2RichBoolean(x: Boolean): RichBoolean = new RichBoolean(x)
  implicit def byte2RichByte(x: Byte): RichByte = new RichByte(x)
  implicit def short2RichShort(x: Short): RichShort = new RichShort(x)
  implicit def int2RichInt(x: Int): RichInt = new RichInt(x)
  implicit def long2RichLong(x: Long): RichLong = new RichLong(x)

  @api implicit def FixFromFloat[F:FixFmt](c: Float): Fix[F] = c.to[Fix[F]]
  @api implicit def FixFromDouble[F:FixFmt](c: Double): Fix[F] = c.to[Fix[F]]

  implicit def stringToStringOps(x: String): StringOps = new StringOps(x)
}

trait LiftsPriority1 extends LiftsPriority2 { this: Lifts =>
  @api implicit def FixFromInt[F:FixFmt](c: Int): Fix[F] = c.to[Fix[F]]

  @api implicit def FltFromByte[F:FltFmt](c: Byte): Flt[F] = c.to[Flt[F]]
  @api implicit def FltFromShort[F:FltFmt](c: Short): Flt[F] = c.to[Flt[F]]
  @api implicit def FltFromLong[F:FltFmt](c: Long): Flt[F] = c.to[Flt[F]]

  @api implicit def SeriesFromFix[F:FixFmt](x: Fix[F]): Series[Fix[F]] = Series[Fix[F]](x, x+1, 1, 1, isUnit=true)

  // Shadows Predef method
  @api implicit def wrapString(x: String): Text = Text.c(x)

  @rig implicit def selfLift[A:Type](a: A): Lift[A] = new Lift[A](a)
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

  @api implicit def BitFromBoolean(c: Boolean): Bit = c.to[Bit]

  @api implicit def FixFromByte[F:FixFmt](c: Byte): Fix[F] = c.to[Fix[F]]
  @api implicit def FixFromShort[F:FixFmt](c: Short): Fix[F] = c.to[Fix[F]]
  @api implicit def FixFromLong[F:FixFmt](c: Long): Fix[F] = c.to[Fix[F]]

  @api implicit def FltFromFloat[F:FltFmt](c: Float): Flt[F] = c.to[Flt[F]]
  @api implicit def FltFromDouble[F:FltFmt](c: Double): Flt[F] = c.to[Flt[F]]

  @api implicit def VoidFromUnit(c: Unit): Void = Void.c

  @api implicit def SeriesToCounter(x: Series[I32]): Counter = Counter.from(x)


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

  class IntWrapper(x: Int)(implicit ctx: SrcCtx, state: State) {
    def until(end: I32): Series[I32] = Series[I32](I32.c(x), end, I32.c(1), I32.c(1))
    def by(step: I32): Series[I32] = Series[I32](1, x, step, 1)
    def par(p: I32): Series[I32] = Series[I32](1, x, 1, p)

    def until(end: Int): Series[I32] = Series[I32](x, end, 1, 1)
    def by(step: Int): Series[I32] = Series[I32](1, x, step, 1)
    def par(p: Int): Series[I32] = Series[I32](1, x, 1, p)

    def ::(start: I32): Series[I32] = Series[I32](start, x, 1, 1)
    def ::(start: Int): Series[I32] = Series[I32](start, x, 1, 1)

    /**
      * Creates a parameter with this value as the default, and the given range with a stride of 1.
      *
      * ``1 (1 -> 5)``
      * creates a parameter with a default of 1 with a range [1,5].
      */
    def apply(range: (Int, Int))(implicit ov1: Overload0): I32 = createParam(x, range._1, 1, range._2)
    /**
      * Creates a parameter with this value as the default, and the given strided range.
      *
      * ``1 (1 -> 2 -> 8)``
      * creates a parameter with a default of 1 with a range in [2,8] with step of 4.
      */
    def apply(range: ((Int, Int), Int))(implicit ov2: Overload1): I32 = createParam(x, range._1._1, range._1._2, range._2)


    def to[B](implicit cast: Cast[Int,B]): B = cast(x)
  }
  @rig implicit def intWrapper(x: Int): IntWrapper = new IntWrapper(x)

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
  @rig implicit def liftBoolean(b: Boolean): Lift[Bit] = new Lift(b.to[Bit])
  class Cvt_Boolean_Bit extends Lifter[Boolean,Bit]
  class Cvt_Boolean_Fix[F:FixFmt] extends Lifter[Boolean,Fix[F]]
  class Cvt_Boolean_Flt[F:FltFmt] extends Lifter[Boolean,Flt[F]]

  implicit lazy val castBooleanToBit: Cast[Boolean,Bit] = Right(new Cvt_Boolean_Bit)
  implicit def CastBooleanToFix[F:FixFmt]: Cast[Boolean,Fix[F]] = Right(new Cvt_Boolean_Fix[F])
  implicit def CastBooleanToFlt[F:FltFmt]: Cast[Boolean,Flt[F]] = Right(new Cvt_Boolean_Flt[F])


  // --- Byte
  @rig implicit def liftByte(b: Byte): Lift[I8] = new Lift[I8](b.to[I8])
  class Cvt_Byte_Fix[F:FixFmt] extends Lifter[Byte,Fix[F]]
  class Cvt_Byte_Flt[F:FltFmt] extends Lifter[Byte,Flt[F]]
  implicit def CastByteToFix[F:FixFmt]: Cast[Byte,Fix[F]] = Right(new Cvt_Byte_Fix[F])
  implicit def CastByteToFlt[F:FltFmt]: Cast[Byte,Flt[F]] = Right(new Cvt_Byte_Flt[F])


  // --- Short
  @rig implicit def liftShort(b: Short): Lift[I16] = new Lift[I16](b.to[I16])
  class Cvt_Short_Fix[F:FixFmt] extends Lifter[Short,Fix[F]]
  class Cvt_Short_Flt[F:FltFmt] extends Lifter[Short,Flt[F]]
  implicit def CastShortToFix[F:FixFmt]: Cast[Short,Fix[F]] = Right(new Cvt_Short_Fix[F])
  implicit def CastShortToFlt[F:FltFmt]: Cast[Short,Flt[F]] = Right(new Cvt_Short_Flt[F])


  // --- Int
  @rig implicit def liftInt(b: Int): Lift[I32] = new Lift[I32](b.to[I32])
  class Cvt_Int_Fix[F:FixFmt] extends Lifter[Int,Fix[F]]
  class Cvt_Int_Flt[F:FltFmt] extends Lifter[Int,Flt[F]]
  implicit def CastIntToFix[F:FixFmt]: Cast[Int,Fix[F]] = Right(new Cvt_Int_Fix[F])
  implicit def CastIntToFlt[F:FltFmt]: Cast[Int,Flt[F]] = Right(new Cvt_Int_Flt[F])


  // --- Long
  @rig implicit def liftLong(b: Long): Lift[I64] = new Lift[I64](b.to[I64])
  class Cvt_Long_Fix[F:FixFmt] extends Lifter[Long,Fix[F]]
  class Cvt_Long_Flt[F:FltFmt] extends Lifter[Long,Flt[F]]
  implicit def CastLongToFix[F:FixFmt]: Cast[Long,Fix[F]] = Right(new Cvt_Long_Fix[F])
  implicit def CastLongToFlt[F:FltFmt]: Cast[Long,Flt[F]] = Right(new Cvt_Long_Flt[F])


  // --- Float
  @rig implicit def liftFloat(b: Float): Lift[F32] = new Lift[F32](b.to[F32])
  class Cvt_Float_Fix[F:FixFmt] extends Lifter[Float,Fix[F]]
  class Cvt_Float_Flt[F:FltFmt] extends Lifter[Float,Flt[F]]
  implicit def CastFloatToFix[F:FixFmt]: Cast[Float,Fix[F]] = Right(new Cvt_Float_Fix[F])
  implicit def CastFloatToFlt[F:FltFmt]: Cast[Float,Flt[F]] = Right(new Cvt_Float_Flt[F])


  // --- Double
  @rig implicit def liftDouble(b: Double): Lift[F64] = new Lift[F64](b.to[F64])
  class Cvt_Double_Fix[F:FixFmt] extends Lifter[Double,Fix[F]]
  class Cvt_Double_Flt[F:FltFmt] extends Lifter[Double,Flt[F]]
  implicit def CastDoubleToFix[F:FixFmt]: Cast[Double,Fix[F]] = Right(new Cvt_Double_Fix[F])
  implicit def CastDoubleToFlt[F:FltFmt]: Cast[Double,Flt[F]] = Right(new Cvt_Double_Flt[F])

}
