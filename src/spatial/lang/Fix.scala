package spatial.lang

import core._
import emul.{FixedPoint, FloatPoint, FixFormat}
import forge.tags._
import spatial.node._

@ref class Fix[F:FixFmt] extends Top[Fix[F]] with Num[Fix[F]] with Ref[FixedPoint,Fix[F]] {
  override val box: Fix[F] <:< Num[Fix[F]] = implicitly[Fix[F] <:< Num[Fix[F]]]

  lazy val fmt: FixFmt[F] = FixFmt[F]
  def sign: Boolean = fmt.isSigned
  def ibits: Int = fmt.ibits
  def fbits: Int = fmt.fbits
  def nbits: Int = ibits + fbits

  // --- Infix Methods
  @api def unary_-(): Fix[F] = stage(FixNeg(this))
  @api def unary_~(): Fix[F] = stage(FixInv(this))
  @api def &(that: Fix[F]): Fix[F] = stage(FixAnd(this,that))
  @api def |(that: Fix[F]): Fix[F] = stage(FixOr(this,that))
  @api def ^(that: Fix[F]): Fix[F] = stage(FixXor(this,that))

  @api def +(that: Fix[F]): Fix[F] = stage(FixAdd(this,that))
  @api def -(that: Fix[F]): Fix[F] = stage(FixSub(this,that))
  @api def *(that: Fix[F]): Fix[F] = stage(FixMul(this,that))
  @api def /(that: Fix[F]): Fix[F] = stage(FixDiv(this,that))
  @api def %(that: Fix[F]): Fix[F] = stage(FixMod(this,that))
  @api def <<(that: Fix[F]): Fix[F] = stage(FixSLA(this,that))
  @api def >>(that: Fix[F]): Fix[F] = stage(FixSRA(this,that))
  @api def >>>(that: Fix[F]): Fix[F] = stage(FixSRU(this,that))

  @api def <(that: Fix[F]): Bit = stage(FixLst(this,that))
  @api def <=(that: Fix[F]): Bit = stage(FixLeq(this,that))

  @api def ::(start: Fix[F]): Series[Fix[F]]  = Series[Fix[F]](start, this, 1, 1, isUnit = false)
  @api def par(p: I32): Series[Fix[F]]        = Series[Fix[F]](zero, this, one, p, isUnit = false)
  @api def by(step: Fix[F]): Series[Fix[F]]   = Series[Fix[F]](one, this, step, 1, isUnit = false)
  @api def until(end: Fix[F]): Series[Fix[F]] = Series[Fix[F]](this, end, one, 1, isUnit = false)

  @api override def neql(that: Fix[F]): Bit = stage(FixNeq(this,that))
  @api override def eql(that: Fix[F]): Bit = stage(FixEql(this,that))

  // --- Typeclass Methods
  @rig def random(max: Option[Fix[F]]): Fix[F] = stage(FixRandom(max))

  @rig def min(a: Fix[F], b: Fix[F]): Fix[F] = stage(FixMin(a,b))
  @rig def max(a: Fix[F], b: Fix[F]): Fix[F] = stage(FixMax(a,b))

  @rig def abs(a: Fix[F]): Fix[F] = stage(FixAbs(a))
  @rig def ceil(a: Fix[F]): Fix[F] = stage(FixCeil(a))
  @rig def floor(a: Fix[F]): Fix[F] = stage(FixFloor(a))
  @rig def pow(b: Fix[F], e: Fix[F]): Fix[F] = stage(FixPow(b,e))
  @rig def exp(a: Fix[F]): Fix[F] = stage(FixExp(a))
  @rig def ln(a: Fix[F]): Fix[F] = stage(FixLn(a))
  @rig def sqrt(a: Fix[F]): Fix[F] = stage(FixSqrt(a))
  @rig def sin(a: Fix[F]): Fix[F] = stage(FixSin(a))
  @rig def cos(a: Fix[F]): Fix[F] = stage(FixCos(a))
  @rig def tan(a: Fix[F]): Fix[F] = stage(FixTan(a))
  @rig def sinh(a: Fix[F]): Fix[F] = stage(FixSinh(a))
  @rig def cosh(a: Fix[F]): Fix[F] = stage(FixCosh(a))
  @rig def tanh(a: Fix[F]): Fix[F] = stage(FixTanh(a))
  @rig def asin(a: Fix[F]): Fix[F] = stage(FixAsin(a))
  @rig def acos(a: Fix[F]): Fix[F] = stage(FixAcos(a))
  @rig def atan(a: Fix[F]): Fix[F] = stage(FixAtan(a))
  @rig def sigmoid(a: Fix[F]): Fix[F] = stage(FixSigmoid(a))

  lazy val efmt: FixFormat = fmt.toEmul
  @rig override def cnst(c: Any, checked: Boolean = true): Option[FixedPoint] = c match {
    case x: BigDecimal => withCheck(FixedPoint(x,efmt),checked){ _.toBigDecimal == x }
    case x: BigInt     => withCheck(FixedPoint(x,efmt),checked){ _.toBigInt == x }
    case x: Boolean    => Some(FixedPoint(x,efmt))
    case x: Char       => withCheck(FixedPoint(x,efmt),checked){ _.toInt == x }
    case x: Byte       => withCheck(FixedPoint(x,efmt),checked){ _.toByte == x }
    case x: Short      => withCheck(FixedPoint(x,efmt),checked){ _.toShort == x }
    case x: Int        => withCheck(FixedPoint(x,efmt),checked){ _.toInt == x }
    case x: Long       => withCheck(FixedPoint(x,efmt),checked){ _.toLong == x }
    case x: Float      => withCheck(FixedPoint(x,efmt),checked){ _.toFloat == x }
    case x: Double     => withCheck(FixedPoint(x,efmt),checked){ _.toDouble == x }
    case x: String     => withCheck(FixedPoint(x,efmt),checked){ _.toBigDecimal == BigDecimal(x) }
    case x: FixedPoint if x.fmt == efmt => Some(x)
    case x: FixedPoint => withCheck(x.toFixedPoint(efmt),checked){ _.toFixedPoint(x.fmt) == x }
    case x: FloatPoint => withCheck(x.toFixedPoint(efmt),checked){ _.toFloatPoint(x.fmt) == x }
    case _ => None
  }
}

object Fix {

}

object I32 {
  def c(x: Int): I32 = const[I32](FixedPoint.fromInt(x))
  @api def p(x: Int): I32 = Type[I32].from(x, isParam = true)
}
