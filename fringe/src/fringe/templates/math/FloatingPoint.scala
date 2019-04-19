package fringe.templates.math

import chisel3._
import chisel3.internal.sourceinfo._

import fringe.templates.hardfloat._

class FloatingPoint(val m: Int, val e: Int) extends Bundle {

  def this(fmt: emul.FltFormat) = this(fmt.sbits+1, fmt.ebits)

  def fmt: emul.FltFormat = emul.FltFormat(m-1, e)

  def apply(msb: Int, lsb: Int): UInt = this.number(msb,lsb)
  def apply(bit: Int): Bool = this.number(bit)

  // Properties
  val number = UInt((m + e).W)

  def raw: UInt = number
  def r: UInt = number

  def msb():Bool = number(m+e-1)

  def cast(dst: FloatingPoint, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping, expect_neg: Bool = false.B, expect_pos: Bool = false.B, myName: String = ""): Unit = {
    throw new Exception("Float casting not implemented yet")
  }

  def raw_mantissa: UInt = this.number(m, 0)
  def raw_exp: UInt = this.number(m+e-1, m)

  // Arithmetic
  override def connect(rawop: Data)(implicit sourceInfo: SourceInfo, connectionCompileOptions: chisel3.core.CompileOptions): Unit = rawop match {
    case op: FixedPoint    => number := op.number
    case op: UInt          => number := op
    case op: FloatingPoint => number := op.number
  }

  def unary_-(): FloatingPoint = {
    val result = Wire(new FloatingPoint(m, e))
    result.r := util.Cat(~this.msb, this.r.apply(m+e-2,0))
    result
  }

  def +(that: FloatingPoint): FloatingPoint = Math.fadd(this, that, None, true.B, "")
  def -(that: FloatingPoint): FloatingPoint = Math.fsub(this, that, None, true.B, "")
  def *(that: FloatingPoint): FloatingPoint = Math.fmul(this, that, None, true.B, "")
  def /(that: FloatingPoint): FloatingPoint = Math.fdiv(this, that, None, true.B, "")
  def <(that: FloatingPoint): Bool = Math.flt(this, that, None, true.B, "")
  def >(that: FloatingPoint): Bool = Math.flt(that, this, None, true.B, "")
  def <=(that: FloatingPoint): Bool = Math.flte(this, that, None, true.B, "")
  def >=(that: FloatingPoint): Bool = Math.flte(that, this, None, true.B, "")

  def ===(that: FloatingPoint): Bool = Math.feql(this, that, None, true.B, "")
  def =/=(that: FloatingPoint): Bool = Math.fneq(this, that, None, true.B, "")

  def ^(that: FloatingPoint): FloatingPoint = {
    assert(this.fmt == that.fmt)
    val result = Wire(new FloatingPoint(m, e))
    result.r := this.r ^ that.r
    result
  }

  def &(that: FloatingPoint): FloatingPoint = {
    assert(this.fmt == that.fmt)
    val result = Wire(new FloatingPoint(m, e))
    result.r := this.r & that.r
    result
  }

  def |(that: FloatingPoint): FloatingPoint = {
    assert(this.fmt == that.fmt)
    val result = Wire(new FloatingPoint(m, e))
    result.r := this.r | that.r
    result
  }


  def toFixed(sign: Boolean, dec: Int, frac: Int, myName: String): FixedPoint = Math.flt2fix(this, sign, dec, frac, None, true.B, Truncate, Wrapping, myName)

  def toFloat(m_out: Int, e_out: Int, myName: String): FloatingPoint = Math.flt2flt(this, m_out, e_out, None, true.B, myName)

  override def cloneType = (new FloatingPoint(m,e)).asInstanceOf[this.type] // See chisel3 bug 358

}

object FloatingPoint {

  def apply(m: Int, e: Int, init: Float): FloatingPoint = FloatingPoint(m, e, init.toDouble)

  def apply(m: Int, e: Int, init: Double): FloatingPoint = {
    val cst = Wire(new FloatingPoint(m,e))
    cst.r := FloatingPoint.bits(init.toFloat, cst.fmt).S((m+e+1).W).asUInt.apply(m+e-1,0)
    cst
  }

  def apply(m: Int, e: Int, init: SInt): FloatingPoint = {
    val cst = Wire(new FloatingPoint(m,e))
    val tmp = Wire(new FixedPoint(true, init.getWidth, 0))
    tmp.r := init.asUInt
    cst.r := Math.fix2flt(tmp,m,e,None,true.B, "")
    cst
  }

  def apply(m: Int, e: Int, init: UInt): FloatingPoint = {
    val cst = Wire(new FloatingPoint(m,e))
    val tmp = Wire(new FixedPoint(false, init.getWidth, 0))
    tmp.r := init
    cst.r := Math.fix2flt(tmp,m,e,None,true.B, "")
    cst
  }

  def bits(num: Float, fmt: emul.FltFormat): Int = {
    val emflt = new emul.FloatPoint(emul.FloatValue(num), true, fmt)
    emflt.bits.map(_.toBoolean).zipWithIndex.map{case (b: Boolean, i: Int) => {if (b) 1.toInt else 0.toInt} << i}.sum
  }
  def bits(num: Double, fmt: emul.FltFormat): Long = {
    val emflt = new emul.FloatPoint(emul.FloatValue(num), true, fmt)
    emflt.bits.map(_.toBoolean).zipWithIndex.map{case (b: Boolean, i: Int) => {if (b) 1.toLong else 0.toLong} << i}.sum
  }
  def bits(num: Float): Int = bits(num, new emul.FltFormat(23,8))
  def bits(num: Double): Long = bits(num, new emul.FltFormat(52,11))

}
