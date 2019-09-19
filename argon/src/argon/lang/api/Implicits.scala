package argon.lang.api

import argon._
import argon.node._
import forge.VarLike
import forge.tags._

import scala.reflect.{ClassTag, classTag}

/** Implicit conversions and methods.
  * Note that these are ordered by priority from lowest to highest via Scala's oh so intuitive
  * method of implicit priority search by trait mix-in/inheritance level.
  */

trait ImplicitsPriority3 extends Serializable { this: Implicits =>
  implicit def numericCast[A:Num,B:Num]: Cast[A,B] = Right(new CastFunc[A,B]{
    @api def apply(a: A): B = (Num[B] match {
      case tp:Fix[s,i,f] =>
        implicit val S: BOOL[s] = tp.fmt.s
        implicit val I: INT[i] = tp.fmt.i
        implicit val F: INT[f] = tp.fmt.f
        a.__toFix[s,i,f]

      case tp:Flt[m,e] =>
        implicit val M: INT[m] = tp.fmt.m
        implicit val E: INT[e] = tp.fmt.e
        a.__toFlt[m,e]
    }).asInstanceOf[B]

    @rig override def saturating(a: A): B = (Num[B] match {
      case tp:Fix[s,i,f] => 
        implicit val S: BOOL[s] = tp.fmt.s
        implicit val I: INT[i] = tp.fmt.i
        implicit val F: INT[f] = tp.fmt.f
        a.__toFixSat[s,i,f]
      case tp:Flt[m,e] => apply(a)
    }).asInstanceOf[B]

    @rig override def unbiased(a: A): B = (Num[B] match {
      case tp:Fix[s,i,f] => 
        implicit val S: BOOL[s] = tp.fmt.s
        implicit val I: INT[i] = tp.fmt.i
        implicit val F: INT[f] = tp.fmt.f
        a.__toFixUnb[s,i,f]

      case tp:Flt[m,e] => apply(a)
    }).asInstanceOf[B]
    
    @rig override def unbsat(a: A): B = (Num[B] match {
      case tp:Fix[s,i,f] => 
        implicit val S: BOOL[s] = tp.fmt.s
        implicit val I: INT[i] = tp.fmt.i
        implicit val F: INT[f] = tp.fmt.f
        a.__toFixUnbSat[s,i,f]

      case tp:Flt[m,e] => apply(a)
    }).asInstanceOf[B]

  })


  // --- A (Any)
  implicit class VirtualizeAnyMethods[A](a: A) {
    @rig def infix_toString(): Text = a match {
      case t: Top[_] => t.toText
      case t => Text(t.toString)
    }

    @api def +[B:Arith](that: VarLike[B])(implicit lift: Lifting[A,B]): B = Arith[B].add(lift(a), that.__read)
    @api def -[B:Arith](that: VarLike[B])(implicit lift: Lifting[A,B]): B = Arith[B].sub(lift(a), that.__read)
    @api def *[B:Arith](that: VarLike[B])(implicit lift: Lifting[A,B]): B = Arith[B].mul(lift(a), that.__read)
    @api def /[B:Arith](that: VarLike[B])(implicit lift: Lifting[A,B]): B = Arith[B].div(lift(a), that.__read)
    @api def %[B:Arith](that: VarLike[B])(implicit lift: Lifting[A,B]): B = Arith[B].mod(lift(a), that.__read)

    @api def < [B:Order](that: VarLike[B])(implicit lift: Lifting[A,B]): Bit = Order[B].lt(lift(a), that.__read)
    @api def <=[B:Order](that: VarLike[B])(implicit lift: Lifting[A,B]): Bit = Order[B].leq(lift(a), that.__read)
    @api def > [B:Order](that: VarLike[B])(implicit lift: Lifting[A,B]): Bit = Order[B].gt(lift(a), that.__read)
    @api def >=[B:Order](that: VarLike[B])(implicit lift: Lifting[A,B]): Bit = Order[B].geq(lift(a), that.__read)

    @api def infix_!=[B:Type](that: VarLike[B])(implicit lift: Lifting[A,B]): Bit = lift(a) match {
      case t: Top[_] => t !== that.__read
      case t => Bit(t != that.__read)
    }
    @api def infix_==[B:Type](that: VarLike[B])(implicit lift: Lifting[A,B]): Bit = lift(a) match {
      case t: Top[_] => t === that.__read
      case t => Bit(t == that.__read)
    }
    @rig def infix_!=(rhs: Top[_]): Bit = if (rhs.getFrom(a).isDefined) rhs !== rhs.tp.from(a) else rhs !== a
    @rig def infix_==(rhs: Top[_]): Bit = if (rhs.getFrom(a).isDefined) rhs === rhs.tp.from(a) else rhs !== a
    @rig def infix_!=(rhs: Any): Boolean = a != rhs
    @rig def infix_==(rhs: Any): Boolean = a == rhs

    def infix_##(rhs: Any): Int = a.##
    def infix_equals(rhs: Any): Boolean = a.equals(rhs)
    def infix_hashCode(): Int = a.hashCode()
    def infix_asInstanceOf[T](): T = a.asInstanceOf[T]
    def infix_isInstanceOf[T:ClassTag](): Boolean = utils.isSubtype(a.getClass, classTag[T].runtimeClass)
    def infix_getClass(): Class[_] = a.getClass
  }


  // --- String
  import scala.collection.immutable.WrappedString
  implicit def stringToWrappedString(x: String): WrappedString = new WrappedString(x)

  implicit class VirtualizeStringMethods(lhs: String) {
    @rig def infix_+(rhs: Any): Text = rhs match {
      case t: Top[_] => Text(lhs) ++ t.toText
      case t => Text(lhs + t.toString)
    }
  }


  // --- FixPt
  @api implicit def SeriesFromFix[S:BOOL,I:INT,F:INT](x: Fix[S,I,F]): Series[Fix[S,I,F]] = x.toSeries


  // --- A:Type
  // Shadows name in Predef
  implicit class any2stringadd[A:Type](x: A)(implicit ctx: SrcCtx, state: State) {
    def +(y: Any): Text = (x, y) match {
      case (a: Top[_], b: Top[_]) => a.toText ++ b.toText
      case (a, b: Top[_]) => Text(a.toString) ++ b.toText
      case (a: Top[_], b) => a.toText ++ Text(b.toString)
      case (a, b)         => Text(a.toString) ++ Text(b.toString)
    }
  }

  // Using Lift[A] is always lowest priority
  // This should only be defined if there are multiple ways from the unstaged type to Sym[_]
  @rig implicit def liftByte(b: Byte): Lift[I8] = new Lift[I8](b,b.to[I8])
  @rig implicit def liftChar(b: Char): Lift[U8] = new Lift[U8](b,b.to[U8])
  @rig implicit def liftShort(b: Short): Lift[I16] = new Lift[I16](b,b.to[I16])
  @rig implicit def liftInt(b: Int): Lift[I32] = new Lift[I32](b,b.to[I32])
  @rig implicit def liftLong(b: Long): Lift[I64] = new Lift[I64](b,b.to[I64])
  @rig implicit def liftFloat(b: Float): Lift[F32] = new Lift[F32](b,b.to[F32])
  @rig implicit def liftDouble(b: Double): Lift[F64] = new Lift[F64](b,b.to[F64])

  //@rig implicit def liftBoolean(b: Boolean): Lift[Bit] = new Lift(b,b.to[Bit])
  //@rig implicit def liftString(b: String): Lift[Text] = new Lift[Text](b, Text(b))
}

trait ImplicitsPriority2 extends ImplicitsPriority3 { this: Implicits =>
  implicit def boxNum[A:Num](x: A): Num[A] = Num[A].box(x)

  import scala.runtime.{RichInt,RichChar,RichByte,RichBoolean,RichShort,RichLong}
  import scala.collection.immutable.StringOps
  implicit def boolean2RichBoolean(x: Boolean): RichBoolean = new RichBoolean(x)
  implicit def char2RichChar(x: Char): RichChar = new RichChar(x)
  implicit def byte2RichByte(x: Byte): RichByte = new RichByte(x)
  implicit def short2RichShort(x: Short): RichShort = new RichShort(x)
  implicit def int2RichInt(x: Int): RichInt = new RichInt(x)
  implicit def long2RichLong(x: Long): RichLong = new RichLong(x)
  implicit def stringToStringOps(x: String): StringOps = new StringOps(x)

  // Literal casts are required for literal types which have multiple implicits to Sym
  implicit def litByte(b: Byte): Literal = new Literal(b)
  implicit def litShort(b: Short): Literal = new Literal(b)
  implicit def litInt(b: Int): Literal = new Literal(b)
  implicit def litLong(b: Long): Literal = new Literal(b)
  implicit def litFloat(b: Float): Literal = new Literal(b)
  implicit def litDouble(b: Double): Literal = new Literal(b)
}


trait ImplicitsPriority1 extends ImplicitsPriority2 { this: Implicits =>
  implicit def boxBits[A:Bits](x: A): Bits[A] = Bits[A].box(x)
  implicit def boxOrder[A:Order](x: A): Order[A] = Order[A].box(x)
  implicit def boxArith[A:Arith](x: A): Arith[A] = Arith[A].box(x)

  implicit def selfCast[A:Type]: Cast[A,A] = Right(new CastFunc[A,A] {
    @rig def apply(a: A): A = a
  })

  implicit def wrapString(x: String): Text = Text(x) // Shadows scala.Predef method
}


trait Implicits extends ImplicitsPriority1 {
  implicit def box[A:Type](x: A): Top[A] = Type[A].boxed(x).asInstanceOf[Top[A]]
  implicit class BoxSym[A:Type](x: A) extends ExpMiscOps[Any,A](x)

  // Ways to lift type U to type S:
  //   1. Implicit conversions:
  //        a + 1
  //   1.a. Implicit wrapping:
  //          0 until 10
  //   2. Lifting with no evidence: Lift[U,S]
  //        if (c) 0 else 1: Use
  //   3. Explicit lifting: Cast[U,S]
  //        1.to[I32]


  //=== Bit ===//
  class Cvt_Text_Bit extends Cast2Way[Text,Bit] {
    @rig def apply(x: Text): Bit = stage(TextToBit(x))
    @rig def applyLeft(x: Bit): Text = stage(BitToText(x))
  }
  implicit lazy val CastTextToBit: Cast[Text,Bit] = Right(new Cvt_Text_Bit)
  implicit lazy val CastBitToText: Cast[Bit,Text] = Left(new Cvt_Text_Bit)

  //=== Fix ===//

  class Cvt_Fix_Fix[S1:BOOL,I1:INT,F1:INT,S2:BOOL,I2:INT,F2:INT] extends CastFunc[Fix[S1,I1,F1],Fix[S2,I2,F2]] {
    @rig def apply(x: Fix[S1,I1,F1]): Fix[S2,I2,F2] = stage(FixToFix(x, FixFmt.from[S2,I2,F2]))
    @rig override def saturating(x: Fix[S1,I1,F1]): Fix[S2,I2,F2] = stage(FixToFixSat(x, FixFmt.from[S2,I2,F2]))
    @rig override def unbiased(x: Fix[S1,I1,F1]): Fix[S2,I2,F2] = stage(FixToFixUnb(x, FixFmt.from[S2,I2,F2]))
    @rig override def unbsat(x: Fix[S1,I1,F1]): Fix[S2,I2,F2] = stage(FixToFixUnbSat(x, FixFmt.from[S2,I2,F2]))
    @rig override def getLeft(x: Fix[S2,I2,F2]): Option[Fix[S1,I1,F1]] = Some(stage(FixToFix(x, FixFmt.from[S1,I1,F1])))
  }
  implicit def CastFixToFix[S1:BOOL,I1:INT,F1:INT,S2:BOOL,I2:INT,F2:INT]: Cast[Fix[S1,I1,F1],Fix[S2,I2,F2]] = {
    Right(new Cvt_Fix_Fix[S1,I1,F1,S2,I2,F2])
  }

  class Cvt_Text_Fix[S:BOOL,I:INT,F:INT] extends Cast2Way[Text,Fix[S,I,F]] {
    @rig def apply(x: Text): Fix[S,I,F] = stage(TextToFix(x,FixFmt.from[S,I,F]))
    @rig def applyLeft(x: Fix[S,I,F]): Text = stage(FixToText(x,None))
  }
  implicit def CastTextToFix[S:BOOL,I:INT,F:INT]: Cast[Text,Fix[S,I,F]] = Right(new Cvt_Text_Fix[S,I,F])
  implicit def CastFixToText[S:BOOL,I:INT,F:INT]: Cast[Fix[S,I,F],Text] = Left(new Cvt_Text_Fix[S,I,F])

  //=== Flt ===//

  class Cvt_Flt_Flt[M1:INT,E1:INT,M2:INT,E2:INT] extends CastFunc[Flt[M1,E1],Flt[M2,E2]] {
    @rig def apply(x: Flt[M1,E1]): Flt[M2,E2] = stage(FltToFlt(x, FltFmt.from[M2,E2]))
    @rig override def getLeft(x: Flt[M2,E2]): Option[Flt[M1,E1]] = Some(stage(FltToFlt(x, FltFmt.from[M1,E1])))
  }
  implicit def CastFltToFlt[M1:INT,E1:INT,M2:INT,E2:INT]: Cast[Flt[M1,E1],Flt[M2,E2]] = {
    Right(new Cvt_Flt_Flt[M1,E1,M2,E2])
  }

  class Cvt_Text_Flt[M:INT,E:INT] extends Cast2Way[Text,Flt[M,E]] {
    @rig def apply(x: Text): Flt[M,E] = stage(TextToFlt(x,FltFmt.from[M,E]))
    @rig def applyLeft(x: Flt[M,E]): Text = stage(FltToText(x,None))
  }
  implicit def CastTextToFlt[M:INT,E:INT]: Cast[Text,Flt[M,E]] = Right(new Cvt_Text_Flt[M,E])
  implicit def CastFltToText[M:INT,E:INT]: Cast[Flt[M,E],Text] = Left(new Cvt_Text_Flt[M,E])


  class Cvt_Fix_Flt[S1:BOOL,I1:INT,F1:INT,M2:INT,E2:INT] extends Cast2Way[Fix[S1,I1,F1],Flt[M2,E2]] {
    @rig def apply(a: Fix[S1,I1,F1]): Flt[M2,E2] = stage(FixToFlt(a,FltFmt.from[M2,E2]))
    @rig def applyLeft(b: Flt[M2,E2]): Fix[S1,I1,F1] = stage(FltToFix(b,FixFmt.from[S1,I1,F1]))
  }
  implicit def CastFixToFlt[S1:BOOL,I1:INT,F1:INT,M2:INT,E2:INT]: Cast[Fix[S1,I1,F1],Flt[M2,E2]] = Right(new Cvt_Fix_Flt[S1,I1,F1,M2,E2])
  implicit def CastFltToFix[M1:INT,E1:INT,S2:BOOL,I2:INT,F2:INT]: Cast[Flt[M1,E1],Fix[S2,I2,F2]] = Left(new Cvt_Fix_Flt[S2,I2,F2,M1,E1])


  // --- Implicit Conversions

  class CastType[A](x: A) {
    @api def to[B](implicit cast: Cast[A,B]): B = cast.apply(x)
    @api def toSaturating[B](implicit cast: Cast[A,B]): B = cast.saturating(x)
    @api def toUnbiased[B](implicit cast: Cast[A,B]): B = cast.unbiased(x)
    @api def toUnbSat[B](implicit cast: Cast[A,B]): B = cast.unbsat(x)
  }

  class LiteralWrapper[A](a: A)(implicit ctx: SrcCtx, state: State) {
    def to[B](implicit cast: Cast[A,B]): B = cast(a)
    def toSaturating[B](implicit cast: Cast[A,B]): B = cast.saturating(a)
    def toUnbiased[B](implicit cast: Cast[A,B]): B = cast.unbiased(a)
    def toUnbSat[B](implicit cast: Cast[A,B]): B = cast.unbsat(a)
    def toUnchecked[B](implicit cast: Cast[A,B]): B = cast.unchecked(a)
  }

  // Note: Naming is important here to override the names in Predef.scala
  // Note: Need the ctx and state at the implicit class to avoid issues with currying
  class BooleanWrapper(a: Boolean)(implicit ctx: SrcCtx, state: State) extends LiteralWrapper[Boolean](a)
  class ByteWrapper(a: Byte)(implicit ctx: SrcCtx, state: State) extends LiteralWrapper[Byte](a)
  class CharWrapper(a: Char)(implicit ctx: SrcCtx, state: State) extends LiteralWrapper[Char](a)
  class ShortWrapper(a: Short)(implicit ctx: SrcCtx, state: State) extends LiteralWrapper[Short](a)

  class IntWrapper(b: Int)(implicit ctx: SrcCtx, state: State) extends LiteralWrapper[Int](b) {
    def until(end: I32): Series[I32] = Series[I32](I32(b), end, I32(1), I32(1))
    def by(step: I32): Series[I32] = Series[I32](0, b, step, 1)
    def par(p: I32): Series[I32] = Series[I32](0, b, 1, p)

    def until(end: Int): Series[I32] = Series[I32](b, end, 1, 1)
    def by(step: Int): Series[I32] = Series[I32](0, b, step, 1)
    def par(p: Int): Series[I32] = Series[I32](0, b, 1, p)

    def ::(start: I32): Series[I32] = Series[I32](start, b, 1, 1)
    def ::(start: Int): Series[I32] = Series[I32](start, b, 1, 1)

    def to(end: Int): Range = Range.inclusive(b, end)

    def x: I32 = this.to[I32]
  }

  class LongWrapper(a: Long)(implicit ctx: SrcCtx, state: State) extends LiteralWrapper[Long](a)
  class FloatWrapper(a: Float)(implicit ctx: SrcCtx, state: State) extends LiteralWrapper[Float](a)
  class DoubleWrapper(a: Double)(implicit ctx: SrcCtx, state: State) extends LiteralWrapper[Double](a)


  // ---------------------------------- Scala Types --------------------------------------------- //
  // --- A (Any)
  implicit def castType[A](a: A): CastType[A] = new CastType[A](a)


  // --- Boolean
  implicit lazy val castBooleanToBit: Lifting[Boolean,Bit] = Lifting[Boolean,Bit]
  implicit def CastBooleanToFix[S:BOOL,I:INT,F:INT]: Lifting[Boolean,Fix[S,I,F]] = Lifting[Boolean,Fix[S,I,F]]
  implicit def CastBooleanToFlt[M:INT,E:INT]: Lifting[Boolean,Flt[M,E]] = Lifting[Boolean,Flt[M,E]]
  implicit def CastBooleanToNum[A:Num]: Lifting[Boolean,A] = Lifting[Boolean,A]

  @api implicit def BitFromBoolean(c: Boolean): Bit = c.to[Bit]
  @api implicit def booleanWrapper(c: Boolean): BooleanWrapper = new BooleanWrapper(c)

  // --- Byte
  implicit lazy val castByteToBit: Lifting[Byte,Bit] = Lifting[Byte,Bit]
  implicit def CastByteToFix[S:BOOL,I:INT,F:INT]: Lifting[Byte,Fix[S,I,F]] = Lifting[Byte,Fix[S,I,F]]
  implicit def CastByteToFlt[M:INT,E:INT]: Lifting[Byte,Flt[M,E]] = Lifting[Byte,Flt[M,E]]
  implicit def CastByteToNum[A:Num]: Lifting[Byte,A] = Lifting[Byte,A]

  @api implicit def FixFromByte[S:BOOL,I:INT,F:INT](c: Byte): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def FltFromByte[M:INT,E:INT](c: Byte): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def NumFromByte[A:Num](c: Byte): A = c.to[A]
  @api implicit def byteWrapper(c: Byte): ByteWrapper = new ByteWrapper(c)


  // --- Char
  implicit lazy val castCharToBit: Lifting[Char,Bit] = Lifting[Char,Bit]
  implicit def CastCharToFix[S:BOOL,I:INT,F:INT]: Lifting[Char,Fix[S,I,F]] = Lifting[Char,Fix[S,I,F]]
  implicit def CastCharToFlt[M:INT,E:INT]: Lifting[Char,Flt[M,E]] = Lifting[Char,Flt[M,E]]
  implicit def CastCharToNum[A:Num]: Lifting[Char,A] = Lifting[Char,A]

  @api implicit def FixFromChar[S:BOOL,I:INT,F:INT](c: Char): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def FltFromChar[M:INT,E:INT](c: Char): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def NumFromChar[A:Num](c: Char): A = c.to[A]
  @api implicit def charWrapper(c: Char): CharWrapper = new CharWrapper(c)


  // --- Double
  implicit lazy val castDoubleToBit: Lifting[Double,Bit] = Lifting[Double,Bit]
  implicit def CastDoubleToFix[S:BOOL,I:INT,F:INT]: Lifting[Double,Fix[S,I,F]] = Lifting[Double,Fix[S,I,F]]
  implicit def CastDoubleToFlt[M:INT,E:INT]: Lifting[Double,Flt[M,E]] = Lifting[Double,Flt[M,E]]
  implicit def CastDoubleToNum[A:Num]: Lifting[Double,A] = Lifting[Double,A]

  @api implicit def FixFromDouble[S:BOOL,I:INT,F:INT](c: Double): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def FltFromDouble[M:INT,E:INT](c: Double): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def NumFromDouble[A:Num](c: Double): A = c.to[A]
  @api implicit def doubleWrapper(c: Double): DoubleWrapper = new DoubleWrapper(c)


  // --- Int
  implicit lazy val castIntToBit: Lifting[Int,Bit] = Lifting[Int,Bit]
  implicit def CastIntToFix[S:BOOL,I:INT,F:INT]: Lifting[Int,Fix[S,I,F]] = Lifting[Int,Fix[S,I,F]]
  implicit def CastIntToFlt[M:INT,E:INT]: Lifting[Int,Flt[M,E]] = Lifting[Int,Flt[M,E]]
  implicit def CastIntToNum[A:Num]: Lifting[Int,A] = Lifting[Int,A]

  @api implicit def FltFromInt[M:INT,E:INT](c: Int): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def FixFromInt[S:BOOL,I:INT,F:INT](c: Int): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def NumFromInt[A:Num](c: Int): A = c.to[A]
  @api implicit def intWrapper(c: Int): IntWrapper = new IntWrapper(c)


  // --- Long
  implicit lazy val castLongToBit: Lifting[Long,Bit] = Lifting[Long,Bit]
  implicit def CastLongToFix[S:BOOL,I:INT,F:INT]: Lifting[Long,Fix[S,I,F]] = Lifting[Long,Fix[S,I,F]]
  implicit def CastLongToFlt[M:INT,E:INT]: Lifting[Long,Flt[M,E]] = Lifting[Long,Flt[M,E]]
  implicit def CastLongToNum[A:Num]: Lifting[Long,A] = Lifting[Long,A]

  @api implicit def FixFromLong[S:BOOL,I:INT,F:INT](c: Long): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def FltFromLong[M:INT,E:INT](c: Long): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def NumFromLong[A:Num](c: Long): A = c.to[A]
  @api implicit def longWrapper(c: Long): LongWrapper = new LongWrapper(c)


  // --- Float
  implicit lazy val castFloatToBit: Lifting[Float,Bit] = Lifting[Float,Bit]
  implicit def CastFloatToFix[S:BOOL,I:INT,F:INT]: Lifting[Float,Fix[S,I,F]] = Lifting[Float,Fix[S,I,F]]
  implicit def CastFloatToFlt[M:INT,E:INT]: Lifting[Float,Flt[M,E]] = Lifting[Float,Flt[M,E]]
  implicit def CastFloatToNum[A:Num]: Lifting[Float,A] = Lifting[Float,A]

  @api implicit def FixFromFloat[S:BOOL,I:INT,F:INT](c: Float): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def FltFromFloat[M:INT,E:INT](c: Float): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def NumFromFloat[A:Num](c: Float): A = c.to[A]
  @api implicit def floatWrapper(c: Float): FloatWrapper = new FloatWrapper(c)


  // --- Short
  implicit lazy val castShortToBit: Lifting[Short,Bit] = Lifting[Short,Bit]
  implicit def CastShortToFix[S:BOOL,I:INT,F:INT]: Lifting[Short,Fix[S,I,F]] = Lifting[Short,Fix[S,I,F]]
  implicit def CastShortToFlt[M:INT,E:INT]: Lifting[Short,Flt[M,E]] = Lifting[Short,Flt[M,E]]
  implicit def CastShortToNum[A:Num]: Lifting[Short,A] = Lifting[Short,A]

  @api implicit def FltFromShort[M:INT,E:INT](c: Short): Flt[M,E] = c.to[Flt[M,E]]
  @api implicit def FixFromShort[S:BOOL,I:INT,F:INT](c: Short): Fix[S,I,F] = c.to[Fix[S,I,F]]
  @api implicit def NumFromShort[A:Num](c: Short): A = c.to[A]
  @api implicit def ShortWrapper(c: Short): ShortWrapper = new ShortWrapper(c)


  // --- String
  implicit lazy val castStringToText: Lifting[String,Text] = Lifting[String,Text]

  implicit def augmentString(x: String): Text = Text(x) // Shadows scala.Predef method


  // --- Unit
  implicit def VoidFromUnit(c: Unit): Void = Void.c


  // --- VarLike
  @api implicit def varRead[A](v: VarLike[A]): A = v.__read

}
