package argon.lang

import argon._
import argon.node._
import emul.{FixFormat, FixedPoint, FloatPoint}
import forge.tags._

case class FixFmt[S,I,F](s: BOOL[S], i: INT[I], f: INT[F]) {
  implicit val S: BOOL[S] = s
  implicit val I: INT[I] = i
  implicit val F: INT[F] = f
  def sign: Boolean = s.v
  def ibits: Int = i.v
  def fbits: Int = f.v
  @rig def nbits: Int = ibits + fbits
  def toEmul: FixFormat = FixFormat(sign,ibits,fbits)
  def merge(that: FixFmt[_,_,_]): FixFmt[_,_,_] = {
    val fmt = this.toEmul.combine(that.toEmul)
    new FixFmt(BOOL.from(fmt.sign),INT.from(fmt.ibits),INT.from(fmt.fbits))
  }
  override def toString: String = s"$s,$i,$f"
}
object FixFmt {
  def from[S:BOOL,I:INT,F:INT]: FixFmt[S,I,F] = new FixFmt[S,I,F](BOOL[S],INT[I],INT[F])
}


/** Staged fixed point type
  *
  * @tparam S Signed or unsigned
  * @tparam I Number of integer bits
  * @tparam F Number of fraction bits
  */
@ref class Fix[S:BOOL,I:INT,F:INT] extends Top[Fix[S,I,F]] with Num[Fix[S,I,F]] with Ref[FixedPoint,Fix[S,I,F]] {
  val box = implicitly[Fix[S,I,F] <:< Num[Fix[S,I,F]]]
  val fmt = FixFmt.from[S,I,F]
  type ST = S
  type IT = I
  type FT = F
  type FixType = FixPt[S,I,F]
  override protected val __typeParams: Seq[Any] = Seq(fmt)

  def sign: Boolean = fmt.sign
  def ibits: Int = fmt.ibits
  def fbits: Int = fmt.fbits
  @rig def nbits: Int = ibits + fbits

  // --- Infix Methods
  @api def unary_-(): Fix[S,I,F] = stage(FixNeg(this))
  @api def unary_~(): Fix[S,I,F] = stage(FixInv(this))
  @api def &(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixAnd(this,that))
  @api def |(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixOr(this,that))
  @api def ^(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixXor(this,that))

  @api def +(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixAdd(this,that))
  @api def -(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixSub(this,that))
  @api def *(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixMul(this,that))
  @api def /(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixDiv(this,that))
  @api def %(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixMod(this,that))
  @api def <<(that: Fix[TRUE,_16,_0]): Fix[S,I,F] = stage(FixSLA(this,that))
  @api def >>(that: Fix[TRUE,_16,_0]): Fix[S,I,F] = stage(FixSRA(this,that))
  @api def >>>(that: Fix[TRUE,_16,_0]): Fix[S,I,F] = stage(FixSRU(this,that))

  @api def <(that: Fix[S,I,F]): Bit = stage(FixLst(this,that))
  @api def <=(that: Fix[S,I,F]): Bit = stage(FixLeq(this,that))


  @api override def neql(that: Fix[S,I,F]): Bit = stage(FixNeq(this,that))
  @api override def eql(that: Fix[S,I,F]): Bit = stage(FixEql(this,that))


  // --- Typeclass Methods
  @rig def random(max: Option[Fix[S,I,F]]): Fix[S,I,F] = stage(FixRandom(max))

  @rig def min(a: Fix[S,I,F], b: Fix[S,I,F]): Fix[S,I,F] = stage(FixMin(a,b))
  @rig def max(a: Fix[S,I,F], b: Fix[S,I,F]): Fix[S,I,F] = stage(FixMax(a,b))


  /**
    * Fixed point multiplication with unbiased rounding.
    *
    * After multiplication, probabilistically rounds up or down to the closest representable number.
    */
  @api def *& (that: Fix[S,I,F]): Fix[S,I,F] = stage(UnbMul(this,that))

  /**
    * Fixed point division with unbiased rounding.
    *
    * After division, probabilistically rounds up or down to the closest representable number.
    */
  @api def /& (that: Fix[S,I,F]): Fix[S,I,F] = stage(UnbDiv(this,that))

  // Saturating operators
  /**
    * Saturating fixed point addition.
    *
    * Addition which saturates at the largest or smallest representable number upon over/underflow.
    */
  @api def +! (that: Fix[S,I,F]): Fix[S,I,F] = stage(SatAdd(this,that))
  /**
    * Saturating fixed point subtraction.
    *
    * Subtraction which saturates at the largest or smallest representable number upon over/underflow.
    */
  @api def -! (that: Fix[S,I,F]): Fix[S,I,F] = stage(SatSub(this,that))
  /**
    * Saturating fixed point multiplication.
    *
    * Multiplication which saturates at the largest or smallest representable number upon over/underflow.
    */
  @api def *! (that: Fix[S,I,F]): Fix[S,I,F] = stage(SatMul(this,that))
  /**
    * Saturating fixed point division.
    *
    * Division which saturates at the largest or smallest representable number upon over/underflow.
    */
  @api def /! (that: Fix[S,I,F]): Fix[S,I,F] = stage(SatDiv(this,that))

  // Saturating and unbiased rounding operators
  /**
    * Saturating fixed point multiplication with unbiased rounding.
    *
    * After multiplication, probabilistically rounds up or down to the closest representable number.
    * After rounding, also saturates at the largest or smallest representable number upon over/underflow.
    */
  @api def *&! (that: Fix[S,I,F]): Fix[S,I,F] = stage(UnbSatMul(this,that))
  /**
    * Saturating fixed point division with unbiased rounding.
    *
    * After division, probabilistically rounds up or down to the closest representable number.
    * After rounding, also saturates at the largest or smallest representable number upon over/underflow.
    */
  @api def /&! (that: Fix[S,I,F]): Fix[S,I,F] = stage(UnbSatDiv(this,that))

  @rig def abs(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixAbs(a))
  @rig def ceil(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixCeil(a))
  @rig def floor(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixFloor(a))
  @rig def pow(b: Fix[S,I,F], e: Fix[S,I,F]): Fix[S,I,F] = stage(FixPow(b,e))
  @rig def exp(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixExp(a))
  @rig def ln(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixLn(a))
  @rig def sqrt(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixSqrt(a))
  @rig def sin(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixSin(a))
  @rig def cos(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixCos(a))
  @rig def tan(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixTan(a))
  @rig def sinh(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixSinh(a))
  @rig def cosh(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixCosh(a))
  @rig def tanh(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixTanh(a))
  @rig def asin(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixAsin(a))
  @rig def acos(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixAcos(a))
  @rig def atan(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixAtan(a))
  @rig def sigmoid(a: Fix[S,I,F]): Fix[S,I,F] = stage(FixSigmoid(a))

  lazy val efmt: FixFormat = fmt.toEmul
  override protected def value(c: Any) = c match {
    case x: BigDecimal => withCheck(FixedPoint(x,efmt)){ _.toBigDecimal == x }
    case x: BigInt     => withCheck(FixedPoint(x,efmt)){ _.toBigInt == x }
    case x: Boolean    => Some(FixedPoint(x,efmt),true)
    case x: Char       => withCheck(FixedPoint(x,efmt)){ _.toInt == x }
    case x: Byte       => withCheck(FixedPoint(x,efmt)){ _.toByte == x }
    case x: Short      => withCheck(FixedPoint(x,efmt)){ _.toShort == x }
    case x: Int        => withCheck(FixedPoint(x,efmt)){ _.toInt == x }
    case x: Long       => withCheck(FixedPoint(x,efmt)){ _.toLong == x }
    case x: Float      => withCheck(FixedPoint(x,efmt)){ _.toFloat == x }
    case x: Double     => withCheck(FixedPoint(x,efmt)){ _.toDouble == x }
    case x: String     => withCheck(FixedPoint(x,efmt)){ _.toBigDecimal == BigDecimal(x) }
    case x: FixedPoint if x.fmt == efmt => Some(x,true)
    case x: FixedPoint => withCheck(x.toFixedPoint(efmt)){ _.toFixedPoint(x.fmt) == x }
    case x: FloatPoint => withCheck(x.toFixedPoint(efmt)){ _.toFloatPoint(x.fmt) == x }
    case _ => super.value(c)
  }
  override protected def extract: Option[Any] = this.c match {
    case Some(x) if x.isExactInt => Some(x.toInt)
    case Some(x) if x.isExactLong => Some(x.toLong)
    case Some(x) if x.isExactFloat => Some(x.toFloat)
    case Some(x) if x.isExactDouble => Some(x.toDouble)
    case c => c
  }

  def uconst(c: Int): Fix[S,I,F] = uconst(FixedPoint.fromInt(c))

  /** Returns a string of ones interpreted as this value's format. */
  def ones: Fix[S,I,F] = {
    uconst(FixedPoint.fromBits(Array.fill(ibits+fbits){emul.Bool(true)}, fmt.toEmul))
  }
  /** Returns the minimum integer value representable by this number's format. */
  def minInt: Fix[S,I,F] = uconst(fmt.toEmul.MIN_INTEGRAL_VALUE_FP)

  /** Returns the maximum integer value representable by this number's format. */
  def maxInt: Fix[S,I,F] = uconst(fmt.toEmul.MAX_INTEGRAL_VALUE_FP)

  /** Returns the minimum value representable by this number's format. */
  def minValue: Fix[S,I,F] = uconst(fmt.toEmul.MIN_VALUE_FP)

  /** Returns the maximum value representable by this number's format. */
  def maxValue: Fix[S,I,F] = uconst(fmt.toEmul.MAX_VALUE_FP)

  @api override def toText: Text = stage(FixToText(this, None))
  @api def toText(format:Option[String]): Text = stage(FixToText(this, format))

  @rig def __toFix[S2:BOOL,I2:INT,F2:INT]: Fix[S2,I2,F2] = {
    stage(FixToFix[S,I,F,S2,I2,F2](this, FixFmt(BOOL[S2],INT[I2],INT[F2])))
  }
  @rig def __toFixSat[S2:BOOL,I2:INT,F2:INT]: Fix[S2,I2,F2] = {
    stage(FixToFixSat[S,I,F,S2,I2,F2](this, FixFmt(BOOL[S2],INT[I2],INT[F2])))
  }
  @rig def __toFixUnb[S2:BOOL,I2:INT,F2:INT]: Fix[S2,I2,F2] = {
    stage(FixToFixUnb[S,I,F,S2,I2,F2](this, FixFmt(BOOL[S2],INT[I2],INT[F2])))
  }
  @rig def __toFixUnbSat[S2:BOOL,I2:INT,F2:INT]: Fix[S2,I2,F2] = {
    stage(FixToFixUnbSat[S,I,F,S2,I2,F2](this, FixFmt(BOOL[S2],INT[I2],INT[F2])))
  }
  @rig def __toFlt[M:INT,E:INT]: Flt[M,E] = stage(FixToFlt[S,I,F,M,E](this, FltFmt(INT[M],INT[E])))
}

object Fix {
  def createFixType(s: Boolean, i: Int, f: Int) = {
    class BT extends BOOL[BT] {
      val v = s
    }
    class IT extends INT[IT] {
      val v = i
    }
    class FT extends INT[FT] {
      val v = f
    }

    implicit def BTEV: BOOL[BT] = new BT
    implicit def ITEV: INT[IT] = new IT
    implicit def FTEV: INT[FT] = new FT

    proto(new FixPt[BT, IT, FT])
  }
}
object I32 {
  def apply(c: Int): I32 = uconst[I32](FixedPoint.fromInt(c))
  @rig def p(c: Int): I32 = parameter[I32](FixedPoint.fromInt(c))
}

object FixPtType {
  def unapply(x: Type[_]): Option[(Boolean,Int,Int)] = x match {
    case t: Fix[_,_,_] => Some((t.sign,t.ibits,t.fbits))
    case _ => None
  }
}

