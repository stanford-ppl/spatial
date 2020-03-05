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

  def log2(a: UInt, delay: Option[Double], flow: Bool, myName: String): UInt = {
    globals.bigIP.log2(a, delay.getOrElse(0.0).toInt, flow, myName)
  }
  def mul(a: UInt, b: UInt, delay: Option[Double], flow: Bool, myName: String): UInt = {
    val latency = delay.getOrElse(globals.target.fixmul_latency * b.getWidth).toInt
    globals.bigIP.multiply(a, b, latency, flow, myName)
  }

  def mul(a: SInt, b: SInt, delay: Option[Double], flow: Bool, myName: String): SInt = {
    val latency = delay.getOrElse(globals.target.fixmul_latency * b.getWidth).toInt
    globals.bigIP.multiply(a, b, latency, flow, myName)
  }

  def div(a: SInt, b: SInt, delay: Option[Double], flow: Bool, myName: String): SInt = {
    val latency = delay.getOrElse(globals.target.fixdiv_latency * b.getWidth).toInt
    globals.bigIP.divide(a, b, latency, flow, myName)
  }

  def div(a: UInt, b: UInt, delay: Option[Double], flow: Bool, myName: String): UInt = {
    val latency = delay.getOrElse(globals.target.fixdiv_latency * b.getWidth).toInt
    globals.bigIP.divide(a, b, latency, flow, myName)
  }

  def mod(a: UInt, b: UInt, delay: Option[Double], flow: Bool, myName: String): UInt = {
    val latency = delay.getOrElse(globals.target.fixmod_latency * b.getWidth).toInt
    globals.bigIP.mod(a, b, latency, flow, myName)
  }

  def mod(a: SInt, b: SInt, delay: Option[Double], flow: Bool, myName: String): SInt = {
    val latency = delay.getOrElse(globals.target.fixmod_latency * b.getWidth).toInt
    globals.bigIP.mod(a, b, latency, flow, myName)
  }

  def singleCycleDivide(num: SInt, den: SInt): SInt = num / den
  def singleCycleModulo(num: SInt, den: SInt): SInt = num % den
  def singleCycleDivide(num: UInt, den: UInt): UInt = num / den
  def singleCycleModulo(num: UInt, den: UInt): UInt = num % den

  // --- Fixed Point Operations --- //

  def sqrt(num: FixedPoint, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val result = Wire(new FixedPoint(num.s, num.d, num.f))
    val latency = delay.getOrElse(0.0).toInt
    if (num.f == 0) result.r := globals.bigIP.sqrt(num.r, latency, flow, myName)
    else if (num.f % 2 == 0) { // sqrt(r/2^f) = sqrt(r) / 2^(f/2)
      val intSqrt = globals.bigIP.sqrt(num.r, latency, flow, myName)
      result.r := intSqrt << (num.f/2)
    }
    else {// sqrt(r/2^f) = sqrt(r) / sqrt(2^f)
      val intSqrt = globals.bigIP.sqrt(num.r, latency, flow, myName)
      val denSqrt = globals.bigIP.sqrt((1.U << num.f), latency, flow, myName)
      result.r := Math.div(intSqrt << num.f, (1.U << (num.f/2)), Some(0.0), flow, myName)
    }
    result
  }
  def sin(num: FixedPoint, delay: Option[Double], myName: String): FixedPoint = {
    num //TODO
  }
  def cos(num: FixedPoint, delay: Option[Double], myName: String): FixedPoint = {
    num //TODO
  }
  def atan(num: FixedPoint, delay: Option[Double], myName: String): FixedPoint = {
    num //TODO
  }
  def sinh(num: FixedPoint, delay: Option[Double], myName: String): FixedPoint = {
    num //TODO
  }
  def cosh(num: FixedPoint, delay: Option[Double], myName: String): FixedPoint = {
    num //TODO
  }

  private def upcast(a: FixedPoint, b: FixedPoint): (FixedPoint, FixedPoint, FixedPoint, FixedPoint) = {
    // Compute upcasted type and return type
    val return_type = a.fmt combine b.fmt
    val upcast_type = return_type.copy(ibits = return_type.ibits + 1)

    // Get upcasted operators
    val result_upcast = Wire(new FixedPoint(upcast_type))
    val result        = Wire(new FixedPoint(return_type))
    val a_upcast   = a.toFixed(upcast_type, "")
    val b_upcast   = b.toFixed(upcast_type, "")

    (a_upcast, b_upcast, result_upcast, result)
  }

  // --- Fixed Point Operations --- //
  def floor(a: FixedPoint, myName: String): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt))
    result.r := util.Cat(a.raw_dec, 0.U(a.f.W))
    result
  }
  def ceil(a: FixedPoint, myName: String): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt))
    val stay = a.raw_frac === 0.U
    result.r := Mux(stay, a.r, util.Cat(a.raw_dec + 1.U, 0.U(a.f.W)))
    result
  }


  def add(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, round: RoundingMode, overflow: OverflowMode, myName: String): FixedPoint = {
    class AddWrapper(s: Boolean, d: Int, f: Int, delay: Option[Double], myName: String) extends Module{
      val io = IO(new Bundle{
        val a = Input(UInt((d + f).W))
        val b = Input(UInt((d + f).W))
        val flow = Input(Bool())
        val result = Output(UInt((d + f).W))
      })
      override def desiredName = myName
      val a = Wire(new FixedPoint(s,d,f)); a.r := io.a
      val b = Wire(new FixedPoint(s,d,f)); b.r := io.b
      val flow = io.flow

      val latency = delay.getOrElse(0.0).toInt
      // Allocate upcasted and result wires
      val (a_upcast, b_upcast, result_upcast, result) = upcast(a, b)

      // Instantiate an unsigned addition
      result_upcast.r := a_upcast.r + b_upcast.r

      // Downcast to result
      val expect_neg = if (a.s | b.s) a_upcast.msb & b_upcast.msb   else false.B
      val expect_pos = if (a.s | b.s) !a_upcast.msb & !b_upcast.msb else true.B
      val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow, latency, myName))
      fix2fixBox.io.a := result_upcast.r
      fix2fixBox.io.expect_neg := expect_neg
      fix2fixBox.io.expect_pos := expect_pos
      fix2fixBox.io.flow := flow
      result.r := fix2fixBox.io.b
      io.result := result.r
    }

    val module = Module(new AddWrapper(a.s, a.d, a.f, delay, myName))
    module.io.a := a.r
    module.io.b := b.r
    module.io.flow := flow
    val result = Wire(new FixedPoint(a.s,a.d,a.f))
    result.r := module.io.result
    result
  }

  def sub(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, round: RoundingMode, overflow: OverflowMode, myName: String): FixedPoint = {
    class SubWrapper(s: Boolean, d: Int, f: Int, delay: Option[Double], myName: String) extends Module{
      val io = IO(new Bundle{
        val a = Input(UInt((d + f).W))
        val b = Input(UInt((d + f).W))
        val flow = Input(Bool())
        val result = Output(UInt((d + f).W))
      })
      override def desiredName = myName
      val a = Wire(new FixedPoint(s,d,f)); a.r := io.a
      val b = Wire(new FixedPoint(s,d,f)); b.r := io.b
      val flow = io.flow

      val latency = delay.getOrElse(0.0).toInt
      // Allocate upcasted and result wires
      val (a_upcast, b_upcast, result_upcast, result) = upcast(a, b)

      // Instantiate an unsigned addition
      result_upcast.r := a_upcast.r - b_upcast.r

      // Downcast to result
      val expect_neg = if (a.s | b.s) a_upcast.msb & !b_upcast.msb else true.B
      val expect_pos = if (a.s | b.s) !a_upcast.msb & b_upcast.msb else false.B
      val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow, latency, myName))
      fix2fixBox.io.a := result_upcast.r
      fix2fixBox.io.expect_neg := expect_neg
      fix2fixBox.io.expect_pos := expect_pos
      fix2fixBox.io.flow := flow
      result.r := fix2fixBox.io.b
      io.result := result.r
    }

    val module = Module(new SubWrapper(a.s, a.d, a.f, delay, myName))
    module.io.a := a.r
    module.io.b := b.r
    module.io.flow := flow
    val result = Wire(new FixedPoint(a.s,a.d,a.f))
    result.r := module.io.result
    result
  }

  def mul(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, round: RoundingMode, overflow: OverflowMode, myName: String): FixedPoint = {
    class MulWrapper(s: Boolean, d: Int, f: Int, delay: Option[Double], myName: String) extends Module{
      val io = IO(new Bundle{
        val a = Input(UInt((d + f).W))
        val b = Input(UInt((d + f).W))
        val flow = Input(Bool())
        val result = Output(UInt((d + f).W))
      })
      override def desiredName = myName
      val a = Wire(new FixedPoint(s,d,f)); a.r := io.a
      val b = Wire(new FixedPoint(s,d,f)); b.r := io.b
      val flow = io.flow

      val latency = if (globals.retime || delay.isDefined) delay.getOrElse(globals.target.fixmul_latency * a.getWidth)
                    else 0.0
      val intMode = round == Truncate && overflow == Wrapping && (a.f == 0 | b.f == 0)

      // Compute upcasted type and return type
      val return_type = a.fmt combine b.fmt
      val upcast_type = if (intMode) return_type else if (overflow == Wrapping) return_type.copy(ibits = a.d max b.d, fbits = a.f + b.f) else return_type.copy(ibits = a.d + b.d, fbits = a.f + b.f)

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
        result_upcast.r := mul(a_upcast, b_upcast, Some(latency), flow, myName) >> scala.math.max(a.f, b.f)
      }
      else if (overflow == Wrapping) {
        val a_upcast: UInt = util.Cat(util.Fill(b.f + 0.max(b.d - a.d), a.msb), a.r)
        val b_upcast: UInt = if (b.litVal.isDefined) b.litVal.get.U((0.max(a.d - b.d) + a.f+b.d+b.f).W)
                             else util.Cat(util.Fill(0.max(a.d - b.d) + a.f, b.msb), b.r)

        result_upcast.r := mul(a_upcast, b_upcast, Some(latency), flow, myName)
      }
      else {
        val a_upcast: UInt = util.Cat(util.Fill(b.d+b.f, a.msb), a.r)
        val b_upcast: UInt = if (b.litVal.isDefined) b.litVal.get.U((a.d+a.f+b.d+b.f).W)
                             else util.Cat(util.Fill(a.d+a.f, b.msb), b.r)

        result_upcast.r := mul(a_upcast, b_upcast, Some(latency), flow, myName)
      }

      // Downcast to result
      val result = Wire(new FixedPoint(return_type))
      val expect_neg = if (a.s | b.s) getRetimed(a.msb ^ b.msb, latency.toInt) else false.B
      val expect_pos = if (a.s | b.s) getRetimed(!(a.msb ^ b.msb), latency.toInt) else true.B
      val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow, 0, "cast_" + myName))
      fix2fixBox.io.a := result_upcast.r
      fix2fixBox.io.expect_neg := expect_neg
      fix2fixBox.io.expect_pos := expect_pos
      fix2fixBox.io.flow := flow
      result.r := fix2fixBox.io.b
      io.result := result.r
    }

    val module = Module(new MulWrapper(a.s, a.d, a.f, delay, myName))
    module.io.a := a.r
    module.io.b := b.r
    module.io.flow := flow
    val result = Wire(new FixedPoint(a.s,a.d,a.f))
    result.r := module.io.result
    result
  }

  def div(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, round: RoundingMode, overflow: OverflowMode, myName: String): FixedPoint = {
    class DivWrapper(s: Boolean, d: Int, f: Int, delay: Option[Double], myName: String) extends Module{
      val io = IO(new Bundle{
        val a = Input(UInt((d + f).W))
        val b = Input(UInt((d + f).W))
        val flow = Input(Bool())
        val result = Output(UInt((d + f).W))
      })
      override def desiredName = myName
      val a = Wire(new FixedPoint(s,d,f)); a.r := io.a
      val b = Wire(new FixedPoint(s,d,f)); b.r := io.b
      val flow = io.flow

      val latency = if (globals.retime || delay.isDefined) delay.getOrElse(globals.target.fixdiv_latency * a.getWidth)
                    else 0.0

      val return_type = a.fmt combine b.fmt

      if (a.f == 0 && b.f == 0) {
        if (a.s | b.s) io.result := Math.div(a.sint, b.sint, Some(latency), flow, myName).FP(return_type).r
        else           io.result := Math.div(a.uint, b.uint, Some(latency), flow, myName).FP(return_type).r
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
          val a_upcast = a.upcastSInt(op_upcast_type, "cast_" + myName)
          val b_upcast = b.sint
          result_upcast.r := Math.div(a_upcast, b_upcast, Some(latency), flow, myName).asUInt
        }
        else {
          val a_upcast = a.upcastUInt(op_upcast_type, "cast_" + myName)
          val b_upcast = b.uint
          result_upcast.r := Math.div(a_upcast, b_upcast, Some(latency), flow, myName)
        }
        val result = Wire(new FixedPoint(return_type))
        val expect_neg = if (a.s | b.s) getRetimed(a.msb ^ b.msb, latency.toInt) else false.B
        val expect_pos = if (a.s | b.s) getRetimed(!(a.msb ^ b.msb), latency.toInt) else true.B
        val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow, 0, "cast_" + myName))
        fix2fixBox.io.a := result_upcast.r
        fix2fixBox.io.expect_neg := expect_neg
        fix2fixBox.io.expect_pos := expect_pos
        fix2fixBox.io.flow := flow
        result.r := fix2fixBox.io.b
        io.result := result.r
      }
    }

    val module = Module(new DivWrapper(a.s, a.d, a.f, delay, myName))
    module.io.a := a.r
    module.io.b := b.r
    module.io.flow := flow
    val result = Wire(new FixedPoint(a.s,a.d,a.f))
    result.r := module.io.result
    result
  }

  // TODO: No upcasting actually occurs here?
  def mod(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, round: RoundingMode, overflow: OverflowMode, myName: String): FixedPoint = {
    class ModWrapper(s: Boolean, d: Int, f: Int, delay: Option[Double], myName: String) extends Module{
      val io = IO(new Bundle{
        val a = Input(UInt((d + f).W))
        val b = Input(UInt((d + f).W))
        val flow = Input(Bool())
        val result = Output(UInt((d + f).W))
      })
      override def desiredName = myName
      val a = Wire(new FixedPoint(s,d,f)); a.r := io.a
      val b = Wire(new FixedPoint(s,d,f)); b.r := io.b
      val flow = io.flow

      val return_type = a.fmt combine b.fmt
      val upcast_type = return_type.copy(ibits = a.d + b.d, fbits = a.f + b.f)

      val result_upcast = Wire(new FixedPoint(upcast_type))
      val result = Wire(new FixedPoint(return_type))
      // Downcast to result
      result_upcast.r := Math.mod(a.uint, b.uint, delay, flow, myName)
      val fix2fixBox = Module(new fix2fixBox(result_upcast.s, result_upcast.d, result_upcast.f, result.s, result.d, result.f, round, overflow, 0, "cast_" + myName))
      fix2fixBox.io.a := result_upcast.r
      fix2fixBox.io.expect_neg := false.B
      fix2fixBox.io.expect_pos := false.B
      fix2fixBox.io.flow := flow
      result.r := fix2fixBox.io.b
      io.result := result.r
    }

    val module = Module(new ModWrapper(a.s, a.d, a.f, delay, myName))
    module.io.a := a.r
    module.io.b := b.r
    module.io.flow := flow
    val result = Wire(new FixedPoint(a.s,a.d,a.f))
    result.r := module.io.result
    result
  }

  def xor(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val upcast_type = a.fmt combine b.fmt
    val result = Wire(new FixedPoint(upcast_type))
    if (a.fmt == upcast_type && b.fmt == upcast_type) {
      if (a.s | b.s) result.r := getRetimed(a.r.asSInt ^ b.r.asSInt, latency, flow).r
      else           result.r := getRetimed(a.r ^ b.r, latency, flow).r
    }
    else { // should really catch all cases
      val a_up = Wire(new FixedPoint(upcast_type))
      val b_up = Wire(new FixedPoint(upcast_type))
      a.cast(a_up, "cast_a" + myName)
      b.cast(b_up, "cast_b" + myName)
      if (a.s | b.s) result.r := getRetimed(a_up.r.asSInt ^ b_up.r.asSInt, latency, flow).r
      else           result.r := getRetimed(a_up.r ^ b_up.r, latency, flow).r
    }
    result
  }

  def inv(a: FixedPoint, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FixedPoint(a.fmt))
    result.r := getRetimed(~a, latency, flow).r
    result
  }

  def and(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val upcast_type = a.fmt combine b.fmt
    val result = Wire(new FixedPoint(upcast_type))
    if (a.fmt == upcast_type && b.fmt == upcast_type) {
      if (a.s | b.s) result.r := getRetimed(a.r.asSInt & b.r.asSInt, latency, flow).r
      else           result.r := getRetimed(a.r & b.r, latency, flow).r
    }
    else { // should really catch all cases
      val a_up = Wire(new FixedPoint(upcast_type))
      val b_up = Wire(new FixedPoint(upcast_type))
      a.cast(a_up, "cast_a" + myName)
      b.cast(b_up, "cast_b" + myName)
      if (a.s | b.s) result.r := getRetimed(a_up.r.asSInt & b_up.r.asSInt, latency, flow).r
      else           result.r := getRetimed(a_up.r & b_up.r, latency, flow).r
    }
    result
  }

  def or(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val upcast_type = a.fmt combine b.fmt
    val result = Wire(new FixedPoint(upcast_type))
    if (a.fmt == upcast_type && b.fmt == upcast_type) {
      if (a.s | b.s) result.r := getRetimed(a.r.asSInt | b.r.asSInt, latency, flow).r
      else           result.r := getRetimed(a.r | b.r, latency, flow).r
    }
    else { // should really catch all cases
      val a_up = Wire(new FixedPoint(upcast_type))
      val b_up = Wire(new FixedPoint(upcast_type))
      a.cast(a_up, "cast_a" + myName)
      b.cast(b_up, "cast_b" + myName)
      if (a.s | b.s) result.r := getRetimed(a_up.r.asSInt | b_up.r.asSInt, latency, flow).r
      else           result.r := getRetimed(a_up.r | b_up.r, latency, flow).r
    }
    result
  }

  def arith_right_shift(a: FixedPoint, shift: Int, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt))
    val latency = delay.getOrElse(0.0).toInt
    val sgnextend = if (a.s) util.Fill(shift, a.msb) else util.Fill(shift, false.B)
    result.r := getRetimed(util.Cat(sgnextend, a(a.d+a.f-1, shift)), latency, flow)
    result
  }

  def arith_right_shift_div(a: FixedPoint, shift: Int, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt)); result.suggestName(myName)
    val latency = delay.getOrElse(0.0).toInt
    val sgnextend = if (a.s) util.Fill(shift, a.msb) else util.Fill(shift, false.B)
    val goesTo0 = if (a.fmt.sign) a(a.d+a.f-1, shift) === util.Fill(a.d+a.f - shift, true.B) && a(shift-1,0) =/= util.Fill(shift, false.B) else false.B
    val real = getRetimed(util.Cat(sgnextend, a(a.d+a.f-1, shift)), latency, flow)
    val zero = 0.U
    result.r := Mux(goesTo0, zero, real)
    result
  }

  def arith_left_shift(a: FixedPoint, shift: Int, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt)); result.suggestName(myName)
    val latency = delay.getOrElse(0.0).toInt
    result.r := getRetimed(a.r << shift, latency, flow)
    result
  }

  def logic_right_shift(a: FixedPoint, shift: Int, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val result = Wire(new FixedPoint(a.fmt)); result.suggestName(myName)
    val latency = delay.getOrElse(0.0).toInt
    result.r := getRetimed(a.r >> shift, latency, flow)
    result
  }

  def lt(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, myName: String): Bool = {
    val latency = delay.getOrElse(0.0).toInt
    val upcast_type = a.fmt combine b.fmt
    if (a.fmt == upcast_type && b.fmt == upcast_type) {
      if (a.s | b.s) getRetimed(a.r.asSInt < b.r.asSInt, latency, flow)
      else           getRetimed(a.r < b.r, latency, flow)
    }
    else { // should really catch all cases
      val a_up = Wire(new FixedPoint(upcast_type))
      val b_up = Wire(new FixedPoint(upcast_type))
      a.cast(a_up, "cast_a" + myName)
      b.cast(b_up, "cast_b" + myName)
      if (a.s | b.s) getRetimed(a_up.r.asSInt < b_up.r.asSInt, latency, flow)
      else           getRetimed(a_up.r < b_up.r, latency, flow)
    }
  }

  def lte(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, myName: String): Bool = {
    val latency = delay.getOrElse(0.0).toInt
    val upcast_type = a.fmt combine b.fmt
    if (a.fmt == upcast_type && b.fmt == upcast_type) {
      if (a.s | b.s) getRetimed(a.r.asSInt <= b.r.asSInt, latency, flow)
      else           getRetimed(a.r <= b.r, latency, flow)
    }
    else { // should really catch all cases
      val a_up = Wire(new FixedPoint(upcast_type))
      val b_up = Wire(new FixedPoint(upcast_type))
      a.cast(a_up, "cast_a" + myName)
      b.cast(b_up, "cast_b" + myName)
      if (a.s | b.s) getRetimed(a_up.r.asSInt <= b_up.r.asSInt, latency, flow)
      else           getRetimed(a_up.r <= b_up.r, latency, flow)
    }
  }

  def eql(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, myName: String): Bool = {
    val latency = delay.getOrElse(0.0).toInt
    val upcast_type = a.fmt combine b.fmt
    if (a.fmt == upcast_type && b.fmt == upcast_type) {
      if (a.s | b.s) getRetimed(a.r.asSInt === b.r.asSInt, latency, flow)
      else           getRetimed(a.r === b.r, latency, flow)
    }
    else { // should really catch all cases
      val a_up = Wire(new FixedPoint(upcast_type))
      val b_up = Wire(new FixedPoint(upcast_type))
      a.cast(a_up, "cast_a" + myName)
      b.cast(b_up, "cast_b" + myName)
      if (a.s | b.s) getRetimed(a_up.r.asSInt === b_up.r.asSInt, latency, flow)
      else           getRetimed(a_up.r === b_up.r, latency, flow)
    }
  }

  def neq(a: FixedPoint, b: FixedPoint, delay: Option[Double], flow: Bool, myName: String): Bool = {
    val latency = delay.getOrElse(0.0).toInt
    val upcast_type = a.fmt combine b.fmt
    if (a.fmt == upcast_type && b.fmt == upcast_type) {
      if (a.s | b.s) getRetimed(a.r.asSInt =/= b.r.asSInt, latency, flow)
      else           getRetimed(a.r =/= b.r, latency, flow)
    }
    else { // should really catch all cases
      val a_up = Wire(new FixedPoint(upcast_type))
      val b_up = Wire(new FixedPoint(upcast_type))
      a.cast(a_up, "cast_a" + myName)
      b.cast(b_up, "cast_b" + myName)
      if (a.s | b.s) getRetimed(a_up.r.asSInt =/= b_up.r.asSInt, latency, flow)
      else           getRetimed(a_up.r =/= b_up.r, latency, flow)
    }
  }

  def neg(a: FixedPoint, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    val latency = delay.getOrElse(0.0).toInt
    getRetimed(-a, latency, flow)
  }


  // --- Floating Point Operations --- //

  def fadd(a: FloatingPoint, b: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    // TODO: Make this a property of the DeviceTarget?
    assert(a.fmt == b.fmt)
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fadd(a.r, b.r, a.m, a.e, delay.getOrElse(0.0).toInt, flow, myName)
    result
  }

  def fsub(a: FloatingPoint, b: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    assert(a.fmt == b.fmt)
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fsub(a.r, b.r, a.m, a.e, delay.getOrElse(0.0).toInt, flow, myName)
    result
  }

  def fmul(a: FloatingPoint, b: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    assert(a.fmt == b.fmt)
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fmul(a.r, b.r, a.m, a.e, delay.getOrElse(0.0).toInt, flow, myName)
    result
  }

  def fdiv(a: FloatingPoint, b: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    assert(a.fmt == b.fmt)
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fdiv(a.r, b.r, a.m, a.e, delay.getOrElse(0.0).toInt, flow, myName)
    result
  }

  def flt(a: FloatingPoint, b: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): Bool = {
    val latency = delay.getOrElse(0.0).toInt
    assert(a.fmt == b.fmt)
    val result = Wire(new Bool)
    result := globals.bigIP.flt(a.r, b.r, a.m, a.e, latency, flow, myName)
    result
  }

  def flte(a: FloatingPoint, b: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): Bool = {
    val latency = delay.getOrElse(0.0).toInt
    assert(a.fmt == b.fmt)
    val result = Wire(new Bool)
    result := globals.bigIP.fle(a.r, b.r, a.m, a.e, latency, flow, myName)
    result
  }

  def feql(a: FloatingPoint, b: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): Bool = {
    val latency = delay.getOrElse(0.0).toInt
    assert(a.fmt == b.fmt)
    val result = Wire(new Bool)
    result := globals.bigIP.feq(a.r, b.r, a.m, a.e, latency, flow, myName)
    result
  }

  def fneq(a: FloatingPoint, b: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): Bool = {
    val latency = delay.getOrElse(0.0).toInt
    assert(a.fmt == b.fmt)
    val result = Wire(new Bool)
    result := globals.bigIP.fne(a.r, b.r, a.m, a.e, latency, flow, myName)
    result
  }

  def fabs(a: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fabs(a.r, a.m, a.e, latency, flow, myName)
    result
  }

  def fsqrt(a: FloatingPoint, latency: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val result = Wire(new FloatingPoint(a.fmt))
    result := globals.bigIP.fsqrt(a.r, a.m, a.e, latency.getOrElse(0.0).toInt, flow, myName)
    result
  }

  def exp(a: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fexp(a.r, a.m, a.e, latency, flow, myName)
    result
  }

  def tanh(a: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.ftanh(a.r, a.m, a.e, latency, flow, myName)
    result
  }

  def sigmoid(a: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(a.fmt))
    result := globals.bigIP.fsigmoid(a.r, a.m, a.e, latency, flow, myName)
    result
  }

  def ln(a: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.fln(a.r, a.m, a.e, latency, flow, myName)
    result
  }
  
  def recip(a: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.frec(a.r, a.m, a.e, latency, flow, myName)
    result
  }
  
  def recip_sqrt(a: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(a.fmt))
    result.r := globals.bigIP.frsqrt(a.r, a.m, a.e, latency, flow, myName)
    result
  }

  def accum(v: FloatingPoint, en: Bool, last: Bool, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(v.fmt))
    result.r := globals.bigIP.fltaccum(v.r, en, last, v.m, v.e,latency, flow, myName)
    result
  }

  def fma(m0: FloatingPoint, m1: FloatingPoint, add: FloatingPoint, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    assert(m0.fmt == m1.fmt && m1.fmt == add.fmt)
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(m0.fmt))
    // TODO: Module naming is wrong!
    result.r := globals.bigIP.fadd(globals.bigIP.fmul(m0.r, m1.r, m0.m, m0.e, latency, flow, myName), getRetimed(add.r, latency, flow), m0.m, m0.e, 0, flow, "")
    result
  }

  def fix2flt(a: FixedPoint, m: Int, e: Int, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(m,e))
    if (a.f == 0) result.r := globals.bigIP.fix2flt(a.r,a.s,a.d,a.f,m,e, latency, flow, myName)
    // else if (a.f > e) {
    //   Console.println(s"[WARN] Conversion from ${a.fmt} to Float[$m,$e] may lose precision!")
    //   val truncate = a.f - e + 1
    //   val tmp = Wire(new FixedPoint(a.s, a.d + truncate, a.f - truncate))
    //   tmp.r := a.r >> truncate
    //   result.r := fix2flt(tmp, m, e ).r
    // }
    else {
      // val raw = globals.bigIP.fix2flt(a.r,a.s,a.d,a.f,m,e)
      // val exp = raw(m+e-1,m)
      // val newexp = exp - scala.math.pow(2,a.f).toInt.U(e.W)
      // val newall = chisel3.util.Cat(raw.msb, chisel3.util.Cat(newexp, raw(m-1,0)))
      // result.r := newall
      val tmp = Wire(new FloatingPoint(m,e))
      tmp.r := globals.bigIP.fix2flt(a.r,a.s,a.d,a.f,m,e, latency, flow, myName)
      result.r := Math.fdiv(tmp, FloatingPoint(m,e,scala.math.pow(2,a.f)),Some(0.0),true.B, "div_" + myName).r
    }
    result
  }
  def fix2fix(a: FixedPoint, s: Boolean, d: Int, f: Int, delay: Option[Double], flow: Bool, rounding: RoundingMode, saturating: OverflowMode, myName: String): FixedPoint = {
    class Fix2FixWrapper() extends Module{
      val io = IO(new Bundle{
        val b = Input(UInt((a.d + a.f).W))
        val flow = Input(Bool())
        val result = Output(UInt((d + f).W))
      })
      override def desiredName = myName
      val b = Wire(new FixedPoint(a.s,a.d,a.f)); b.r := io.b
      val flow = io.flow

      val latency = delay.getOrElse(0.0).toInt
      val result = Wire(new FixedPoint(s,d,f))
      result.r := globals.bigIP.fix2fix(b.r,b.s,b.d,b.f,s,d,f, latency, flow, rounding, saturating,myName)
      io.result := result.r
    }

    val module = Module(new Fix2FixWrapper())
    module.io.b := a.r
    module.io.flow := flow
    val result = Wire(new FixedPoint(s,d,f))
    result.r := module.io.result
    result
  }
  def fix2fix(a: UInt, s: Boolean, d: Int, f: Int, delay: Option[Double], flow: Bool, rounding: RoundingMode, saturating: OverflowMode, myName: String): FixedPoint = {
    class Fix2FixWrapper() extends Module{
      val io = IO(new Bundle{
        val b = Input(UInt((a.getWidth).W))
        val flow = Input(Bool())
        val result = Output(UInt((d + f).W))
      })
      override def desiredName = myName
      val b = io.b
      val flow = io.flow

      val latency = delay.getOrElse(0.0).toInt
      val result = Wire(new FixedPoint(s,d,f))
      result.r := globals.bigIP.fix2fix(b,false,b.getWidth,0,s,d,f, latency, flow, rounding, saturating, myName)
      io.result := result.r
    }

    val module = Module(new Fix2FixWrapper())
    module.io.b := a.r
    module.io.flow := flow
    val result = Wire(new FixedPoint(s,d,f))
    result.r := module.io.result
    result
  }
  def fix2fix(a: SInt, s: Boolean, d: Int, f: Int, delay: Option[Double], flow: Bool, rounding: RoundingMode, saturating: OverflowMode, myName: String): FixedPoint = {
    class Fix2FixWrapper() extends Module{
      val io = IO(new Bundle{
        val b = Input(SInt((a.getWidth).W))
        val flow = Input(Bool())
        val result = Output(UInt((d + f).W))
      })
      override def desiredName = myName
      val b = io.b
      val flow = io.flow

      val latency = delay.getOrElse(0.0).toInt
      val result = Wire(new FixedPoint(s,d,f))
      result.r := globals.bigIP.fix2fix(b.asUInt,true,b.getWidth,0,s,d,f, latency, flow, rounding, saturating, myName)
      io.result := result.r
    }

    val module = Module(new Fix2FixWrapper())
    module.io.b := a.r
    module.io.flow := flow
    val result = Wire(new FixedPoint(s,d,f))
    result.r := module.io.result
    result
  }
  def flt2fix(a: FloatingPoint, sign: Boolean, dec: Int, frac: Int, delay: Option[Double], flow: Bool, rounding: RoundingMode, saturating: OverflowMode, myName: String): FixedPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FixedPoint(sign,dec,frac))
    if (frac == 0) result.r := globals.bigIP.flt2fix(a.r,a.m,a.e,sign,dec,frac, latency, flow, rounding, saturating, myName)
    // else if (frac > a.e) {
    //   Console.println(s"[WARN] Conversion from ${a.fmt} to Fix[$sign,$dec,$frac] may lose precision!")
    //   val truncate = frac - a.e + 1
    //   result.r := flt2fix(a,sign,dec+truncate,frac-truncate,rounding,saturating).r << truncate
    // }
    else           result.r := globals.bigIP.flt2fix(Math.fmul(a, FloatingPoint(a.m,a.e,scala.math.pow(2,frac)), Some(0.0), true.B, myName).r,a.m,a.e,sign,dec,frac,latency, flow, rounding, saturating, myName)
    // else {
    //   val exp = a.raw_exp
    //   val newexp = exp + scala.math.pow(2,frac).toInt.U(a.e.W)
    //   val newall = chisel3.util.Cat(a.msb, chisel3.util.Cat(newexp, a.raw_mantissa))
    //   result.r := globals.bigIP.flt2fix(newall, a.m, a.e, sign, dec, frac, rounding, saturating)
    // }
    result
  }

  def flt2flt(a: FloatingPoint, manOut: Int, expOut: Int, delay: Option[Double], flow: Bool, myName: String): FloatingPoint = {
    val latency = delay.getOrElse(0.0).toInt
    val result = Wire(new FloatingPoint(manOut,expOut))
    result.r := globals.bigIP.flt2flt(a.r,a.m,a.e,manOut,expOut, latency, flow, myName)
    result
  }


  def frand(seed: Int, m: Int, e: Int, en: Bool, myName: String): FloatingPoint = {
      val size = m+e
      val flt_rng = Module(new PRNG(seed, size))
      val result = Wire(new FloatingPoint(m, e))
      flt_rng.io.en := en
      result.r := flt_rng.io.output
      result
  }

  def fixrand(seed: Int, bits: Int, en: Bool, myName: String): FixedPoint = {
    val prng = Module(new PRNG(seed, bits))
    val result = Wire(new FixedPoint(false, bits, 0))
    prng.io.en := en
    result := prng.io.output
    result
  }

/*
  def popcount[T <: chisel3.core.UInt](data : T) : UInt = {
    val popcnter = Module(new Popcount())
    val result = Wire(new UInt)
    popcnter.io.bit_string := data
    result := popcnter.io.popcnt_out
    result
  }
 */

  def min[T <: chisel3.core.Data](a: T, b: T, myName: String): T = {
    (a,b) match {
      case (aa:UInt,bb:UInt) => Mux(aa < bb, a, b)
      case (_,_) => a // TODO: implement for other types
    }
  }

  def max[T <: chisel3.core.Data](a: T, b: T, myName: String): T = {
    (a,b) match {
      case (aa:UInt,bb:UInt) => Mux(aa > bb, a, b)
      case (_,_) => a // TODO: implement for other types
    }
  }
 
  // TODO: Use IP core.  Also be consistent with Module wrapping around logic in other defs
  def fma(m0: FixedPoint, m1: FixedPoint, add: FixedPoint, delay: Option[Double], flow: Bool, myName: String): FixedPoint = {
    class FMAWrapper(s: Boolean, d: Int, f: Int, delay: Option[Double], myName: String) extends Module{
      val io = IO(new Bundle{
        val m0 = Input(UInt((d + f).W))
        val m1 = Input(UInt((d + f).W))
        val add = Input(UInt((d + f).W))
        val flow = Input(Bool())
        val result = Output(UInt((d + f).W))
      })
      override def desiredName = myName
      val m0 = Wire(new FixedPoint(s,d,f)); m0.r := io.m0
      val m1 = Wire(new FixedPoint(s,d,f)); m1.r := io.m1
      val add = Wire(new FixedPoint(s,d,f)); add.r := io.add
      io.result := (mul(m0, m1, delay, io.flow, Truncate, Wrapping, "fmamul_" + myName) + getRetimed(add, delay.getOrElse(0.0).toInt, io.flow)).r
    }
    
    val module = Module(new FMAWrapper(m0.s, m0.d, m0.f, delay, myName))
    module.io.m0 := m0.r
    module.io.m1 := m1.r
    module.io.add := add.r
    module.io.flow := flow
    val result = Wire(new FixedPoint(m0.s,m0.d,m0.f))
    result.r := module.io.result
    result
  }

}

