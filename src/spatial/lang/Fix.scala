package spatial.lang

import core._
import emul.{FixedPoint, FloatPoint, FixFormat}
import forge.tags._
import spatial.node._

class FixFmt[S,I,F](val s: BOOL[S], val i: INT[I], val f: INT[F]) {
  def sign: Boolean = s.v
  def ibits: Int = i.v
  def fbits: Int = f.v
  def toEmul: FixFormat = FixFormat(sign,ibits,fbits)
}
object FixFmt {
  def apply[S:BOOL,I:INT,F:INT]: FixFmt[S,I,F] = new FixFmt[S,I,F](BOOL[S],INT[I],INT[F])
}

@ref class Fix[S:BOOL,I:INT,F:INT] extends Top[Fix[S,I,F]] with Num[Fix[S,I,F]] with Ref[FixedPoint,Fix[S,I,F]] {
  val box = implicitly[Fix[S,I,F] <:< Num[Fix[S,I,F]]]

  val fmt = FixFmt[S,I,F]
  def sign: Boolean = fmt.sign
  def ibits: Int = fmt.ibits
  def fbits: Int = fmt.fbits
  def nbits: Int = ibits + fbits

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
  @api def <<(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixSLA(this,that))
  @api def >>(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixSRA(this,that))
  @api def >>>(that: Fix[S,I,F]): Fix[S,I,F] = stage(FixSRU(this,that))

  @api def <(that: Fix[S,I,F]): Bit = stage(FixLst(this,that))
  @api def <=(that: Fix[S,I,F]): Bit = stage(FixLeq(this,that))

  @api def ::(start: Fix[S,I,F]): Series[Fix[S,I,F]]  = Series[Fix[S,I,F]](start, this, 1, 1, isUnit = false)
  @api def par(p: I32): Series[Fix[S,I,F]]            = Series[Fix[S,I,F]](zero, this, one, p, isUnit = false)
  @api def by(step: Fix[S,I,F]): Series[Fix[S,I,F]]   = Series[Fix[S,I,F]](one, this, step, 1, isUnit = false)
  @api def until(end: Fix[S,I,F]): Series[Fix[S,I,F]] = Series[Fix[S,I,F]](this, end, one, 1, isUnit = false)
  //@api def to(end: Fix[S,I,F]): Series[Fix[S,I,F]]    = Series[Fix[S,I,F]](this, end+1, one, 1, isUnit=false)

  @api override def neql(that: Fix[S,I,F]): Bit = stage(FixNeq(this,that))
  @api override def eql(that: Fix[S,I,F]): Bit = stage(FixEql(this,that))

  // --- Typeclass Methods
  @rig def random(max: Option[Fix[S,I,F]]): Fix[S,I,F] = stage(FixRandom(max))

  @rig def min(a: Fix[S,I,F], b: Fix[S,I,F]): Fix[S,I,F] = stage(FixMin(a,b))
  @rig def max(a: Fix[S,I,F], b: Fix[S,I,F]): Fix[S,I,F] = stage(FixMax(a,b))

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
  def apply(c: Int): I32 = uconst[I32](FixedPoint.fromInt(c))
  @rig def p(c: Int): I32 = param[I32](FixedPoint.fromInt(c))
}

object FixPtType {
  def unapply(x: Type[_]): Option[(Boolean,Int,Int)] = x match {
    case t: Fix[_,_,_] => Some((t.sign,t.ibits,t.fbits))
    case _ => None
  }
}

