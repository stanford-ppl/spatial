// package emul

// case class FixFormat(sign: Boolean, ibits: Int, fbits: Int) {
//   lazy val bits: Int = ibits + fbits
//   lazy val MAX_FRACTIONAL_VALUE: BigInt = (BigInt(1) << (fbits - 1)) - 1

//   lazy val MAX_INTEGRAL_VALUE: BigInt = (if (sign) (BigInt(1) << (ibits-1)) - 1      else (BigInt(1) << ibits) - 1) << fbits
//   lazy val MIN_INTEGRAL_VALUE: BigInt = if (sign) -(BigInt(1) << (ibits-1)) << fbits else BigInt(0)

//   lazy val MAX_VALUE: BigInt = if (sign) (BigInt(1) << (ibits+fbits-1)) - 1 else (BigInt(1) << (ibits+fbits)) - 1
//   lazy val MIN_VALUE: BigInt = if (sign) -(BigInt(1) << (ibits+fbits-1))    else BigInt(0)

//   lazy val MAX_INTEGRAL_VALUE_FP: FixedPoint = FixedPoint.clamped(MAX_INTEGRAL_VALUE, valid=true, this)
//   lazy val MIN_INTEGRAL_VALUE_FP: FixedPoint = FixedPoint.clamped(MIN_INTEGRAL_VALUE, valid=true, this)
//   lazy val MAX_VALUE_FP: FixedPoint = FixedPoint.clamped(MAX_VALUE, valid=true, this)
//   lazy val MIN_VALUE_FP: FixedPoint = FixedPoint.clamped(MIN_VALUE, valid=true, this)
//   lazy val MIN_POSITIVE_VALUE_FP: FixedPoint = FixedPoint.clamped(BigInt(1), valid=true, this)

//   def isExactInt: Boolean  = fbits == 0 && ((sign && ibits <= 32) || ibits <= 31)
//   def isExactLong: Boolean = fbits == 0 && ((sign && ibits <= 64) || ibits <= 63)
// }

// class FixedPoint(val value: BigInt, val valid: Boolean, val fmt: FixFormat) extends Number {
//   def abs: FixedPoint = if (this < 0) -this else this
//   def floor: FixedPoint = {
//     val add = if (this < 0 && (this % FixedPoint(1,fmt) !== 0)) FixedPoint(-1, fmt) else FixedPoint(0,fmt)
//     val clamp = (this.value >> fmt.fbits) << fmt.fbits
//     FixedPoint.clamped(clamp, this.valid, fmt) + add
//   }
//   def ceil: FixedPoint = {
//     val add = if (this > 0 && (this % FixedPoint(1,fmt) !== 0)) FixedPoint(1, fmt) else FixedPoint(0, fmt)
//     val clamp = (this.value >> fmt.fbits) << fmt.fbits
//     FixedPoint.clamped(clamp, this.valid, fmt) + add
//   }

//   // All operations assume that both the left and right hand side have the same fixed point format
//   def unary_-(): FixedPoint = FixedPoint.clamped(-this.value, this.valid, fmt)
//   def unary_~(): FixedPoint = FixedPoint.clamped(~this.value, this.valid, fmt)
//   def +(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value + that.value, this.valid && that.valid, fmt)
//   def -(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value - that.value, this.valid && that.valid, fmt)
//   def *(that: FixedPoint): FixedPoint = FixedPoint.clamped((this.value * that.value) >> fmt.fbits, this.valid && that.valid, fmt)
//   def /(that: FixedPoint): FixedPoint = valueOrX{ FixedPoint.clamped((this.value << fmt.fbits) / that.value, this.valid && that.valid, fmt) }
//   def %(that: FixedPoint): FixedPoint = valueOrX{
//     val result = this.value % that.value
//     val posResult = if (result < 0) result + that.value else result
//     FixedPoint.clamped(posResult, this.valid && that.valid, fmt)
//   }
//   def &(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value & that.value, this.valid && that.valid, fmt)
//   def ^(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value ^ that.value, this.valid && that.valid, fmt)
//   def |(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value | that.value, this.valid && that.valid, fmt)

//   def <(that: FixedPoint): Bool   = Bool(this.value < that.value, this.valid && that.valid)
//   def <=(that: FixedPoint): Bool  = Bool(this.value <= that.value, this.valid && that.valid)
//   def >(that: FixedPoint): Bool   = Bool(this.value > that.value, this.valid && that.valid)
//   def >=(that: FixedPoint): Bool  = Bool(this.value >= that.value, this.valid && that.valid)
//   def !==(that: FixedPoint): Bool = Bool(this.value != that.value, this.valid && that.valid)
//   def ===(that: FixedPoint): Bool = Bool(this.value == that.value, this.valid && that.valid)

//   def <(that: Int): Boolean = (this < FixedPoint(that, this.fmt)).value
//   def <=(that: Int): Boolean = (this <= FixedPoint(that, this.fmt)).value
//   def >(that: Int): Boolean = (this > FixedPoint(that, this.fmt)).value
//   def >=(that: Int): Boolean = (this >= FixedPoint(that, this.fmt)).value
//   def ===(that: Int): Boolean = (this === FixedPoint(that, this.fmt)).value
//   def !==(that: Int): Boolean = (this !== FixedPoint(that, this.fmt)).value

//   def bits: Array[Bool] = Array.tabulate(fmt.bits){i => Bool(value.testBit(i)) }
//   def bitString: String = "0b" + bits.reverse.map{x =>
//     if (x.valid && x.value) "1" else if (x.valid) "0" else "X"
//   }.mkString("")

//   def fancyBitString(grp: Int = 4): String = "0b" + bits.grouped(grp).toSeq.reverse.map{xs =>
//     xs.reverse.map{x => if (x.valid && x.value) "1" else if (x.valid) 0 else "X" }.mkString("")
//   }.mkString("|")



//   def +!(that: FixedPoint): FixedPoint = FixedPoint.saturating(this.value + that.value, this.valid && that.valid, fmt)
//   def -!(that: FixedPoint): FixedPoint = FixedPoint.saturating(this.value - that.value, this.valid && that.valid, fmt)
//   def *!(that: FixedPoint): FixedPoint = FixedPoint.saturating((this.value * that.value) >> fmt.bits, this.valid && that.valid, fmt)
//   def /!(that: FixedPoint): FixedPoint = FixedPoint.saturating((this.value << fmt.bits) / that.value, this.valid && that.valid, fmt)

//   def *&(that: FixedPoint): FixedPoint = {
//     FixedPoint.unbiased(((this.value << 2) * (that.value << 2)) >> fmt.fbits, this.valid && that.valid, fmt)
//   }
//   def /&(that: FixedPoint): FixedPoint = valueOrX {
//     FixedPoint.unbiased((this.value << fmt.fbits+4) / that.value, this.valid && that.valid, fmt)
//   }
//   def *&!(that: FixedPoint): FixedPoint = {
//     FixedPoint.unbiased((this.value << 2) * (that.value << 2) >> fmt.fbits, this.valid && that.valid, fmt, saturate = true)
//   }
//   def /&!(that: FixedPoint): FixedPoint = valueOrX {
//     FixedPoint.unbiased((this.value << fmt.fbits+4) / that.value, this.valid && that.valid, fmt, saturate = true)
//   }

//   def <<(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value << that.toInt, this.valid && that.valid, fmt)
//   def >>(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value >> that.toInt, this.valid && that.valid, fmt)
//   def >>>(that: FixedPoint): FixedPoint = {
//     val shift = that.toInt
//     if (shift >= 0) {
//       // Unsigned right shift isn't supported in BigInt because BigInt technically has infinite precision
//       // But we're only using BigInt to model arbitrary precision data here
//       val zeros = Array.fill(shift)(Bool(false))
//       val bits = this.bits.drop(shift) // Drop that number of lsbs
//       FixedPoint.fromBits(bits ++ zeros, fmt).withValid(this.valid && that.valid)
//     }
//     else FixedPoint.clamped(BigInt(0), this.valid && that.valid, fmt)
//   }


//   def toByte: Byte = (value >> fmt.fbits).toByte
//   def toShort: Short = (value >> fmt.bits).toShort
//   def toInt: Int = (value >> fmt.fbits).toInt
//   def toLong: Long = (value >> fmt.fbits).toLong
//   def toBigInt: BigInt = value >> fmt.fbits

//   def toFloat: Float = this.toBigDecimal.toFloat
//   def toDouble: Double = this.toBigDecimal.toDouble
//   def toBigDecimal: BigDecimal = BigDecimal(value) / BigDecimal(BigInt(1) << fmt.fbits)

//   def toFixedPoint(fmt2: FixFormat): FixedPoint = {
//     if (fmt2.fbits == fmt.fbits) FixedPoint.clamped(value, valid, fmt2)
//     else if (fmt2.fbits > fmt.fbits) FixedPoint.clamped(value << (fmt2.fbits - fmt.fbits), valid, fmt2)
//     else FixedPoint.clamped(value >> (fmt.fbits - fmt2.fbits), valid, fmt2)
//   }
//   def toFloatPoint(fmt: FltFormat): FloatPoint = FloatPoint(this.toBigDecimal, fmt).withValid(valid)

//   def withValid(v: Boolean) = new FixedPoint(value, valid=v, fmt)

//   def valueOrX(x: => FixedPoint): FixedPoint = {
//     try { x } catch { case _: Throwable => FixedPoint.invalid(fmt) }
//   }
//   override def toString: String = if (valid) {
//     if (fmt.fbits > 0) {
//       this.toBigDecimal.bigDecimal.toPlainString
//     }
//     else {
//       value.toString
//     }
//   } else "X"

//   override def hashCode(): Int = (value,valid,fmt).hashCode()

//   override def canEqual(that: Any): Boolean = that match {
//     case _: Byte => true
//     case _: Short => true
//     case _: Int => true
//     case _: Long => true
//     case _: Float => true
//     case _: Double => true
//     case _: FixedPoint => true
//     case _ => false
//   }

//   override def equals(that: Any): Boolean = that match {
//     case that: Byte   => this == FixedPoint(that, fmt) && that == this.toByte
//     case that: Short  => this == FixedPoint(that, fmt) && that == this.toShort
//     case that: Int    => this == FixedPoint(that, fmt) && that == this.toInt
//     case that: Long   => this == FixedPoint(that, fmt) && that == this.toLong
//     case that: Float  => this == FixedPoint(that, fmt) && that == this.toFloat
//     case that: Double => this == FixedPoint(that, fmt) && that == this.toDouble
//     case that: FixedPoint => (if (this.fmt != that.fmt) {
//       if (this.fmt.fbits > that.fmt.fbits) that.toFixedPoint(this.fmt) === this
//       else                                 this.toFixedPoint(that.fmt) === that
//     } else this === that).value
//     case _ => false
//   }
// }

// object FixedPoint {
//   def apply(x: Int): FixedPoint = FixedPoint(x, FixFormat(true,32,0))

//   def apply(x: Byte, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
//   def apply(x: Short, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
//   def apply(x: Int, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
//   def apply(x: Long, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
//   def apply(x: BigInt, fmt: FixFormat): FixedPoint = FixedPoint.clamped(x << fmt.fbits, valid=true, fmt)

//   def apply(x: Float, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigDecimal(x.toDouble) * Math.pow(2,fmt.fbits), valid=true, fmt)
//   def apply(x: Double, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigDecimal(x) * Math.pow(2,fmt.fbits), valid=true, fmt)
//   def apply(x: BigDecimal, fmt: FixFormat): FixedPoint = FixedPoint.clamped(x * Math.pow(2,fmt.fbits), valid=true, fmt)
//   def apply(x: String, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigDecimal(x) * Math.pow(2,fmt.fbits), valid=true, fmt)

//   def invalid(fmt: FixFormat) = new FixedPoint(-1, valid=false, fmt)
//   def clamped(value: BigDecimal, valid: Boolean, fmt: FixFormat): FixedPoint = clamped(value.toBigInt, valid, fmt)

//   def fromBits(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = clamped(bits, valid, fmt)

//   /**
//     * Creates a fixed point value from the given array of bits, wrapping the result upon overflow/underflow
//     * @param bits Big-endian (LSB at index 0, MSB at last index) bits
//     * @param fmt The fixed point format to be used by this number
//     */
//   def fromBits(bits: Array[Bool], fmt: FixFormat): FixedPoint = {
//     // Will be negative?
//     if (fmt.sign && bits.length >= fmt.bits && bits(fmt.bits-1).value) {
//       var x = BigInt(-1)
//       bits.take(fmt.bits).zipWithIndex.foreach{case (bit, i) => if (!bit.value) x = x.clearBit(i) }
//       new FixedPoint(x, bits.forall(_.valid), fmt)
//     }
//     else {
//       var x = BigInt(0)
//       bits.take(fmt.bits).zipWithIndex.foreach{case (bit, i) => if (bit.value) x = x.setBit(i) }
//       new FixedPoint(x, bits.forall(_.valid), fmt)
//     }
//   }

//   def fromByteArray(array: Array[Byte], fmt: FixFormat): FixedPoint = {
//     val bits = array.flatMap{byte =>
//       (0 until 8).map{i => Bool( (byte & (1 << i)) > 0) }
//     }
//     fromBits(bits, fmt)
//   }

//   /**
//     * Create a new fixed point number, wrapping on overflow or underflow
//     * @param bits Value's bits (with both integer and fractional components)
//     * @param valid Defines whether this value is valid or not
//     * @param fmt The fixed point format used by this number
//     */
//   def clamped(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = {
//     val clampedValue = if (fmt.sign && bits.testBit(fmt.bits-1)) {
//       bits | fmt.MIN_VALUE
//     }
//     else bits & fmt.MAX_VALUE
//     new FixedPoint(clampedValue, valid, fmt)
//   }

//   /**
//     * Create a new fixed point number, saturating on overflow or underflow
//     * at the format's max or min values, respectively
//     * @param bits Value's bits (with both integer and fractional components)
//     * @param valid Defines whether this value is valid or not
//     * @param fmt The fixed point format used by this number
//     */
//   def saturating(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = {
//     if (bits < fmt.MIN_VALUE) fmt.MIN_VALUE_FP
//     else if (bits > fmt.MAX_VALUE) fmt.MAX_VALUE_FP
//     else FixedPoint.clamped(bits, valid, fmt)
//   }

//   /**
//     * Create a new fixed point number, rounding to the closest representable number
//     * using unbiased rounding
//     * @param bits Value's bits, with 4 extra fractional bits (beyond normal format representation)
//     * @param valid Defines whether this value is valid or not
//     * @param fmt The fixed point format used by this number
//     * @param saturate When true, also use saturating arithmetic on underflow/overflow
//     */
//   def unbiased(bits: BigInt, valid: Boolean, fmt: FixFormat, saturate: Boolean = false): FixedPoint = {
//     val biased = bits >> 4
//     val remainder = (bits & 0xF).toFloat / 16.0f
//     val rand = scala.util.Random.nextFloat() // TODO: This is actually heavier than it needs to be
//     val add = rand + remainder
//     val value = if (add >= 1 && biased >= 0) biased + 1  else if (add >= 1 && biased < 0) biased - 1 else biased
//     if (!saturate) FixedPoint.clamped(value, valid, fmt)
//     else FixedPoint.saturating(value, valid, fmt)
//   }

//   /**
//     * Generate a pseudo-random fixed point number, uniformly distributed across the entire representation's range
//     * @param fmt The format for the fixed point number being generated
//     */
//   def random(fmt: FixFormat): FixedPoint = {
//     val bits = Array.tabulate(fmt.bits){_ => Bool(scala.util.Random.nextBoolean()) }
//     FixedPoint.fromBits(bits, fmt)
//   }

//   /**
//     * Generate a pseudo-random fixed point number, uniformly distributed between [0, max)
//     * @param max The maximum value of the range, non-inclusive
//     * @param fmt The format for the max and the fixed point number being generated
//     */
//   def random(max: FixedPoint, fmt: FixFormat): FixedPoint = {
//     val rand = random(fmt)
//     rand % max
//   }

//   implicit object FixedPointIsIntegral extends Integral[FixedPoint] {
//     def quot(x: FixedPoint, y: FixedPoint): FixedPoint = x / y
//     def rem(x: FixedPoint, y: FixedPoint): FixedPoint = x % y
//     def compare(x: FixedPoint, y: FixedPoint): Int = if ((x < y).value) -1 else if ((x > y).value) 1 else 0
//     def plus(x : FixedPoint, y : FixedPoint) : FixedPoint = x + y
//     def minus(x : FixedPoint, y : FixedPoint) : FixedPoint = x - y
//     def times(x : FixedPoint, y : FixedPoint) : FixedPoint = x * y
//     def negate(x : FixedPoint) : FixedPoint = -x
//     def fromInt(x : scala.Int) : FixedPoint = FixedPoint(x)
//     def toInt(x : FixedPoint) : scala.Int = x.toInt
//     def toLong(x : FixedPoint) : scala.Long = x.toLong
//     def toFloat(x : FixedPoint) : scala.Float = x.toFloat
//     def toDouble(x : FixedPoint) : scala.Double = x.toDouble
//   }
// }

