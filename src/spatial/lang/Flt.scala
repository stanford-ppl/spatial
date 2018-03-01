package spatial.lang

import core._
import emul.{FloatPoint, FltFormat, FixedPoint}
import forge.tags._

import spatial.node._

@ref class Flt[F:FltFmt] extends Top[Flt[F]] with Num[Flt[F]] with Ref[FloatPoint,Flt[F]] {
  override val box: Flt[F] <:< Num[Flt[F]] = implicitly[Flt[F] <:< Num[Flt[F]]]

  @api def unary_-(): Flt[F] = stage(FltNeg(this))
  @api def +(that: Flt[F]): Flt[F] = stage(FltAdd(this,that))
  @api def -(that: Flt[F]): Flt[F] = stage(FltSub(this,that))
  @api def *(that: Flt[F]): Flt[F] = stage(FltMul(this,that))
  @api def /(that: Flt[F]): Flt[F] = stage(FltDiv(this,that))
  @api def %(that: Flt[F]): Flt[F] = stage(FltMod(this,that))

  @api def <(that: Flt[F]): Bit = stage(FltLst(this,that))
  @api def <=(that: Flt[F]): Bit = stage(FltLeq(this,that))
  @api override def neql(that: Flt[F]): Bit = that match {
    case that: Flt[_] if this.fmt == that.fmt => stage(FltNeq(that, this))
    case _ => super.!==(that)
  }
  @api override def eql(that: Flt[F]): Bit = that match {
    case that: Flt[_] if this.fmt == that.fmt => stage(FltEql(that,this))
    case _ => super.===(that)
  }

  // --- Typeclass Methods
  val fmt: FltFmt[F] = FltFmt[F]
  def mbits: Int = fmt.mbits
  def ebits: Int = fmt.ebits
  def nbits: Int = mbits + ebits

  @rig def min(a: Flt[F], b: Flt[F]): Flt[F] = stage(FltMin(a,b))
  @rig def max(a: Flt[F], b: Flt[F]): Flt[F] = stage(FltMax(a,b))

  @rig def random(max: Option[Flt[F]]): Flt[F] = stage(FltRandom(max))

  @rig def abs(a: Flt[F]): Flt[F] = stage(FltAbs(a))
  @rig def ceil(a: Flt[F]): Flt[F] = stage(FltCeil(a))
  @rig def floor(a: Flt[F]): Flt[F] = stage(FltFloor(a))
  @rig def pow(b: Flt[F], e: Flt[F]): Flt[F] = stage(FltPow(b,e))
  @rig def exp(a: Flt[F]): Flt[F] = stage(FltExp(a))
  @rig def ln(a: Flt[F]): Flt[F] = stage(FltLn(a))
  @rig def sqrt(a: Flt[F]): Flt[F] = stage(FltSqrt(a))
  @rig def sin(a: Flt[F]): Flt[F] = stage(FltSin(a))
  @rig def cos(a: Flt[F]): Flt[F] = stage(FltCos(a))
  @rig def tan(a: Flt[F]): Flt[F] = stage(FltTan(a))
  @rig def sinh(a: Flt[F]): Flt[F] = stage(FltSinh(a))
  @rig def cosh(a: Flt[F]): Flt[F] = stage(FltCosh(a))
  @rig def tanh(a: Flt[F]): Flt[F] = stage(FltTanh(a))
  @rig def asin(a: Flt[F]): Flt[F] = stage(FltAsin(a))
  @rig def acos(a: Flt[F]): Flt[F] = stage(FltAcos(a))
  @rig def atan(a: Flt[F]): Flt[F] = stage(FltAtan(a))
  @rig def sigmoid(a: Flt[F]): Flt[F] = stage(FltSigmoid(a))

  lazy val efmt: FltFormat = fmt.toEmul
  @rig override def cnst(c: Any, checked: Boolean = true): Option[FloatPoint] = c match {
    case x: BigDecimal => withCheck(FloatPoint(x,efmt),checked){ _.toBigDecimal == x }
    case x: BigInt     => withCheck(FloatPoint(x,efmt),checked){ _.toBigDecimal == BigDecimal(x) }
    case x: Boolean    => Some(FloatPoint(x,efmt))
    case x: Char       => withCheck(FloatPoint(x,efmt),checked){ _.toInt == x }
    case x: Byte       => withCheck(FloatPoint(x,efmt),checked){ _.toByte == x }
    case x: Short      => withCheck(FloatPoint(x,efmt),checked){ _.toShort == x }
    case x: Int        => withCheck(FloatPoint(x,efmt),checked){ _.toInt == x }
    case x: Long       => withCheck(FloatPoint(x,efmt),checked){ _.toLong == x }
    case x: Float      => withCheck(FloatPoint(x,efmt),checked){ _.toFloat == x }
    case x: Double     => withCheck(FloatPoint(x,efmt),checked){ _.toDouble == x }
    case x: String     => withCheck(FloatPoint(x,efmt),checked){ _.toBigDecimal == BigDecimal(x) }
    case x: FixedPoint => withCheck(x.toFloatPoint(efmt),checked){ _.toFixedPoint(x.fmt) == x }
    case x: FloatPoint if x.fmt == efmt => Some(x)
    case x: FloatPoint => withCheck(x.toFloatPoint(efmt),checked){ _.toFloatPoint(x.fmt) == x }
    case _ => None
  }
}

object Flt {

}
