package fringe.templates.math

import chisel3._
import fringe.globals
import fringe.utils.getRetimed
import fringe.utils.implicits._
import scala.math.BigInt

/** Math operations API.
  * This object should NOT define any implementations - it should rely on BigIP classes for this.
  * The purpose of this is to allow hardware targets to customize implementation of these operations under a common API.
  */
object Math {

  // --- UInt Operations --- //
  def floor(a: UInt): UInt = a
  def ceil(a: UInt): UInt = a

  def mul(a: UInt, b: UInt, delay: Option[Double], flow: Bool): UInt = {
    if (globals.retime) {
      val latency = delay.getOrElse(globals.target.fixmul_latency * b.getWidth).toInt
      globals.bigIP.multiply(a, b, latency, flow)
    }
    else a * b
  }

  def mul(a: SInt, b: SInt, delay: Option[Double], flow: Bool): SInt = {
    if (globals.retime) {
      val latency = delay.getOrElse(globals.target.fixmul_latency * b.getWidth).toInt
      globals.bigIP.multiply(a, b, latency, flow)
    }
    else a * b
  }

  def div(a: SInt, b: SInt, delay: Option[Double], flow: Bool): SInt = {
    if (globals.retime) {
      val latency = delay.getOrElse(globals.target.fixdiv_latency * b.getWidth).toInt
      globals.bigIP.divide(a, b, latency, flow)
    }
    else globals.target match {
      case _:fringe.targets.zynq.Zynq  => globals.bigIP.divide(a, b, (globals.target.fixdiv_latency * b.getWidth).toInt, flow)
      case _ => a / b
    }
  }

  def div(a: UInt, b: UInt, delay: Option[Double], flow: Bool): UInt = {
    if (globals.retime) {
      val latency = delay.getOrElse(globals.target.fixdiv_latency * b.getWidth).toInt
      globals.bigIP.divide(a, b, latency, flow)
    }
    else a / b
  }

  def mod(a: UInt, b: UInt, delay: Option[Double], flow: Bool): UInt = {
    if (globals.retime) {
      val latency = delay.getOrElse(globals.target.fixmod_latency * b.getWidth).toInt
      globals.bigIP.mod(a, b, latency, flow)
    }
    else a % b
  }

  def mod(a: SInt, b: SInt, delay: Option[Double], flow: Bool): SInt = {
    if (globals.retime) {
      val latency = delay.getOrElse(globals.target.fixmod_latency * b.getWidth).toInt
      globals.bigIP.mod(a, b, latency, flow)
    }
    else a % b
  }

  def singleCycleDivide(num: SInt, den: SInt): SInt = num / den
  def singleCycleModulo(num: SInt, den: SInt): SInt = num % den
  def singleCycleDivide(num: UInt, den: UInt): UInt = num / den
  def singleCycleModulo(num: UInt, den: UInt): UInt = num % den

  // --- Fixed Point Operations --- //

  private def upcast(a: FixedPoint, b: FixedPoint): (FixedPoint, FixedPoint, FixedPoint, FixedPoint) = {
    // Compute upcasted type and return type
    val return_type = a.fmt combine b.fmt
    val upcast_type = return_type.copy(ibits = return_type.ibits + 1)

    // Get upcasted operators
    val result_upcast = Wire(new FixedPoint(upcast_type))
    val result        = Wire(new FixedPoint(return_type))
    val a_upcast   = a.toFixed(upcast_type)
    val b_upcast   = b.toFixed(upcast_type)

    (a_upcast, b_upcast, result_upcast, result)
  }

  // --- Fixed Point Operations --- //
  def floor(a: FixedPoint): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt))
    result.r := util.Cat(a.raw_dec, 0.U(a.f.W))
    result
  }
  def ceil(a: FixedPoint): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt))
    val stay = a.raw_frac === 0.U
    result.r := Mux(stay, a.r, util.Cat(a.raw_dec + 1.U, 0.U(a.f.W)))
    result
  }


  def add(a: FixedPoint, b: FixedPoint, round: RoundingMode, overflow: OverflowMode): FixedPoint = {
    // Allocate upcasted and result wires
    val (a_upcast, b_upcast, result_upcast, result) = upcast(a, b)

    // Instantiate an unsigned addition
    result_upcast.r := a_upcast.r + b_upcast.r

    // Downcast to result
    val expect_neg = if (a.s | b.s) a_upcast.msb & b_upcast.msb   else false.B
    val expect_pos = if (a.s | b.s) !a_upcast.msb & !b_upcast.msb else true.B
    val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow))
    fix2fixBox.io.a := result_upcast.r
    fix2fixBox.io.expect_neg := expect_neg
    fix2fixBox.io.expect_pos := expect_pos
    result.r := fix2fixBox.io.b
    result
  }

  def sub(a: FixedPoint, b: FixedPoint, round: RoundingMode, overflow: OverflowMode): FixedPoint = {
    val (a_upcast, b_upcast, result_upcast, result) = upcast(a, b)

    // Instantiate an unsigned subtraction
    result_upcast.r := a_upcast.r - b_upcast.r

    // Downcast to result
    val expect_neg = if (a.s | b.s) a_upcast.msb & !b_upcast.msb else true.B
    val expect_pos = if (a.s | b.s) !a_upcast.msb & b_upcast.msb else false.B
    val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow))
    fix2fixBox.io.a := result_upcast.r
    fix2fixBox.io.expect_neg := expect_neg
    fix2fixBox.io.expect_pos := expect_pos
    result.r := fix2fixBox.io.b
    result
  }

  def mul(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, round: RoundingMode, overflow: OverflowMode): FixedPoint = {
    val latency = if (globals.retime || delay.isDefined) delay.getOrElse(globals.target.fixmul_latency * a.getWidth)
                  else 0.0
    val intMode = round == Truncate && overflow == Wrapping && (a.f == 0 | b.f == 0)

    // Compute upcasted type and return type
    val return_type = a.fmt combine b.fmt
    val upcast_type = if (intMode) return_type else return_type.copy(ibits = a.d + b.d, fbits = a.f + b.f)

    // Get upcasted operators
    val result_upcast = Wire(new FixedPoint(upcast_type))

    // Do upcasted operation
    if (intMode) {
      val rhs_bits = a.f - b.f
      val a_upcast: UInt = if (rhs_bits > 0) util.Cat(a.r, util.Fill(rhs_bits, false.B))
                           else a.r
      val b_upcast: UInt = if (rhs_bits < 0 && b.litVal.isEmpty) util.Cat(b.r, util.Fill(-rhs_bits, false.B))
                           else if (b.litVal.isDefined) b.litVal.get.U(a.getWidth.W)
                           else b.r
      result_upcast.r := mul(a_upcast, b_upcast, Some(latency), flow) >> scala.math.max(a.f, b.f)
    }
    else {
      val a_upcast: UInt = util.Cat(util.Fill(b.d+b.f, a.msb), a.r)
      val b_upcast: UInt = if (b.litVal.isDefined) b.litVal.get.U((a.d+a.f+b.d+b.f).W)
                           else util.Cat(util.Fill(a.d+a.f, b.msb), b.r)

      result_upcast.r := mul(a_upcast, b_upcast, Some(latency), flow)
    }

    // Downcast to result
    val result = Wire(new FixedPoint(return_type))
    val expect_neg = if (a.s | b.s) getRetimed(a.msb ^ b.msb, latency.toInt) else false.B
    val expect_pos = if (a.s | b.s) getRetimed(!(a.msb ^ b.msb), latency.toInt) else true.B
    val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow))
    fix2fixBox.io.a := result_upcast.r
    fix2fixBox.io.expect_neg := expect_neg
    fix2fixBox.io.expect_pos := expect_pos
    result.r := fix2fixBox.io.b
    result
  }

  def div(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, round: RoundingMode, overflow: OverflowMode): FixedPoint = {
    val latency = if (globals.retime || delay.isDefined) delay.getOrElse(globals.target.fixdiv_latency * a.getWidth)
                  else 0.0

    val return_type = a.fmt combine b.fmt

    if (a.f == 0 && b.f == 0) {
      if (a.s | b.s) Math.div(a.sint, b.sint, Some(latency), flow).FP(return_type)
      else           Math.div(a.uint, b.uint, Some(latency), flow).FP(return_type)
    }
    else {
      // Interpret numerator as this type
      val upcast_type = if (round == Truncate && overflow == Wrapping) return_type.copy(fbits = return_type.fbits + 1)
                        else return_type.copy(ibits = a.d + b.d, fbits = a.f + b.f + 1)
      // But upcast it to this type
      val op_upcast_type = if (round == Truncate && overflow == Wrapping) return_type.copy(fbits = a.f + b.f + 1)
                           else return_type.copy(ibits = a.d + b.d, fbits = a.f + b.f + 1)

      val result_upcast = Wire(new FixedPoint(upcast_type))

      // TODO: Why is this not upcasting the denominator?
      if (a.s | b.s) {
        val a_upcast = a.upcastSInt(op_upcast_type)
        val b_upcast = b.sint
        result_upcast.r := Math.div(a_upcast, b_upcast, Some(latency), flow).asUInt
      }
      else {
        val a_upcast = a.upcastUInt(op_upcast_type)
        val b_upcast = b.uint
        result_upcast.r := Math.div(a_upcast, b_upcast, Some(latency), flow)
      }
      Console.println(s"uptype ${upcast_type}, return type ${return_type}")
      val result = Wire(new FixedPoint(return_type))
      val expect_neg = if (a.s | b.s) getRetimed(a.msb ^ b.msb, latency.toInt) else false.B
      val expect_pos = if (a.s | b.s) getRetimed(!(a.msb ^ b.msb), latency.toInt) else true.B
      val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow))
      fix2fixBox.io.a := result_upcast.r
      fix2fixBox.io.expect_neg := expect_neg
      fix2fixBox.io.expect_pos := expect_pos
      result.r := fix2fixBox.io.b
      result
    }
  }

  // TODO: No upcasting actually occurs here?
  def mod(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, round: RoundingMode, overflow: OverflowMode): FixedPoint = {
    val return_type = a.fmt combine b.fmt
    val upcast_type = return_type.copy(ibits = a.d + b.d, fbits = a.f + b.f)

    val result_upcast = Wire(new FixedPoint(upcast_type))
    val result = Wire(new FixedPoint(return_type))
    // Downcast to result
    result_upcast.r := Math.mod(a.uint, b.uint, delay, flow)
    val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow))
    fix2fixBox.io.a := result_upcast.r
    fix2fixBox.io.expect_neg := false.B
    fix2fixBox.io.expect_pos := false.B
    result.r := fix2fixBox.io.b
    result
  }

  def xor(a: FixedPoint, b: FixedPoint): FixedPoint = {
    val upcast_type = a.fmt combine b.fmt
    val result = Wire(new FixedPoint(upcast_type))
    result.r := a.upcastUInt(upcast_type) ^ b.upcastUInt(upcast_type)
    result
  }

  def and(a: FixedPoint, b: FixedPoint): FixedPoint = {
    val upcast_type = a.fmt combine b.fmt
    val result = Wire(new FixedPoint(upcast_type))
    result.r := a.upcastUInt(upcast_type) & b.upcastUInt(upcast_type)
    result
  }

  def or(a: FixedPoint, b: FixedPoint): FixedPoint = {
    val upcast_type = a.fmt combine b.fmt
    val result = Wire(new FixedPoint(upcast_type))
    result.r := a.upcastUInt(upcast_type) | b.upcastUInt(upcast_type)
    result
  }

  def arith_right_shift(a: FixedPoint, shift: Int): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt))
    result.r := util.Cat(util.Fill(shift, a.msb), a(a.d+a.f-1, shift))
    result
  }

  def arith_left_shift(a: FixedPoint, shift: Int): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt))
    result.r := a.r << shift
    result
  }

  def logic_right_shift(a: FixedPoint, shift: Int): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt))
    result.r := a.r >> shift
    result
  }

  def lt(a: FixedPoint, b: FixedPoint): Bool = {
    val upcast_type = a.fmt combine b.fmt
    if (a.s | b.s) a.upcastSInt(upcast_type) < b.upcastSInt(upcast_type)
    else           a.upcastUInt(upcast_type) < b.upcastUInt(upcast_type)
  }

  def lte(a: FixedPoint, b: FixedPoint): Bool = {
    val upcast_type = a.fmt combine b.fmt
    if (a.s | b.s) a.upcastSInt(upcast_type) <= b.upcastSInt(upcast_type)
    else           a.upcastUInt(upcast_type) <= b.upcastUInt(upcast_type)
  }

  def eql(a: FixedPoint, b: FixedPoint): Bool = {
    val upcast_type = a.fmt combine b.fmt
    a.upcastUInt(upcast_type) === b.upcastUInt(upcast_type)
  }

  def neq(a: FixedPoint, b: FixedPoint): Bool = {
    val upcast_type = a.fmt combine b.fmt
    a.upcastUInt(upcast_type) =/= b.upcastUInt(upcast_type)
  }


  // --- Floating Point Operations --- //

  def add(a: FloatingPoint, b: FloatingPoint): FloatingPoint = {
    // TODO: Make this a property of the DeviceTarget?
    val latency = 13
    assert(a.fmt == b.fmt)
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fadd(a.r, b.r, a.m, a.e, latency)
    result
  }

  def sub(a: FloatingPoint, b: FloatingPoint): FloatingPoint = {
    assert(a.fmt == b.fmt)
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fsub(a.r, b.r, a.m, a.e)
    result
  }

  def mul(a: FloatingPoint, b: FloatingPoint): FloatingPoint = {
    assert(a.fmt == b.fmt)
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fmul(a.r, b.r, a.m, a.e)
    result
  }

  def div(a: FloatingPoint, b: FloatingPoint): FloatingPoint = {
    assert(a.fmt == b.fmt)
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fdiv(a.r, b.r, a.m, a.e)
    result
  }

  def lt(a: FloatingPoint, b: FloatingPoint): Bool = {
    assert(a.fmt == b.fmt)
    val result = Wire(new Bool)
    result := globals.bigIP.flt(a.r, b.r, a.m, a.e)
    result
  }

  def lte(a: FloatingPoint, b: FloatingPoint): Bool = {
    assert(a.fmt == b.fmt)
    val result = Wire(new Bool)
    result := globals.bigIP.fle(a.r, b.r, a.m, a.e)
    result
  }

  def eql(a: FloatingPoint, b: FloatingPoint): Bool = {
    assert(a.fmt == b.fmt)
    val result = Wire(new Bool)
    result := globals.bigIP.feq(a.r, b.r, a.m, a.e)
    result
  }

  def neq(a: FloatingPoint, b: FloatingPoint): Bool = {
    assert(a.fmt == b.fmt)
    val result = Wire(new Bool)
    result := globals.bigIP.fne(a.r, b.r, a.m, a.e)
    result
  }

  def abs(a: FloatingPoint): FloatingPoint = {
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fabs(a.r, a.m, a.e)
    result
  }

  def sqrt(a: FloatingPoint): FloatingPoint = {
    val result = Wire(new FloatingPoint(a.fmt))
    result := globals.bigIP.fsqrt(a.r, a.m, a.e)
    result
  }

  def exp(a: FloatingPoint): FloatingPoint = {
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fexp(a.r, a.m, a.e)
    result
  }

  def tanh(a: FloatingPoint): FloatingPoint = {
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.ftanh(a.r, a.m, a.e)
    result
  }

  def sigmoid(a: FloatingPoint): FloatingPoint = {
    val result = Wire(new FloatingPoint(a.fmt))
    result := globals.bigIP.fsigmoid(a.r, a.m, a.e)
    result
  }

  def ln(a: FloatingPoint): FloatingPoint = {
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fln(a.r, a.m, a.e)
    result
  }
  
  def recip(a: FloatingPoint): FloatingPoint = {
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.frec(a.r, a.m, a.e)
    result
  }
  
  def recip_sqrt(a: FloatingPoint): FloatingPoint = {
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.frsqrt(a.r, a.m, a.e)
    result
  }

  def accum(v: FloatingPoint, en: Bool, last: Bool): FloatingPoint = {
    val result = Wire(new FloatingPoint(v.fmt))
    result.r := globals.bigIP.fltaccum(v.r, en, last, v.m, v.e)
    result
  }

  def fma(m0: FloatingPoint, m1: FloatingPoint, add: FloatingPoint): FloatingPoint = {
    assert(m0.fmt == m1.fmt && m1.fmt == add.fmt)
    val result = Wire(new FloatingPoint(m0.fmt))
    result.r := globals.bigIP.ffma(m0.r, m1.r, add.r, m0.m, m0.e)
    result
  }

  def fix2flt(a: FixedPoint, m: Int, e: Int): FloatingPoint = {
    val result = Wire(new FloatingPoint(m,e))
    result.r := globals.bigIP.fix2flt(a.r,a.s,a.d,a.f,m,e)
    result
  }
  def fix2fix(a: FixedPoint, s: Boolean, d: Int, f: Int, rounding: RoundingMode, saturating: OverflowMode): FixedPoint = {
    val result = Wire(new FixedPoint(s,d,f))
    result.r := globals.bigIP.fix2fix(a.r,a.s,a.d,a.f,s,d,f, rounding, saturating)
    result
  }
  def flt2fix(a: FloatingPoint, sign: Boolean, dec: Int, frac: Int, rounding: RoundingMode, saturating: OverflowMode): FixedPoint = {
    val result = Wire(new FixedPoint(sign,dec,frac))
    result.r := globals.bigIP.flt2fix(a.r,a.m,a.e,sign,dec,frac, rounding, saturating)
    result
  }

  def flt2flt(a: FloatingPoint, manOut: Int, expOut: Int): FloatingPoint = {
    val result = Wire(new FloatingPoint(manOut,expOut))
    result.r := globals.bigIP.flt2flt(a.r,a.m,a.e,manOut,expOut)
    result
  }


  def frand(seed: Int, m: Int, e: Int): FloatingPoint = {
      val size = m+e
      val flt_rng = Module(new PRNG(seed, size))
      val result = Wire(new FloatingPoint(m, e))
      flt_rng.io.en := true.B
      result.r := flt_rng.io.output
      result
  }

  def fixrand(seed: Int, bits: Int, en: Bool): FixedPoint = {
    val prng = Module(new PRNG(seed, bits))
    val result = Wire(new FixedPoint(false, bits, 0))
    prng.io.en := en
    result := prng.io.output
    result
  }


  def min[T <: chisel3.core.Data](a: T, b: T): T = {
    (a,b) match {
      case (aa:UInt,bb:UInt) => Mux(aa < bb, a, b)
      case (_,_) => a // TODO: implement for other types
    }
  }

  def max[T <: chisel3.core.Data](a: T, b: T): T = {
    (a,b) match {
      case (aa:UInt,bb:UInt) => Mux(aa > bb, a, b)
      case (_,_) => a // TODO: implement for other types
    }
  }

  // TODO: Use IP
  def fma(m0: FixedPoint, m1: FixedPoint, add: FixedPoint, delay: Int, flow: Bool): FixedPoint = {
    mul(m0, m1, Some(delay), flow, Truncate, Wrapping) + getRetimed(add, delay, flow)
  }

}

