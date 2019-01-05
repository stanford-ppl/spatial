package fringe.templates.math

import chisel3._
import chisel3.core.CompileOptions
import chisel3.internal.sourceinfo.SourceInfo

import fringe.utils.implicits._

import scala.math.BigInt

/** Fixed point numbers **/
class FixedPoint(val s: Boolean, val d: Int, val f: Int, val litVal: Option[BigInt] = None) extends Bundle {
  def this(fmt: emul.FixFormat) = this(fmt.sign, fmt.ibits, fmt.fbits)

  assert(d >= 0 && f >= 0, s"Cannot make FixedPoint($s,$d,$f)")
  lazy val fmt: emul.FixFormat = emul.FixFormat(s, d, f)

  def apply(msb: Int, lsb: Int): UInt = this.number(msb,lsb)
  def apply(bit: Int): Bool = this.number(bit)

  def toSeq: Seq[FixedPoint] = Seq(this)

  def uint: UInt = litVal.map(_.U((d + f).W)).getOrElse(r.asUInt)
  def sint: SInt = litVal.map(_.S((d + f).W)).getOrElse(r.asSInt)

  def upcastUInt(fmt: emul.FixFormat): UInt = {
    val result = Wire(new FixedPoint(fmt))
    if (litVal.isDefined) result.r := litVal.get.toInt.FP(fmt.sign, fmt.ibits + f, fmt.fbits - f).r
    else result.r := Math.fix2fix(this, fmt.sign, fmt.ibits, fmt.fbits, None, true.B, Truncate, Wrapping).r
    result.r
  }
  def upcastSInt(fmt: emul.FixFormat): SInt = {
    this.upcastUInt(fmt).asSInt
  }

  // Properties
  val number: UInt = UInt((d + f).W)
  // val debug_overflow: Bool = Bool()

  def raw: UInt = number
  def r: UInt = number
  def raw_dec: UInt = number(d+f-1, f)
  def rd: UInt = number(d+f-1, f)
  def raw_frac: UInt = number(f, 0)
  def rf: UInt = number(f, 0)

  // Conversions
  def reverse: UInt = chisel3.util.Reverse(this.number)

  def msb: Bool = number(d+f-1)

  def cast(dest: FixedPoint): Unit = dest.r := Math.fix2fix(this, dest.s, dest.d, dest.f, None, true.B, Truncate, Wrapping)

  // Arithmetic
  override def connect(rawop: Data)(implicit ctx: SourceInfo, opts: CompileOptions): Unit = rawop match {
    case op: FixedPoint => number := op.number
    case op: UInt       => number := op
  }

  /** Fixed point addition with standard truncation and overflow. */
  def +(that: FixedPoint): FixedPoint = Math.add(this, that, None, true.B, round = Truncate, overflow = Wrapping)
  def +(that: UInt): FixedPoint = this + that.trueFP(fmt)
  def +(that: SInt): FixedPoint = this + that.trueFP(fmt)

  /** Fixed point addition with standard truncation and saturation on overflow. */
  def <+>(that: FixedPoint): FixedPoint = Math.add(this, that, None, true.B, round = Truncate, overflow = Saturating)

  /** Fixed point subtraction with standard truncation and overflow. */
  def -(that: FixedPoint): FixedPoint = Math.sub(this, that, None, true.B, round = Truncate, overflow = Wrapping)
  def -(that: UInt): FixedPoint = this - that.trueFP(fmt)
  def -(that: SInt): FixedPoint = this - that.trueFP(fmt)

  /** Fixed point subtraction with standard truncation and saturation on overflow. */
  def <->(that: FixedPoint): FixedPoint = Math.sub(this, that, None, true.B, round = Truncate, overflow = Saturating)

  /** Fixed point multiplication with standard truncation and overflow. */
  def *(that: FixedPoint): FixedPoint = Math.mul(this, that, delay = None, flow = true.B, round = Truncate, overflow = Wrapping)
  def *(that: UInt): FixedPoint = this * that.trueFP(fmt)
  def *(that: SInt): FixedPoint = this * that.trueFP(fmt)

  def mul(that: FixedPoint, delay: Option[Double], flow: Bool, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping): FixedPoint = Math.mul(this, that, delay, flow, rounding, saturating)

  /** Fixed point division with standard truncation and overflow. */
  def /(that: FixedPoint): FixedPoint = Math.div(this, that, delay = None, flow = true.B, round = Truncate, overflow = Wrapping)
  def /(that: UInt): FixedPoint = this / that.trueFP(fmt)
  def /(that: SInt): FixedPoint = this / that.trueFP(fmt)

  def div(that: FixedPoint, delay: Option[Double], flow: Bool, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping): FixedPoint = Math.div(this, that, delay, flow, rounding, saturating)

  /** Fixed point modulus with standard truncation and overflow. */
  def %(that: FixedPoint): FixedPoint = Math.mod(this, that, delay = None, flow = true.B, round = Truncate, overflow = Wrapping)
  def %(that: UInt): FixedPoint = this % that.trueFP(fmt)
  def %(that: SInt): FixedPoint = this % that.trueFP(fmt)

  def mod(that: FixedPoint, delay: Option[Double], flow: Bool): FixedPoint = Math.mod(this, that, delay, flow, round = Truncate, overflow = Wrapping)

  /** Fixed point floor (round towards negative infinity). */
  def floor: FixedPoint = Math.floor(this)

  /** Fixed point ceiling (round towards positive infinity). */
  def ceil: FixedPoint = Math.ceil(this)

  def >>(shift: Int): FixedPoint = Math.arith_right_shift(this, shift, None, true.B)
  def <<(shift: Int): FixedPoint = Math.arith_left_shift(this, shift, None, true.B)
  def >>>(shift: Int): FixedPoint = Math.logic_right_shift(this, shift, None, true.B)

  def <(that: FixedPoint): Bool = Math.lt(this, that, None, true.B)
  def <(that: UInt): Bool = this < that.trueFP(fmt)
  def <(that: SInt): Bool = this < that.trueFP(fmt)

  def <=(that: FixedPoint): Bool = Math.lte(this, that, None, true.B)
  def <=(that: UInt): Bool = this <= that.trueFP(fmt)
  def <=(that: SInt): Bool = this <= that.trueFP(fmt)

  def >(that: FixedPoint): Bool = Math.lt(that, this, None, true.B)
  def >(that: UInt): Bool = this > that.trueFP(fmt)
  def >(that: SInt): Bool = this > that.trueFP(fmt)

  def >=(that: FixedPoint): Bool = Math.lte(that, this, None, true.B)
  def >=(that: UInt): Bool = this >= that.trueFP(fmt)
  def >=(that: SInt): Bool = this >= that.trueFP(fmt)

  def ^(that: FixedPoint): FixedPoint = Math.xor(this, that, None, true.B)
  def ^(that: UInt): FixedPoint = this ^ that.trueFP(fmt)
  def ^(that: SInt): FixedPoint = this ^ that.trueFP(fmt)

  def &(that: FixedPoint): FixedPoint = Math.and(this, that, None, true.B)
  def &(that: UInt): FixedPoint = this & that.trueFP(fmt)
  def &(that: SInt): FixedPoint = this & that.trueFP(fmt)

  def |(that: FixedPoint): FixedPoint = Math.or(this, that, None, true.B)
  def |(that: UInt): FixedPoint = this | that.trueFP(fmt)
  def |(that: SInt): FixedPoint = this | that.trueFP(fmt)

  def ===(that: FixedPoint): Bool = Math.eql(this, that, None, true.B)
  def ===(that: UInt): Bool = this === that.trueFP(fmt)
  def ===(that: SInt): Bool = this === that.trueFP(fmt)

  def =/=(that: FixedPoint): Bool = Math.neq(this, that, None, true.B)
  def =/=(that: UInt): Bool = this =/= that.trueFP(fmt)
  def =/=(that: SInt): Bool = this =/= that.trueFP(fmt)

  def isNeg: Bool = Mux(s.B && number(f+d-1), true.B, false.B)

  def unary_-(): FixedPoint = {
    val neg = Wire(new FixedPoint(s,d,f))
    neg.r := ~number + 1.U
    neg
  }
  def unary_~(): FixedPoint = {
    val neg = Wire(new FixedPoint(s,d,f))
    neg.r := ~number
    neg
  }

  def toFixed(fmt: emul.FixFormat): FixedPoint = Math.fix2fix(this, fmt.sign, fmt.ibits, fmt.fbits, None, true.B, Truncate, Wrapping)
  def toFixed(num: FixedPoint): FixedPoint = Math.fix2fix(this, num.s, num.d, num.f, None, true.B, Truncate, Wrapping)
  def toFloat(fmt: emul.FltFormat): FloatingPoint = Math.fix2flt(this, fmt.sbits, fmt.ebits, None, true.B)

  override def cloneType = (new FixedPoint(s,d,f,litVal)).asInstanceOf[this.type] // See chisel3 bug 358
}

object FixedPoint {

  /** Creates a FixedPoint wire from the given Bool wire. */
  def apply(s: Boolean, d: Int, f: Int, init: Bool): FixedPoint = {
    val cst = Wire(new FixedPoint(s, d, f, init.litOption))
    cst.r := init
    cst
  }

  /** Creates a FixedPoint wire from the given UInt wire. */
  def apply(s: Boolean, d: Int, f: Int, init: UInt, sign_extend: Boolean = true): FixedPoint = {
    val cst = Wire(new FixedPoint(s, d, f, init.litOption))
    val tmp = Wire(new FixedPoint(s, init.getWidth, 0))
    tmp.r := init
    cst.r := Math.fix2fix(tmp, s, d, f, None, true.B, Truncate, Wrapping).r
    cst
  }

  /** Creates a FixedPoint wire from the given SInt wire. */
  def apply(s: Boolean, d: Int, f: Int, value: SInt): FixedPoint = FixedPoint(s, d, f, value.asUInt)

  /** Creates a FixedPoint wire with the given Int literal value. */
  def apply(s: Boolean, d: Int, f: Int, value: Int): FixedPoint = {
    FixedPoint(s,d,f, BigInt(value) << f)
  }

  /** Creates a FixedPoint wire with the given Double literal value. */
  def apply(s: Boolean, d: Int, f: Int, value: Double): FixedPoint = {
    FixedPoint(s, d, f, (BigDecimal(value) * BigDecimal(2).pow(f)).toBigInt())
  }

  /** Creates a FixedPoint wire with the given BigInt literal value. */
  def apply(s: Boolean, d: Int, f: Int, value: BigInt): FixedPoint = {
    val cst = Wire(new FixedPoint(s, d, f, Some(value)))
    cst.raw := value.S((d + f + 1).W).apply(d+f-1,0).asUInt()
    cst
  }
}