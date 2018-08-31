package types

import chisel3._
import templates._
import templates.ops._
import chisel3.util

import scala.language.experimental.macros

import chisel3.internal._
import chisel3.internal.firrtl._
import chisel3.internal.sourceinfo._
import chisel3.internal.firrtl.PrimOp.AsUIntOp

sealed trait MSBCasting
object Lazy extends MSBCasting
object Saturation extends MSBCasting

sealed trait LSBCasting
object Truncate extends LSBCasting
object Unbiased extends LSBCasting

// Raw numbers.. This should be deprecated
class RawBits(b: Int) extends Bundle { 
	val raw = UInt(b.W)

	// Conversions
	def storeFix(dst: FixedPoint): Unit = { 
	  assert(dst.d + dst.f == b)
	  dst.number := raw
	}

	// Arithmetic

  override def cloneType = (new RawBits(b)).asInstanceOf[this.type] // See chisel3 bug 358
}

// Fixed point numbers
class FixedPoint(val s: Boolean, val d: Int, val f: Int, val litVal: Option[BigInt] = None) extends Bundle {
	// Overloaded
	def this(s: Int, d: Int, f: Int) = this(s == 1, d, f)
	def this(tuple: (Boolean, Int, Int)) = this(tuple._1, tuple._2, tuple._3)

	def apply(msb:Int, lsb:Int): UInt = this.number(msb,lsb)
	def apply(bit:Int): Bool = this.number(bit)

    def toSeq: Seq[FixedPoint] = Seq(this)

	// Properties
	// val litNum = if (litVal.isDefined) Some(litVal.get.S((d + f + 1).W).asUInt().apply(d+f,0)) else None
	val number = UInt((d + f).W)
	val debug_overflow = Bool()

	def raw():UInt = number
	def r():UInt = number
	// Conversions
	def storeRaw(dst: RawBits): Unit = {
		dst.raw := number
	}
	def reverse: UInt = chisel3.util.Reverse(this.number)

	def msb():Bool = number(d+f-1)


	def cast[T](dest: T, rounding: LSBCasting = Truncate, saturating: MSBCasting = Lazy, sign_extend: scala.Boolean = true, expect_neg: Bool = false.B, expect_pos: Bool = false.B): Unit = {
		dest match {
			case dst: FixedPoint if this.litVal.isEmpty => 
				val has_frac = dst.f > 0
				val has_dec = dst.d > 0
				val up_frac = dst.f max 1
				val up_dec = dst.d max 1
				val tmp_frac = Wire(UInt(up_frac.W))
				val new_frac = Wire(UInt(up_frac.W))
				val new_dec = Wire(UInt(up_dec.W))
				if (!has_frac) { tmp_frac := 0.U(1.W) }
				if (!has_frac) { new_frac := 0.U(1.W) }
				if (!has_dec) { new_dec := 0.U(1.W) }

				// Compute new frac part
				// val new_frac = Wire(UInt(dst.f.W))
				val shave_f = f - dst.f
				val shave_d = d - dst.d
				if (has_frac) {
					if (dst.f < f) { // shrink decimals
						rounding match {
							case Truncate => 
								tmp_frac := number(shave_f + dst.f - 1, shave_f)
								// (0 until dst.f).map{ i => number(shave_f + i)*scala.math.pow(2,i).toInt.U }.reduce{_+_}
							case Unbiased => 
								val prng = Module(new PRNG(scala.math.abs(scala.util.Random.nextInt)))
								prng.io.en := true.B
								val salted = number + prng.io.output(shave_f-1,0)
								tmp_frac := salted(shave_f + dst.f -1, shave_f)
							// case "biased" =>
							// 	Mux(number(shave_f-1), number + 1.U(shave_f.W) << (shave_f-1), number(shave_f + dst.f - 1, shave_f)) // NOT TESTED!!
							case _ =>
								tmp_frac := 0.U(dst.f.W)
								// TODO: throw error
						}
					} else if (dst.f > f) { // expand decimals
						val expand = dst.f - f
						if (f > 0) { tmp_frac := util.Cat(number(f-1,0), 0.U(expand.W))} else {tmp_frac := 0.U(expand.W)}
						// (0 until dst.f).map{ i => if (i < expand) {0.U} else {number(i - expand)*scala.math.pow(2,i).toInt.U}}.reduce{_+_}
					} else { // keep same
						tmp_frac := number(dst.f-1,0)
						// (0 until dst.f).map{ i => number(i)*scala.math.pow(2,i).toInt.U }.reduce{_+_}
					}
				}

				// Compute new dec part (concatenated with frac part from before)
				if (has_dec) {
					if (dst.d < d) { // shrink decimals
						saturating match { 
							case Lazy =>
								dst.debug_overflow := (0 until shave_d).map{i => number(d + f - 1 - i)}.reduce{_||_}
								new_frac := tmp_frac
								new_dec := number(dst.d + f - 1, f)
								// (0 until dst.d).map{i => number(f + i) * scala.math.pow(2,i).toInt.U}.reduce{_+_}
							case Saturation =>
								val sign = number.msb
								val overflow = ((sign & expect_pos) | (~sign & expect_neg)) 
							  	val not_saturated = ( number(f+d-1,f+d-1-shave_d) === 0.U(shave_d.W) ) | ( ~number(f+d-1,f+d-1-shave_d) === 0.U(shave_d.W) )

							  	val saturated_frac = Mux(expect_pos, 
							  			util.Cat(util.Fill(up_frac, true.B)), 
							  			Mux(expect_neg, 0.U(up_frac.W), 0.U(up_frac.W)))
							  	val saturated_dec = Mux(expect_pos, 
							  			util.Cat(~(dst.s | s).B, util.Fill(up_dec-1, true.B)), 
							  			Mux(expect_neg, 1.U((dst.d).W) << (dst.d-1), 1.U((dst.d).W) << (dst.d-1))) 

							  	new_frac := Mux(number === 0.U, 0.U, Mux(not_saturated & ~overflow, tmp_frac, saturated_frac))
							  	new_dec := Mux(number === 0.U, 0.U, Mux(not_saturated & ~overflow, number(dst.d + f - 1, f), saturated_dec))
							case _ =>
								new_frac := tmp_frac
								new_dec := 0.U(dst.d.W)
						}
					} else if (dst.d > d) { // expand decimals
						val expand = dst.d - d
						val sgn_extend = if (s & sign_extend) { number(d+f-1) } else {0.U(1.W)}
						new_frac := tmp_frac
						new_dec := util.Cat(util.Fill(expand, sgn_extend), number(f+d-1, f))
						// (0 until dst.d).map{ i => if (i >= dst.d - expand) {sgn_extend*scala.math.pow(2,i).toInt.U} else {number(i+f)*scala.math.pow(2,i).toInt.U }}.reduce{_+_}
					} else { // keep same
						new_frac := tmp_frac
						new_dec := number(f+d-1, f)
						// (0 until dst.d).map{ i => number(i + f)*scala.math.pow(2,i).toInt.U }.reduce{_+_}
					}

				}

				if (has_dec & has_frac) {
					dst.number := chisel3.util.Cat(new_dec, new_frac)	
				} else if (has_dec & !has_frac) {
					dst.number := new_dec		
				} else if (!has_dec & has_frac) {
					dst.number := tmp_frac
				}
				
				
				// dst.number := util.Cat(new_dec, new_frac) //new_frac + new_dec*(scala.math.pow(2,dst.f).toInt.U)
			case dst: UInt if this.litVal.isEmpty => 
				val result = Wire(new FixedPoint(true, dst.getWidth, 0))
				this.cast(result, rounding, saturating, sign_extend, expect_neg, expect_pos)
				dst := result.r
			case dst: FixedPoint if this.litVal.isDefined =>  // Likely that there are mistakes here
				val f_gain = dst.f - this.f
				val d_gain = dst.d - this.d
				val salt = rounding match {case Truncate => BigInt(0); case Unbiased if (f_gain < 0) => BigInt((scala.math.random * ((1 << -f_gain).toDouble)).toLong); case _ => BigInt(0)}
				val newlit = saturating match {
					case Lazy => 
						if (f_gain < 0 & d_gain >= 0)       (this.litVal.get + salt) >> -f_gain
						else if (f_gain >= 0 & d_gain >= 0) (this.litVal.get) << f_gain
						else if (f_gain >= 0 & d_gain < 0)  ((this.litVal.get + salt) >> -f_gain) & BigInt((1 << (dst.d + dst.f + 1)) - 1)
						else ((this.litVal.get) << f_gain) & BigInt((1 << (dst.d + dst.f + 1)) -1)
					case Saturation => 
						if (this.litVal.get > BigInt((1 << (dst.d + dst.f + 1))-1)) BigInt((1 << (dst.d + dst.f + 1))-1)
					    else {
							if (f_gain < 0 & d_gain >= 0)       (this.litVal.get + salt) >> -f_gain
							else if (f_gain >= 0 & d_gain >= 0) (this.litVal.get) << f_gain
							else if (f_gain >= 0 & d_gain < 0)  ((this.litVal.get + salt) >> -f_gain) & BigInt((1 << (dst.d + dst.f + 1)) - 1)
							else ((this.litVal.get) << f_gain) & BigInt((1 << (dst.d + dst.f + 1)) -1)
					    }
					case _ => 
						if (f_gain < 0 & d_gain >= 0)       (this.litVal.get + salt) >> -f_gain
						else if (f_gain >= 0 & d_gain >= 0) (this.litVal.get) << f_gain
						else if (f_gain >= 0 & d_gain < 0)  ((this.litVal.get + salt) >> -f_gain) & BigInt((1 << (dst.d + dst.f + 1)) - 1)
						else ((this.litVal.get) << f_gain) & BigInt((1 << (dst.d + dst.f + 1)) -1)
					}
				val result = Wire(new FixedPoint(dst.s, dst.d, dst.f, Some(newlit)))
				dst.r := newlit.U((dst.d + dst.f).W)
		}
	}
	

	def raw_dec[T]: UInt = this.number(d+f-1, f)
	def rd[T]: UInt = this.number(d+f-1, f)
	def raw_frac[T]: UInt = this.number(f, 0)
	def rf[T]: UInt = this.number(f, 0)

	// Arithmetic
	override def connect (rawop: Data)(implicit sourceInfo: SourceInfo, connectionCompileOptions: chisel3.core.CompileOptions): Unit = {
		rawop match {
			case op: FixedPoint =>
				number := op.number
			case op: UInt =>
				number := op
		}
	}

	def +[T] (rawop: T, rounding:LSBCasting = Truncate, saturating:MSBCasting = Lazy): FixedPoint = {
		rawop match {
			case op: FixedPoint => 
				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d) + 1, scala.math.max(op.f, f))
				val return_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val full_result = Wire(new FixedPoint(upcasted_type))
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				// Do upcasted operation
				this.cast(lhs)
				op.cast(rhs)
				full_result.number := lhs.number + rhs.number
				// Downcast to result
				val result = Wire(new FixedPoint(return_type))
				val expect_neg = if (op.s | s) {lhs.msb & rhs.msb} else false.B
				val expect_pos = if (op.s | s) {~lhs.msb & ~rhs.msb} else true.B
				full_result.cast(result, rounding = rounding, saturating = saturating, expect_neg = expect_neg, expect_pos = expect_pos)
				result
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this + op_cast
			case op: SInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op.asUInt)
				number + op_cast
		}
	}
	def <+>[T] (rawop: T): FixedPoint = {this.+(rawop, saturating = Saturation)}

	def -[T] (rawop: T, rounding:LSBCasting = Truncate, saturating:MSBCasting = Lazy): FixedPoint = {
		rawop match { 
			case op: FixedPoint => 
				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d) + 1, scala.math.max(op.f, f))
				val return_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val full_result = Wire(new FixedPoint(upcasted_type))
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				// Do upcasted operation
				this.cast(lhs)
				op.cast(rhs)
				full_result.number := lhs.number - rhs.number
				// Downcast to result
				val result = Wire(new FixedPoint(return_type))
				val expect_neg = if (op.s | s) {(lhs.msb & ~rhs.msb)} else true.B
				val expect_pos = if (op.s | s) {(~lhs.msb & rhs.msb)} else false.B
				full_result.cast(result, rounding = rounding, saturating = saturating, expect_neg = expect_neg, expect_pos = expect_pos)
				result
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this - op_cast
			case op: SInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op.asUInt)
				number - op_cast

		}
	}
	def <->[T] (rawop: T): FixedPoint = {this.-(rawop, saturating = Saturation)}

	def *-*[T] (rawop: T): FixedPoint = {this.*-*(rawop, None, true.B)}

	def *-*[T] (rawop: T, delay: Option[Double], flow: Bool, rounding:LSBCasting = Truncate, saturating:MSBCasting = Lazy): FixedPoint = {
		rawop match { 
			case op: FixedPoint => 
				val op_latency = if (Utils.retime | delay.isDefined) { 
					if (delay.isDefined) delay.get.toDouble
					else (Utils.fixmul_latency * op.getWidth).toDouble
				} else 0

				// Compute upcasted type and return type
				val upcasted_type = if (rounding == Truncate && saturating == Lazy && (op.f == 0 | f == 0)) (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
									else (op.s | s, op.d + d, op.f + f)
				val return_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val full_result = Wire(new FixedPoint(upcasted_type))
				// Do upcasted operation
				if (rounding == Truncate && saturating == Lazy && (op.f == 0 | f == 0)) {
					// val expanded_self = if (op.f != 0) util.Cat(util.Fill(op.f, this.msb), this.number) else this.number
					// val expanded_op = if (f != 0) util.Cat(util.Fill(f, op.msb), op.number) else op.number
					val rhs_bits = op.f - f
					val expanded_self = if (rhs_bits > 0) util.Cat(this.number, util.Fill(rhs_bits, false.B)) else this.number
					val expanded_op = if (rhs_bits < 0 && op.litVal.isEmpty) util.Cat(op.number, util.Fill(-rhs_bits, false.B)) else if (op.litVal.isDefined) op.litVal.get.U((d+f).W) else op.number
					full_result.number := (expanded_self.*-*(expanded_op, Some(op_latency), flow)) >> scala.math.max(op.f, f)
				} else {
					val expanded_self = util.Cat(util.Fill(op.d+op.f, this.msb), this.number)
					val expanded_op = if (op.litVal.isDefined) op.litVal.get.U((d+f+op.d+op.f).W) else util.Cat(util.Fill(d+f, op.msb), op.number)
					full_result.number := expanded_self.*-*(expanded_op, Some(op_latency), flow)
				}

				// Downcast to result
				val result = Wire(new FixedPoint(return_type))
				val expect_neg = if (op.s | s) {Utils.getRetimed((this.msb ^ op.msb), op_latency.toInt)} else false.B
				val expect_pos = if (op.s | s) {Utils.getRetimed(~(this.msb ^ op.msb), op_latency.toInt)} else true.B
				full_result.cast(result, rounding = rounding, saturating = saturating, expect_neg = expect_neg, expect_pos = expect_pos)
				result
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this.*-*(op_cast, delay, flow)
			case op: SInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op.asUInt)
				number.*-*(op_cast, delay, flow)

			}
	}

	def /-/[T] (rawop: T): FixedPoint = {this./-/(rawop, None, true.B)}
	def /-/[T] (rawop: T, delay: Option[Double], flow: Bool, rounding:LSBCasting = Truncate, saturating:MSBCasting = Lazy): FixedPoint = {
		rawop match { 
			case op: FixedPoint => 
				val op_latency = if (Utils.retime | !delay.isDefined) { 
					if (delay.isDefined) delay.get.toDouble
					else (Utils.fixdiv_latency * op.getWidth).toDouble
				} else 0

				if (op.f + f == 0) {
					if (op.s | s) {
						val denominator = if (op.litVal.isDefined) op.litVal.get.S((op.d).W) else op.number.asSInt
						(this.number.asSInt./-/(denominator, Some(op_latency), flow)).FP(false, scala.math.max(op.d, d), scala.math.max(op.f, f))
					} else {
						val denominator = if (op.litVal.isDefined) op.litVal.get.U((op.d).W) else op.number
						(this.number./-/(denominator, Some(op_latency), flow)).FP(false, scala.math.max(op.d, d), scala.math.max(op.f, f))
					}
				} else {
					// Compute upcasted type and return type
					val upcasted_type = if (rounding == Truncate && saturating == Lazy) (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f) + 1 )
										else (op.s | s, op.d + d, op.f + f + 1)
					val return_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
					// Get upcasted operators
					val full_result = Wire(new FixedPoint(upcasted_type))
					// Do upcasted operation
					// TODO: Should go back and clean this a little, eventually..
					if (op.s | s) {
						val numerator = util.Cat(this.number, 0.U(upcasted_type._3.W)).asSInt
						val denominator = if (op.litVal.isDefined) op.litVal.get.S((op.d + op.f).W) else op.number.asSInt
						full_result.number := (numerator./-/(denominator, Some(op_latency), flow)).asUInt
					} else {
						val numerator = util.Cat(this.number, 0.U(upcasted_type._3.W))
						val denominator = if (op.litVal.isDefined) op.litVal.get.U((op.d + op.f).W) else op.number
						full_result.number := (numerator./-/(denominator, Some(op_latency), flow)) // Not sure why we need the +1 in pow2
					}
					// Downcast to result
					val result = Wire(new FixedPoint(return_type))
					val expect_neg = if (op.s | s) {Utils.getRetimed((op.msb ^ this.msb), op_latency.toInt)} else false.B
					val expect_pos = if (op.s | s) {Utils.getRetimed(~(this.msb ^ op.msb), op_latency.toInt)} else true.B
					full_result.cast(result, rounding = rounding, saturating = saturating, expect_neg = expect_neg, expect_pos = expect_pos)
					result					
				}
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this./-/(op_cast, delay, flow)
			case op: SInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op.asUInt)
				number./-/(op_cast, delay, flow)

		}
	}

	def %-%[T] (rawop: T): FixedPoint = {this.%-%(rawop, None, true.B)}
	def %-%[T] (rawop: T, delay: Option[Double], flow: Bool): FixedPoint = {
		rawop match { 
			case op: FixedPoint => 
				// Compute upcasted type and return type
				// val upcasted_type = if (rounding == Truncate && saturating == Lazy) (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
			    val upcasted_type = (op.s | s, op.d + d, op.f + f)
				val return_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val full_result = Wire(new FixedPoint(upcasted_type))
				val denominator = if (op.litVal.isDefined) op.litVal.get.U((op.d + op.f).W) else op.number
				// Do upcasted operation
				full_result.number := this.number.%-%(denominator, delay, flow) // Not sure why we need the +1 in pow2
				// Downcast to result
				val result = Wire(new FixedPoint(return_type))
				full_result.cast(result)
				result
			case op: UInt =>
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this.%-%(op_cast, delay, flow)

		}
	}

	def floor[T] (): FixedPoint = {
		val return_type = (s, d, f)
		val result = Wire(new FixedPoint(return_type))
		result.r := util.Cat(this.raw_dec, util.Fill(f, false.B))			
		result
	}

	def ceil[T] (): FixedPoint = {
		val return_type = (s, d, f)
		val result = Wire(new FixedPoint(return_type))
		val stay = this.raw_frac === 0.U
		result.r := Mux(stay, this.number.r, util.Cat(this.raw_dec + 1.U, util.Fill(f, false.B)))
		result
	}

	def >>[T] (shift: Int, sgnextend: Boolean = true): FixedPoint = {
		val return_type = (s, d, f)
		val result = Wire(new FixedPoint(return_type))
		if (sgnextend & s) {
			result.r := util.Cat(util.Fill(shift, number.msb), number(d+f-1, shift))
		} else {
			result.r := this.number >> shift			
		}
		result
	}
	def >>>[T] (shift: Int): FixedPoint = {this.>>(shift, sgnextend = false)}

	def <<[T] (shift: Int): FixedPoint = {
		val return_type = (s, d, f)
		val result = Wire(new FixedPoint(return_type))
		result.r := this.number << shift			
		result
	}

	def <[T] (rawop: T): Bool = { // TODO: Probably completely wrong for signed fixpts
		rawop match { 
			case op: FixedPoint => 

				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				this.cast(lhs)
				op.cast(rhs)
				if (op.s | s) {lhs.number.asSInt < rhs.number.asSInt} else {lhs.number < rhs.number}
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this < op_cast
			case op: SInt => 
				val op_cast = Utils.FixedPoint(true, op.getWidth max this.d, this.f, op.asUInt)
				number < op_cast
		}
	}

	def ^[T] (rawop: T): FixedPoint = { 
		rawop match { 
			case op: FixedPoint => 

				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				val return_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				val res = Wire(new FixedPoint(return_type))
				this.cast(lhs)
				op.cast(rhs)
				res.r := lhs.r ^ rhs.r
				res
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this ^ op_cast
			case op: SInt => 
				val op_cast = Utils.FixedPoint(true, op.getWidth max this.d, this.f, op.asUInt)
				number ^ op_cast
		}
	}

	def &[T] (rawop: T): FixedPoint = { 
		rawop match { 
			case op: FixedPoint => 

				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				val return_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				val res = Wire(new FixedPoint(return_type))
				this.cast(lhs)
				op.cast(rhs)
				res.r := lhs.r & rhs.r
				res
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this & op_cast
			case op: SInt => 
				val op_cast = Utils.FixedPoint(true, op.getWidth max this.d, this.f, op.asUInt)
				this & op_cast
		}
	}

	def |[T] (rawop: T): FixedPoint = { 
		rawop match { 
			case op: FixedPoint => 

				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				val return_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				val res = Wire(new FixedPoint(return_type))
				this.cast(lhs)
				op.cast(rhs)
				res.r := lhs.r | rhs.r
				res
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this | op_cast
			case op: SInt => 
				val op_cast = Utils.FixedPoint(true, op.getWidth max this.d, this.f, op.asUInt)
				this | op_cast
		}
	}

	def <=[T] (rawop: T): Bool = { // TODO: Probably completely wrong for signed fixpts
		rawop match { 
			case op: FixedPoint => 

				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				this.cast(lhs)
				op.cast(rhs)
				if (op.s | s) {lhs.number.asSInt <= rhs.number.asSInt} else {lhs.number <= rhs.number}
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this <= op_cast
			case op: SInt => 
				val op_cast = Utils.FixedPoint(true, op.getWidth max this.d, this.f, op.asUInt)
				number <= op_cast
		}
	}
	
	def >[T] (rawop: T): Bool = { // TODO: Probably completely wrong for signed fixpts
		rawop match { 
			case op: FixedPoint => 
				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				this.cast(lhs)
				op.cast(rhs)
				if (op.s | s) {lhs.number.asSInt > rhs.number.asSInt} else {lhs.number > rhs.number}
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this > op_cast
			case op: SInt => 
				val op_cast = Utils.FixedPoint(true, op.getWidth max this.d, this.f, op.asUInt)
				number > op_cast
		}
	}

	def >=[T] (rawop: T): Bool = { // TODO: Probably completely wrong for signed fixpts
		rawop match { 
			case op: FixedPoint => 
				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				this.cast(lhs)
				op.cast(rhs)
				if (op.s | s) {lhs.number.asSInt >= rhs.number.asSInt} else {lhs.number >= rhs.number}
			case op: UInt => 
				val op_cast = Utils.FixedPoint(this.s, op.getWidth max this.d, this.f, op)
				this >= op_cast
			case op: SInt => 
				val op_cast = Utils.FixedPoint(true, op.getWidth max this.d, this.f, op.asUInt)
				number >= op_cast
		}
	}

	def === [T](r: T): Bool = { // TODO: Probably completely wrong for signed fixpts
		r match {
			case op: FixedPoint =>
				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				this.cast(lhs)
				op.cast(rhs)
				lhs.number === rhs.number
			case op: UInt => 
				// Compute upcasted type and return type
				val upcasted_type = (s, d, f)
				// Get upcasted operators
				val rhs = Utils.FixedPoint(s,d,f, op)
				number === rhs.number
			case op: SInt => 
				val op_cast = Utils.FixedPoint(true, op.getWidth max this.d, this.f, op.asUInt)
				number === op_cast
		}
	}

	def =/= [T](r: T): Bool = { // TODO: Probably completely wrong for signed fixpts
		r match {
			case op: FixedPoint =>
				// Compute upcasted type and return type
				val upcasted_type = (op.s | s, scala.math.max(op.d, d), scala.math.max(op.f, f))
				// Get upcasted operators
				val lhs = Wire(new FixedPoint(upcasted_type))
				val rhs = Wire(new FixedPoint(upcasted_type))
				this.cast(lhs)
				op.cast(rhs)
				lhs.number =/= rhs.number
			case op: UInt => 
				// Compute upcasted type and return type
				val upcasted_type = (s, d, f)
				// Get upcasted operators
				val rhs = Utils.FixedPoint(s,d,f, op)
				number =/= rhs.number
			case op: SInt => 
				val op_cast = Utils.FixedPoint(true, op.getWidth max this.d, this.f, op.asUInt)
				number =/= op_cast
		}
	}

	
	def isNeg (): Bool = {
		Mux(s.B && number(f+d-1), true.B, false.B)
	}

    def unary_-() : FixedPoint = {
    	val neg = Wire(new FixedPoint(s,d,f))
    	neg.number := ~number + 1.U
    	neg
    }
    def unary_~() : FixedPoint = {
    	val neg = Wire(new FixedPoint(s,d,f))
    	neg.number := ~number
    	neg
    }



	// def * (op: FixedPoint): FixedPoint = {
	// 	// Compute upcasted type
	// 	val sign = op.s | s
	// 	val d_prec = op.d + d
	// 	val f_prec = op.f + f
	// 	// Do math on UInts
	// 	val r1 = Wire(new RawBits(d_prec + f_prec))
	// 	this.storeRaw(r1)
	// 	val r2 = Wire(new RawBits(d_prec + f_prec))
	// 	op.storeRaw(r2)
	// 	val rawResult = r1 * r2
	// 	// Store to FixedPoint result
	// 	val result = Wire(new FixedPoint(sign, scala.math.max(op.d, d), scala.math.max(op.f, f)))
	// 	rawResult.storeFix(result)
	// 	result.debug_overflow := Mux(rawResult.raw(0), true.B, false.B)
	// 	result
	// }

    override def cloneType = (new FixedPoint(s,d,f,litVal)).asInstanceOf[this.type] // See chisel3 bug 358

}

// Testing
class FixedPointTester(val s: Boolean, val d: Int, val f: Int) extends Module {
	def this(tuple: (Boolean, Int, Int)) = this(tuple._1, tuple._2, tuple._3)
	val io = IO( new Bundle {
		val num1 = Input(new RawBits(d+f))
		val num2 = Input(new RawBits(d+f))

		val add_result = Output(new RawBits(d+f))
		val prod_result = Output(new RawBits(d+f))
		val sub_result = Output(new RawBits(d+f))
		val quotient_result = Output(new RawBits(d+f))
	})

	val fix1 = Wire(new FixedPoint(s,d,f))
	io.num1.storeFix(fix1)
	val fix2 = Wire(new FixedPoint(s,d,f))
	io.num2.storeFix(fix2)
	val sum = fix1 + fix2
	sum.storeRaw(io.add_result)
	val prod = fix1 *-* fix2
	prod.storeRaw(io.prod_result)
	val sub = fix1 - fix2
	sub.storeRaw(io.sub_result)
	val quotient = fix1 /-/ fix2
	quotient.storeRaw(io.quotient_result)





}



class FloatingPoint(val m: Int, val e: Int) extends Bundle {
	// Overloaded

	def apply(msb:Int, lsb:Int): UInt = this.number(msb,lsb)
	def apply(bit:Int): Bool = this.number(bit)

	// Properties
	val number = UInt((m + e).W)

	def raw():UInt = number
	def r():UInt = number

	def msb():Bool = number(m+e-1)

	def cast(dst: FloatingPoint, rounding: LSBCasting = Truncate, saturating: MSBCasting = Lazy, expect_neg: Bool = false.B, expect_pos: Bool = false.B): Unit = {
		throw new Exception("Float casting not implemented yet")
	}

	def raw_mantissa[T] (): UInt = {
		this.number(m+e-1, e)
	}
	def raw_exp[T] (): UInt = {
		this.number(e, 0)
	}

	// Arithmetic
	override def connect (rawop: Data)(implicit sourceInfo: SourceInfo, connectionCompileOptions: chisel3.core.CompileOptions): Unit = {
		rawop match {
			case op: FixedPoint =>
				number := op.number
			case op: UInt =>
				number := op
			case op: FloatingPoint =>
				number := op.number
		}
	}

	def +[T] (rawop: T, rounding:LSBCasting = Truncate, saturating:MSBCasting = Lazy): FloatingPoint = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(new FloatingPoint(m, e))
				val fma = Module(new MulAddRecFN(m, e))
				fma.io.a := recFNFromFN(m, e, this.r)
				fma.io.b := recFNFromFN(m, e, Utils.getFloatBits(1.0f).S((m+e).W))
				fma.io.c := recFNFromFN(m, e, op.r)
				fma.io.op := 0.U(2.W)
				result.r := fNFromRecFN(m, e, fma.io.out)
				result
		}
	}

	def -[T] (rawop: T, rounding:LSBCasting = Truncate, saturating:MSBCasting = Lazy): FloatingPoint = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(new FloatingPoint(m, e))
				val fma = Module(new MulAddRecFN(m, e))
				fma.io.a := recFNFromFN(m, e, this.r)
				fma.io.b := recFNFromFN(m, e, Utils.getFloatBits(1.0f).S((m+e).W))
				fma.io.c := recFNFromFN(m, e, op.r)
				fma.io.op := 1.U(2.W)
				result.r := fNFromRecFN(m, e, fma.io.out)
				result
		}
	}

	def *-*[T] (rawop: T, rounding:LSBCasting = Truncate, saturating:MSBCasting = Lazy): FloatingPoint = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(new FloatingPoint(m, e))
				val fma = Module(new MulAddRecFN(m, e))
				fma.io.a := recFNFromFN(m, e, this.r)
				fma.io.b := recFNFromFN(m, e, op.r)
				fma.io.c := recFNFromFN(m, e, Utils.getFloatBits(0.0f).S((m+e).W))
				fma.io.op := 0.U(2.W)
				result.r := fNFromRecFN(m, e, fma.io.out)
				result
		}
	}

	def /-/[T] (rawop: T, rounding:LSBCasting = Truncate, saturating:MSBCasting = Lazy): FloatingPoint = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(new FloatingPoint(m, e))
				val fma = Module(new DivSqrtRecFN_small(m, e, 0))
				fma.io.a := recFNFromFN(m, e, this.r)
				fma.io.b := recFNFromFN(m, e, op.r)
				fma.io.inValid := true.B // TODO: What should this be?
				fma.io.sqrtOp := false.B // TODO: What should this be?
				fma.io.roundingMode := 0.U(3.W) // TODO: What should this be?
				fma.io.detectTininess := false.B // TODO: What should this be?
				result.r := fNFromRecFN(m, e, fma.io.out)
				result
		}
	}


	// def %[T] (rawop: T): FloatingPoint = {}

	// def floor[T] (): FloatingPoint = {}

	// def ceil[T] (): FloatingPoint = {}

	def >>[T] (shift: Int, sgnextend: Boolean = false): FloatingPoint = {
		val result = Wire(new FloatingPoint(m, e))
		result.r := this.r >> shift
		result
	}
	// def >>>[T] (shift: Int): FloatingPoint = {this.>>(shift, sgnextend = true)}

	def <<[T] (shift: Int, sgnextend: Boolean = false): FloatingPoint = {
		val result = Wire(new FloatingPoint(m, e))
		result.r := this.r << shift
		result
	}

	def <[T] (rawop: T): Bool = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(Bool())
				val comp = Module(new CompareRecFN(m, e))
				comp.io.a := recFNFromFN(m, e, this.r)
				comp.io.b := recFNFromFN(m, e, op.r)
				comp.io.signaling := false.B // TODO: What is this bit for?
				result := comp.io.lt
				result
		}
	}

	def ===[T] (rawop: T): Bool = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(Bool())
				val comp = Module(new CompareRecFN(m, e))
				comp.io.a := recFNFromFN(m, e, this.r)
				comp.io.b := recFNFromFN(m, e, op.r)
				comp.io.signaling := false.B // TODO: What is this bit for?
				result := comp.io.eq
				result
		}
	}

	def >[T] (rawop: T): Bool = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(Bool())
				val comp = Module(new CompareRecFN(m, e))
				comp.io.a := recFNFromFN(m, e, this.r)
				comp.io.b := recFNFromFN(m, e, op.r)
				comp.io.signaling := false.B // TODO: What is this bit for?
				result := comp.io.gt
				result
		}
	}

	def >=[T] (rawop: T): Bool = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(Bool())
				val comp = Module(new CompareRecFN(m, e))
				comp.io.a := recFNFromFN(m, e, this.r)
				comp.io.b := recFNFromFN(m, e, op.r)
				comp.io.signaling := false.B // TODO: What is this bit for?
				result := comp.io.gt | comp.io.eq
				result
		}
	}

	def <=[T] (rawop: T): Bool = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(Bool())
				val comp = Module(new CompareRecFN(m, e))
				comp.io.a := recFNFromFN(m, e, this.r)
				comp.io.b := recFNFromFN(m, e, op.r)
				comp.io.signaling := false.B // TODO: What is this bit for?
				result := comp.io.lt | comp.io.eq
				result
		}
	}

	def =/=[T] (rawop: T): Bool = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(Bool())
				val comp = Module(new CompareRecFN(m, e))
				comp.io.a := recFNFromFN(m, e, this.r)
				comp.io.b := recFNFromFN(m, e, op.r)
				comp.io.signaling := false.B // TODO: What is this bit for?
				result := ~comp.io.eq
				result
		}
	}

	def ^[T] (rawop: T): FloatingPoint = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(new FloatingPoint(m, e))
				result.r := op.r ^ this.r
				result
		}
	}

	def &[T] (rawop: T): FloatingPoint = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(new FloatingPoint(m, e))
				result.r := op.r & this.r
				result
		}
	}

	def |[T] (rawop: T): FloatingPoint = {
		rawop match {
			case op: FloatingPoint => 
				assert(op.m == m & op.e == e)
				val result = Wire(new FloatingPoint(m, e))
				result.r := op.r | this.r
				result
		}
	}

	// def isNeg (): Bool = {}

	def unary_-() : FloatingPoint = {
		val result = Wire(new FloatingPoint(m, e))
		result.r := util.Cat(~this.msb, this.r.apply(m+e-2,0))
		result
	}


    override def cloneType = (new FloatingPoint(m,e)).asInstanceOf[this.type] // See chisel3 bug 358

}
