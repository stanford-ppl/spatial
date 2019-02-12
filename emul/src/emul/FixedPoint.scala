package emul

class FixedPoint(val value: BigInt, val valid: Boolean, val fmt: FixFormat) extends Number {
  // All operations assume that both the left and right hand side have the same fixed point format
  def unary_-(): FixedPoint = FixedPoint.clamped(-this.value, this.valid, fmt)
  def unary_~(): FixedPoint = FixedPoint.clamped(~this.value, this.valid, fmt)

  def +(that: Int): FixedPoint = this + FixedPoint.clamped(BigDecimal(that), true, this.fmt)
  def -(that: Int): FixedPoint = this - FixedPoint.clamped(BigDecimal(that), true, this.fmt)
  def *(that: Int): FixedPoint = this * FixedPoint.clamped(BigDecimal(that), true, this.fmt)
  def /(that: Int): FixedPoint = this / FixedPoint.clamped(BigDecimal(that), true, this.fmt)
  def %(that: Int): FixedPoint = this % FixedPoint.clamped(BigDecimal(that), true, this.fmt)

  def +(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value + that.value, this.valid && that.valid, fmt)
  def -(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value - that.value, this.valid && that.valid, fmt)
  def *(that: FixedPoint): FixedPoint = FixedPoint.clamped((this.value * that.value) >> fmt.fbits, this.valid && that.valid, fmt)
  def /(that: FixedPoint): FixedPoint = valueOrX{ FixedPoint.clamped((this.value << fmt.fbits) / that.value, this.valid && that.valid, fmt) }
  def %(that: FixedPoint): FixedPoint = valueOrX{
    val result = this.value % that.value
    val posResult = if (result < 0) result + that.value else result
    FixedPoint.clamped(posResult, this.valid && that.valid, fmt)
  }
  def &(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value & that.value, this.valid && that.valid, fmt)
  def ^(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value ^ that.value, this.valid && that.valid, fmt)
  def |(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value | that.value, this.valid && that.valid, fmt)

  def <(that: FixedPoint): Bool   = Bool(this.value < that.value, this.valid && that.valid)
  def <=(that: FixedPoint): Bool  = Bool(this.value <= that.value, this.valid && that.valid)
  def >(that: FixedPoint): Bool   = Bool(this.value > that.value, this.valid && that.valid)
  def >=(that: FixedPoint): Bool  = Bool(this.value >= that.value, this.valid && that.valid)
  def !==(that: FixedPoint): Bool = Bool(this.value != that.value, this.valid && that.valid)
  def ===(that: FixedPoint): Bool = Bool(this.value == that.value, this.valid && that.valid)

  def <(that: Int): Boolean = (this < FixedPoint(that, this.fmt)).value
  def <=(that: Int): Boolean = (this <= FixedPoint(that, this.fmt)).value
  def >(that: Int): Boolean = (this > FixedPoint(that, this.fmt)).value
  def >=(that: Int): Boolean = (this >= FixedPoint(that, this.fmt)).value
  def ===(that: Int): Boolean = (this === FixedPoint(that, this.fmt)).value
  def !==(that: Int): Boolean = (this !== FixedPoint(that, this.fmt)).value

  def ===(that: Long): Boolean = (this === FixedPoint(that, this.fmt)).value
  def ===(that: Float): Boolean = (this === FixedPoint(that, this.fmt)).value
  def ===(that: Double): Boolean = (this === FixedPoint(that, this.fmt)).value

  def bits: Array[Bool] = Array.tabulate(fmt.bits){i => Bool(value.testBit(i)) }


  def +!(that: FixedPoint): FixedPoint = FixedPoint.saturating(this.value + that.value, this.valid && that.valid, fmt)
  def -!(that: FixedPoint): FixedPoint = FixedPoint.saturating(this.value - that.value, this.valid && that.valid, fmt)
  def *!(that: FixedPoint): FixedPoint = FixedPoint.saturating((this.value * that.value) >> fmt.fbits, this.valid && that.valid, fmt)
  def /!(that: FixedPoint): FixedPoint = FixedPoint.saturating((this.value << fmt.fbits) / that.value, this.valid && that.valid, fmt)

  def *&(that: FixedPoint): FixedPoint = {
    FixedPoint.unbiased(((this.value << 2) * (that.value << 2)) >> fmt.fbits, this.valid && that.valid, fmt)
  }
  def /&(that: FixedPoint): FixedPoint = valueOrX {
    FixedPoint.unbiased((this.value << fmt.fbits+4) / that.value, this.valid && that.valid, fmt)
  }
  def *&!(that: FixedPoint): FixedPoint = {
    FixedPoint.unbiased((this.value << 2) * (that.value << 2) >> fmt.fbits, this.valid && that.valid, fmt, saturate = true)
  }
  def /&!(that: FixedPoint): FixedPoint = valueOrX {
    FixedPoint.unbiased((this.value << fmt.fbits+4) / that.value, this.valid && that.valid, fmt, saturate = true)
  }

  def <<(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value << that.toInt, this.valid && that.valid, fmt)
  def >>(that: FixedPoint): FixedPoint = FixedPoint.clamped(this.value >> that.toInt, this.valid && that.valid, fmt)
  def >>>(that: FixedPoint): FixedPoint = {
    val shift = that.toInt
    if (shift >= 0) {
      // Unsigned right shift isn't supported in BigInt because BigInt technically has infinite precision
      // But we're only using BigInt to model arbitrary precision data here
      val zeros = Array.fill(shift)(Bool(false))
      val bits = this.bits.drop(shift) // Drop that number of lsbs
      FixedPoint.fromBits(bits ++ zeros, fmt).withValid(this.valid && that.valid)
    }
    else FixedPoint.clamped(BigInt(0), this.valid && that.valid, fmt)
  }


  def toByte: Byte = (value >> fmt.fbits).toByte
  def toShort: Short = (value >> fmt.bits).toShort
  def toInt: Int = (value >> fmt.fbits).toInt
  def toLong: Long = (value >> fmt.fbits).toLong
  def toBigInt: BigInt = value >> fmt.fbits

  def toFloat: Float = this.toBigDecimal.toFloat
  def toDouble: Double = this.toBigDecimal.toDouble
  def toBigDecimal: BigDecimal = BigDecimal(value) / BigDecimal(BigInt(1) << fmt.fbits)

  def toFixedPoint(fmt2: FixFormat): FixedPoint = {
    if (fmt2.fbits == fmt.fbits) FixedPoint.clamped(value, valid, fmt2)
    else if (fmt2.fbits > fmt.fbits) FixedPoint.clamped(value << (fmt2.fbits - fmt.fbits), valid, fmt2)
    else FixedPoint.clamped(value >> (fmt.fbits - fmt2.fbits), valid, fmt2)
  }
  def toFloatPoint(fmt: FltFormat): FloatPoint = FloatPoint(this.toBigDecimal, fmt).withValid(valid)

  def withValid(v: Boolean) = new FixedPoint(value, valid=v, fmt)

  def until(end: FixedPoint) = FixedPointRange(this, end, FixedPoint(1, fmt), isInclusive = false)
  def to(end: FixedPoint) = FixedPointRange(this, end, FixedPoint(1, fmt), isInclusive = true)

  def valueOrX(x: => FixedPoint): FixedPoint = {
    try { x } catch { case _: Throwable => FixedPoint.invalid(fmt) }
  }
  override def toString: String = if (valid) {
    if (fmt.fbits > 0) {
      this.toBigDecimal.bigDecimal.toPlainString
    }
    else {
      value.toString
    }
  } else "X"

  def is1s: Boolean = this.bits.forall(_.value)
  def isPow2: Boolean = (this & (this - 1)) === 0 && this.isExactInt

  override def hashCode(): Int = (value,valid,fmt).hashCode()

  override def canEqual(that: Any): Boolean = that match {
    case _: FixedPoint => true
    case _ => super.canEqual(that)
  }

  override def equals(that: Any): Boolean = that match {
    case that: Byte   => this == FixedPoint(that, fmt) && that == this.toByte
    case that: Short  => this == FixedPoint(that, fmt) && that == this.toShort
    case that: Int    => this == FixedPoint(that, fmt) && that == this.toInt
    case that: Long   => this == FixedPoint(that, fmt) && that == this.toLong
    case that: Float  => this == FixedPoint(that, fmt) && that == this.toFloat
    case that: Double => this == FixedPoint(that, fmt) && that == this.toDouble
    case that: FixedPoint if this.fmt != that.fmt =>
      val fmt = this.fmt combine that.fmt
      (this.toFixedPoint(fmt) === that.toFixedPoint(fmt)).value
    case that: FixedPoint => (this === that).value
    case that: String => this.toString == that  // TODO[4]: Always correct to compare based on string?
    case _ =>
      println(s"No comparison available to ${that.getClass}")
      false
  }
}

object FixedPoint {
  def fromChar(x: Char): FixedPoint = FixedPoint(x, FixFormat(false,8,0))
  def fromByte(x: Byte): FixedPoint = FixedPoint(x, FixFormat(true,8,0))
  def fromShort(x: Short): FixedPoint = FixedPoint(x, FixFormat(true,16,0))
  def fromInt(x: Int): FixedPoint = FixedPoint(x, FixFormat(true,32,0))
  def fromLong(x: Long): FixedPoint = FixedPoint(x, FixFormat(true,64,0))

  def apply(x: Boolean, fmt: FixFormat): FixedPoint = FixedPoint(if (x) 1 else 0,fmt)
  def apply(x: Byte, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
  def apply(x: Short, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
  def apply(x: Int, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
  def apply(x: Long, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
  def apply(x: BigInt, fmt: FixFormat): FixedPoint = FixedPoint.clamped(x << fmt.fbits, valid=true, fmt)

  def apply(x: Float, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigDecimal(x.toDouble) * Math.pow(2,fmt.fbits), valid=true, fmt)
  def apply(x: Double, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigDecimal(x) * Math.pow(2,fmt.fbits), valid=true, fmt)
  def apply(x: BigDecimal, fmt: FixFormat): FixedPoint = FixedPoint.clamped(x * Math.pow(2,fmt.fbits), valid=true, fmt)
  def apply(x: String, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigDecimal(x) * Math.pow(2,fmt.fbits), valid=true, fmt)

  def invalid(fmt: FixFormat) = new FixedPoint(-1, valid=false, fmt)
  def clamped(value: => BigDecimal, valid: Boolean, fmt: FixFormat): FixedPoint = {
    try {
      clamped(value.toBigInt, valid, fmt)
    }
    catch {case _:NumberFormatException => FixedPoint.invalid(fmt) }
  }

  def fromBits(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = clamped(bits, valid, fmt)

  /**
    * Creates a fixed point value from the given array of bits, wrapping the result upon overflow/underflow
    * @param bits Big-endian (LSB at index 0, MSB at last index) bits
    * @param fmt The fixed point format to be used by this number
    */
  def fromBits(bits: Array[Bool], fmt: FixFormat): FixedPoint = {
    // Will be negative?
    if (fmt.sign && bits.length >= fmt.bits && bits(fmt.bits-1).value) {
      var x = BigInt(-1)
      bits.take(fmt.bits).zipWithIndex.foreach{case (bit, i) => if (!bit.value) x = x.clearBit(i) }
      new FixedPoint(x, bits.forall{bit => bit.valid}, fmt)
    }
    else {
      var x = BigInt(0)
      bits.take(fmt.bits).zipWithIndex.foreach{case (bit, i) => if (bit.value) x = x.setBit(i) }
      new FixedPoint(x, bits.forall{bit => bit.valid}, fmt)
    }
  }

  def fromByteArray(array: Array[Byte], fmt: FixFormat): FixedPoint = {
    val bits = array.flatMap{byte =>
      (0 until 8).map{i => Bool( (byte & (1 << i)) > 0) }
    }
    fromBits(bits, fmt)
  }

  /**
    * Create a new fixed point number, wrapping on overflow or underflow
    * @param bits Value's bits (with both integer and fractional components)
    * @param valid Defines whether this value is valid or not
    * @param fmt The fixed point format used by this number
    */
  def clamped(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = {
    val clampedValue = if (fmt.sign && bits.testBit(fmt.bits-1)) {
      bits | fmt.MIN_VALUE
    }
    else bits & fmt.MAX_VALUE
    new FixedPoint(clampedValue, valid, fmt)
  }

  /**
    * Create a new fixed point number, saturating on overflow or underflow
    * at the format's max or min values, respectively
    * @param bits Value's bits (with both integer and fractional components)
    * @param valid Defines whether this value is valid or not
    * @param fmt The fixed point format used by this number
    */
  def saturating(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = {
    if (bits < fmt.MIN_VALUE) fmt.MIN_VALUE_FP
    else if (bits > fmt.MAX_VALUE) fmt.MAX_VALUE_FP
    else FixedPoint.clamped(bits, valid, fmt)
  }

  /**
    * Create a new fixed point number, rounding to the closest representable number
    * using unbiased rounding
    * @param bits Value's bits, with 4 extra fractional bits (beyond normal format representation)
    * @param valid Defines whether this value is valid or not
    * @param fmt The fixed point format used by this number
    * @param saturate When true, also use saturating arithmetic on underflow/overflow
    */
  def unbiased(bits: BigInt, valid: Boolean, fmt: FixFormat, saturate: Boolean = false): FixedPoint = {
    val biased = bits >> 4
    val remainder = (bits & 0xF).toFloat / 16.0f
    // TODO[5]: RNG here for unbiased rounding is actually heavier than it needs to be
    val rand = scala.util.Random.nextFloat()
    val add = rand + remainder
    val value = if (add >= 1 && biased >= 0) biased + 1  else if (add >= 1 && biased < 0) biased - 1 else biased
    if (!saturate) FixedPoint.clamped(value, valid, fmt)
    else FixedPoint.saturating(value, valid, fmt)
  }

  /**
    * Generate a pseudo-random fixed point number, uniformly distributed across the entire representation's range
    * @param fmt The format for the fixed point number being generated
    */
  def random(fmt: FixFormat): FixedPoint = {
    val bits = Array.tabulate(fmt.bits){_ => Bool(scala.util.Random.nextBoolean()) }
    FixedPoint.fromBits(bits, fmt)
  }

  /**
    * Generate a pseudo-random fixed point number, uniformly distributed between [0, max)
    * @param max The maximum value of the range, non-inclusive
    * @param fmt The format for the max and the fixed point number being generated
    */
  def random(max: FixedPoint, fmt: FixFormat): FixedPoint = {
    val rand = random(fmt)
    rand % max
  }

  implicit object FixedPointIsIntegral extends Integral[FixedPoint] {
    def quot(x: FixedPoint, y: FixedPoint): FixedPoint = x / y
    def rem(x: FixedPoint, y: FixedPoint): FixedPoint = x % y
    def compare(x: FixedPoint, y: FixedPoint): Int = if ((x < y).value) -1 else if ((x > y).value) 1 else 0
    def plus(x : FixedPoint, y : FixedPoint) : FixedPoint = x + y
    def minus(x : FixedPoint, y : FixedPoint) : FixedPoint = x - y
    def times(x : FixedPoint, y : FixedPoint) : FixedPoint = x * y
    def negate(x : FixedPoint) : FixedPoint = -x
    def fromInt(x : scala.Int) : FixedPoint = FixedPoint.fromInt(x)
    def toInt(x : FixedPoint) : scala.Int = x.toInt
    def toLong(x : FixedPoint) : scala.Long = x.toLong
    def toFloat(x : FixedPoint) : scala.Float = x.toFloat
    def toDouble(x : FixedPoint) : scala.Double = x.toDouble
    def parseString(str: String): Option[FixedPoint] = None // Undefined for general type
  }
}

