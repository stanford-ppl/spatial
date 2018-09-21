package fringe.templates.math

import chisel3._
import chisel3.internal.sourceinfo._

import fringe.templates.hardfloat._

class FloatingPoint(val m: Int, val e: Int) extends Bundle {

  def this(fmt: emul.FltFormat) = this(fmt.sbits, fmt.ebits)

  def fmt: emul.FltFormat = emul.FltFormat(m, e)

  def apply(msb: Int, lsb: Int): UInt = this.number(msb,lsb)
  def apply(bit: Int): Bool = this.number(bit)

  // Properties
  val number = UInt((m + e).W)

  def raw: UInt = number
  def r: UInt = number

  def msb():Bool = number(m+e-1)

  def cast(dst: FloatingPoint, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping, expect_neg: Bool = false.B, expect_pos: Bool = false.B): Unit = {
    throw new Exception("Float casting not implemented yet")
  }

  def raw_mantissa: UInt = this.number(m+e-1, e)
  def raw_exp: UInt = this.number(e, 0)

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

  def +(that: FloatingPoint): FloatingPoint = Math.add(this, that)
  def -(that: FloatingPoint): FloatingPoint = Math.sub(this, that)
  def *(that: FloatingPoint): FloatingPoint = Math.mul(this, that)
  def /(that: FloatingPoint): FloatingPoint = Math.div(this, that)
  def <(that: FloatingPoint): Bool = Math.lt(this, that)
  def >(that: FloatingPoint): Bool = Math.lt(that, this)
  def <=(that: FloatingPoint): Bool = Math.lte(this, that)
  def >=(that: FloatingPoint): Bool = Math.lte(that, this)

  def ===(that: FloatingPoint): Bool = Math.eql(this, that)
  def =/=(that: FloatingPoint): Bool = Math.neq(this, that)

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


  def toFixed(sign: Boolean, dec: Int, frac: Int): FixedPoint = Math.flt2fix(this, sign, dec, frac, Truncate, Wrapping)

  def toFloat(m_out: Int, e_out: Int): FloatingPoint = Math.flt2flt(this, m_out, e_out)

  override def cloneType = (new FloatingPoint(m,e)).asInstanceOf[this.type] // See chisel3 bug 358

}

object FloatingPoint {

  def apply(m: Int, e: Int, init: Float): FloatingPoint = FloatingPoint(m, e, init.toDouble)

  // TODO
  def apply(m: Int, e: Int, init: Double): FloatingPoint = {
    val cst = Wire(new FloatingPoint(m,e))
    cst
  }

  // TODO
  def apply(m: Int, e: Int, init: SInt): FloatingPoint = {
    val cst = Wire(new FloatingPoint(m,e))
    cst
  }

  def apply(m: Int, e: Int, init: UInt): FloatingPoint = {
    val cst = Wire(new FloatingPoint(m,e))
    cst
  }



  // TODO: Fix these
  def bits(num: Float): Int = java.lang.Float.floatToRawIntBits(num)
  def bits(num: Double): Long = java.lang.Double.doubleToRawLongBits(num)

}
