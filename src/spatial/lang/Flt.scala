package spatial.lang

import core._
import emul.{FloatPoint, FltFormat, FixedPoint}
import forge.tags._

import spatial.node._

class FltFmt[M,E](val m: INT[M], val e: INT[E]) {
  def sign: Boolean = true
  def mbits: Int = m.v
  def ebits: Int = e.v
  def toEmul: FltFormat = FltFormat(mbits-1,ebits)
}
object FltFmt {
  def apply[M:INT,E:INT]: FltFmt[M,E] = new FltFmt[M,E](INT[M],INT[E])
}

@ref class Flt[M:INT,E:INT] extends Top[Flt[M,E]] with Num[Flt[M,E]] with Ref[FloatPoint,Flt[M,E]] {
  override val box = implicitly[Flt[M,E] <:< Num[Flt[M,E]]]
  lazy val fmt = FltFmt[M,E]
  def mbits: Int = fmt.mbits
  def ebits: Int = fmt.ebits
  def nbits: Int = mbits + ebits

  @api def unary_-(): Flt[M,E] = stage(FltNeg(this))
  @api def +(that: Flt[M,E]): Flt[M,E] = stage(FltAdd(this,that))
  @api def -(that: Flt[M,E]): Flt[M,E] = stage(FltSub(this,that))
  @api def *(that: Flt[M,E]): Flt[M,E] = stage(FltMul(this,that))
  @api def /(that: Flt[M,E]): Flt[M,E] = stage(FltDiv(this,that))
  @api def %(that: Flt[M,E]): Flt[M,E] = stage(FltMod(this,that))

  @api def <(that: Flt[M,E]): Bit = stage(FltLst(this,that))
  @api def <=(that: Flt[M,E]): Bit = stage(FltLeq(this,that))
  @api override def neql(that: Flt[M,E]): Bit = that match {
    case that: Flt[_,_] if this.fmt == that.fmt => stage(FltNeq(that, this))
    case _ => super.!==(that)
  }
  @api override def eql(that: Flt[M,E]): Bit = that match {
    case that: Flt[_,_] if this.fmt == that.fmt => stage(FltEql(that,this))
    case _ => super.===(that)
  }

  // --- Typeclass Methods

  @rig def min(a: Flt[M,E], b: Flt[M,E]): Flt[M,E] = stage(FltMin(a,b))
  @rig def max(a: Flt[M,E], b: Flt[M,E]): Flt[M,E] = stage(FltMax(a,b))

  @rig def random(max: Option[Flt[M,E]]): Flt[M,E] = stage(FltRandom(max))

  @rig def abs(a: Flt[M,E]): Flt[M,E] = stage(FltAbs(a))
  @rig def ceil(a: Flt[M,E]): Flt[M,E] = stage(FltCeil(a))
  @rig def floor(a: Flt[M,E]): Flt[M,E] = stage(FltFloor(a))
  @rig def pow(b: Flt[M,E], e: Flt[M,E]): Flt[M,E] = stage(FltPow(b,e))
  @rig def exp(a: Flt[M,E]): Flt[M,E] = stage(FltExp(a))
  @rig def ln(a: Flt[M,E]): Flt[M,E] = stage(FltLn(a))
  @rig def sqrt(a: Flt[M,E]): Flt[M,E] = stage(FltSqrt(a))
  @rig def sin(a: Flt[M,E]): Flt[M,E] = stage(FltSin(a))
  @rig def cos(a: Flt[M,E]): Flt[M,E] = stage(FltCos(a))
  @rig def tan(a: Flt[M,E]): Flt[M,E] = stage(FltTan(a))
  @rig def sinh(a: Flt[M,E]): Flt[M,E] = stage(FltSinh(a))
  @rig def cosh(a: Flt[M,E]): Flt[M,E] = stage(FltCosh(a))
  @rig def tanh(a: Flt[M,E]): Flt[M,E] = stage(FltTanh(a))
  @rig def asin(a: Flt[M,E]): Flt[M,E] = stage(FltAsin(a))
  @rig def acos(a: Flt[M,E]): Flt[M,E] = stage(FltAcos(a))
  @rig def atan(a: Flt[M,E]): Flt[M,E] = stage(FltAtan(a))
  @rig def sigmoid(a: Flt[M,E]): Flt[M,E] = stage(FltSigmoid(a))

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

object FltPtType {
  def unapply(x: Type[_]): Option[(Int,Int)] = x match {
    case t: Flt[_,_] => Some((t.mbits,t.ebits))
    case _ => None
  }
}
object FloatType {
  def unapply(x: Type[_]): Boolean = x match {
    case FltPtType(24,8) => true
    case _ => false
  }
}
object DoubleType {
  def unapply(x: Type[_]): Boolean = x match {
    case FltPtType(53,11) => true
    case _ => false
  }
}
