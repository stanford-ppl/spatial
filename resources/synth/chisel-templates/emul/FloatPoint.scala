// package emul

// case class FltFormat(sbits: Int, ebits: Int) {
//   lazy val bits: Int = sbits + ebits + 1
//   lazy val bias: BigInt = BigInt(2).pow(ebits - 1) - 1
//   lazy val MIN_E: BigInt = -bias + 1      // Represented as exponent of 1
//   lazy val MAX_E: BigInt = bias
//   lazy val SUB_E: BigInt = MIN_E - sbits  // Exponent is all 0s

//   lazy val MAX_VALUE_FP: FloatPoint = {
//     val x = Array.tabulate(bits){i => if (i == bits-1 || i == bits-1-ebits) Bool(false) else Bool(true) }
//     //println(x.toStr)
//     FloatPoint.fromBits(x, this)
//   }
//   lazy val MIN_VALUE_FP: FloatPoint = -MAX_VALUE_FP

//   lazy val MIN_POSITIVE_VALUE: FloatPoint = {
//     val exp = BigInt(0)
//     val man = BigInt(1)
//     val value = FloatPoint.convertBackToValue(Right(false,man,exp), this)
//     new FloatPoint(value, true, this)
//   }
// }

// protected sealed abstract class FloatValue {
//   def negative: Boolean
//   def bits(fmt: FltFormat): Array[Bool]
//   def unary_-(): FloatValue = this match {
//     case NaN => NaN
//     case i: Inf => Inf(!i.negative)
//     case z: Zero => Zero(!z.negative)
//     case v: Value => Value(-v.value)
//   }
//   def +(that: FloatValue): FloatValue = (this, that) match {
//     case (_, NaN) => NaN
//     case (NaN, _) => NaN
//     case (a:Inf, b:Inf) => if (a.negative != b.negative) NaN else a
//     case (_:Inf, _) => this
//     case (_, _:Inf) => that
//     case (_:Zero, _:Zero) => Zero(negative=false)
//     case (_:Zero, _) => that
//     case (_, _:Zero) => this
//     case (a:Value, b:Value) => Value(a.value + b.value)
//   }
//   def -(that: FloatValue): FloatValue = this + (-that)
//   def *(that: FloatValue): FloatValue = (this, that) match {
//     case (_, NaN)         => NaN
//     case (NaN, _)         => NaN
//     case (a:Inf, b:Inf)   => Inf(a.negative != b.negative)
//     case (_:Inf, _:Zero)  => NaN
//     case (_:Zero, _:Inf)  => NaN
//     case (a:Inf, b:Value) => Inf(a.negative != b.negative)
//     case (a:Value, b:Inf) => Inf(a.negative != b.negative)
//     case (a:Zero,b:Zero)  => Zero(a.negative != b.negative)
//     case (a:Zero,b:Value) => Zero(a.negative != b.negative)
//     case (a:Value,b:Zero) => Zero(a.negative != b.negative)
//     case (a:Value,b:Value) => Value(a.value * b.value)
//   }
//   def /(that: FloatValue): FloatValue = (this, that) match {
//     case (_, NaN) => NaN
//     case (NaN, _) => NaN
//     case (_:Inf,_:Inf) => NaN
//     case (_:Zero,_:Zero) => NaN
//     case (a:Inf, b:Value) => Inf(a.negative != b.negative)
//     case (a, b:Zero) => Inf(a.negative != b.negative)
//     case (a, b:Inf) => Zero(a.negative != b.negative)
//     case (a:Zero, b:Value) => Zero(a.negative != b.negative)
//     case (a:Value,b:Value) => Value(a.value / b.value)
//   }
//   def %(that: FloatValue): FloatValue = (this, that) match {
//     case (_, NaN) => NaN
//     case (NaN, _) => NaN
//     case (_:Inf, _) => NaN
//     case (a, _:Inf) => a
//     case (_, _:Zero) => NaN
//     case (_:Zero, _) => this
//     case (a:Value, b:Value) => Value(a.value % b.value)
//   }
//   def <(that: FloatValue): Boolean = (this, that) match {
//     case (_, NaN) => false
//     case (NaN, _) => false
//     case (a:Inf, b:Inf) => a.negative && !b.negative
//     case (_, b:Inf) => !b.negative
//     case (_:Inf, b) => b.negative
//     case (_:Zero, _:Zero) => false
//     case (a, _:Zero) => a.negative
//     case (_:Zero, b) => !b.negative
//     case (a:Value,b:Value) => a.value < b.value
//   }
//   def <=(that: FloatValue): Boolean = this < that || this === that
//   def >(that: FloatValue): Boolean = that < this
//   def >=(that: FloatValue): Boolean = that <= this
//   def ===(that: FloatValue): Boolean = (this, that) match {
//     case (_, NaN) => false
//     case (NaN, _) => false
//     case (a:Inf, b:Inf) => a.negative == b.negative
//     case (_, _:Inf) => false
//     case (_:Inf, _) => false
//     case (_:Zero,_:Zero) => true
//     case (_:Zero, _) => false
//     case (_, _:Zero) => false
//     case (a:Value,b:Value) => a.value == b.value
//   }
//   def !==(that: FloatValue): Boolean = !(this === that)
// }
// protected case object NaN extends FloatValue {
//   val negative = false
//   override def toString: String = "NaN"

//   def bits(fmt: FltFormat): Array[Bool] = {
//     Array.tabulate(fmt.sbits){i => Bool(i == fmt.sbits-1) } ++
//       Array.tabulate(fmt.ebits){_ => Bool(true) } ++
//       Array(Bool(false))
//   }
// }
// protected case class Inf(negative: Boolean) extends FloatValue {
//   override def toString: String = if (negative) "-Inf" else "Inf"

//   def bits(fmt: FltFormat): Array[Bool] = {
//     Array.tabulate(fmt.sbits){_ => Bool(false) } ++
//       Array.tabulate(fmt.ebits){_ => Bool(true) } ++
//       Array(Bool(negative))
//   }
// }
// protected case class Zero(negative: Boolean) extends FloatValue {
//   override def toString: String = if (negative) "-0.0" else "0.0"

//   def bits(fmt: FltFormat): Array[Bool] = {
//     Array.tabulate(fmt.sbits){_ => Bool(false) } ++
//       Array.tabulate(fmt.ebits){_ => Bool(false) } ++
//       Array(Bool(negative))
//   }
// }
// protected case class Value(value: BigDecimal) extends FloatValue {
//   def negative: Boolean = value < 0
//   // Default cutoff for formatting for scala Double is 1E7, so using the same here
//   override def toString: String = {
//     if (value.abs >= 1E7 || value.abs < 1E-7) value.bigDecimal.toEngineeringString
//     else value.bigDecimal.toPlainString
//   }

//   def bits(fmt: FltFormat): Array[Bool] = FloatPoint.clamp(value, fmt) match {
//     case Left(fv) => fv.bits(fmt)
//     case Right((s,mantissa,exp)) =>
//       Array.tabulate(fmt.sbits){i => Bool(mantissa.testBit(i)) } ++
//         Array.tabulate(fmt.ebits){i => Bool(exp.testBit(i)) } ++
//         Array(Bool(s))
//   }
// }
// object FloatValue {
//   def apply(x: Float): FloatValue = {
//     if (x.isNaN) NaN
//     else if (x.isInfinity) Inf(negative = x < 0)
//     else if (x == 0.0f) Zero(negative = 1 / x < 0)
//     else Value(BigDecimal(x.toDouble))
//   }
//   def apply(x: Double): FloatValue = {
//     if (x.isNaN) NaN
//     else if (x.isInfinity) Inf(negative = x < 0)
//     else if (x == 0.0) Zero(negative = 1 / x < 0)
//     else Value(BigDecimal(x))
//   }
//   def apply(x: BigDecimal): FloatValue = Value(x)
// }

// class FloatPoint(val value: FloatValue, val valid: Boolean, val fmt: FltFormat) extends Number {
//   def abs: FloatPoint = if ((this < FloatPoint(0, fmt)).value) -this else this
//   def floor: FloatPoint = Number.floor(this)
//   def ceil: FloatPoint = Number.ceil(this)

//   def unary_-(): FloatPoint = FloatPoint.clamped(-this.value, this.valid, fmt)

//   // All operations assume that both the left and right hand side have the same fixed point format
//   def +(that: FloatPoint): FloatPoint = FloatPoint.clamped(this.value + that.value, this.valid && that.valid, fmt)
//   def -(that: FloatPoint): FloatPoint = FloatPoint.clamped(this.value - that.value, this.valid && that.valid, fmt)
//   def *(that: FloatPoint): FloatPoint = FloatPoint.clamped(this.value * that.value, this.valid && that.valid, fmt)
//   def /(that: FloatPoint): FloatPoint = FloatPoint.clamped(this.value / that.value, this.valid && that.valid, fmt)
//   def %(that: FloatPoint): FloatPoint = {
//     val result = this.value % that.value
//     val posResult = result match {
//       case Value(v) => if (v < 0) Value(v) + that.value else result
//       case _ => result
//     }
//     FloatPoint.clamped(posResult, this.valid && that.valid, fmt)
//   }

//   def <(that: FloatPoint): Bool   = Bool(this.value < that.value, this.valid && that.valid)
//   def <=(that: FloatPoint): Bool  = Bool(this.value <= that.value, this.valid && that.valid)
//   def >(that: FloatPoint): Bool   = Bool(this.value > that.value, this.valid && that.valid)
//   def >=(that: FloatPoint): Bool  = Bool(this.value >= that.value, this.valid && that.valid)
//   def !==(that: FloatPoint): Bool = Bool(this.value != that.value, this.valid && that.valid)
//   def ===(that: FloatPoint): Bool = Bool(this.value == that.value, this.valid && that.valid)

//   def ===(that: Int): Boolean = this.value == FloatPoint(that,fmt).value

//   def toBigDecimal: BigDecimal = value match {
//     case Zero(_) => BigDecimal(0)
//     case Value(v) => v
//     case v => throw new Exception(s"Cannot convert $v to BigDecimal")
//   }
//   def toDouble: Double = value match {
//     case NaN       => Double.NaN
//     case Inf(neg)  => if (neg) Double.NegativeInfinity else Double.PositiveInfinity
//     case Zero(neg) => if (neg) -0.0 else 0.0
//     case Value(v)  => v.toDouble
//   }
//   def toFloat: Float = value match {
//     case NaN       => Float.NaN
//     case Inf(neg)  => if (neg) Float.NegativeInfinity else Float.PositiveInfinity
//     case Zero(neg) => if (neg) -0.0f else 0.0f
//     case Value(v)  => v.toFloat
//   }
//   def toFixedPoint(fmt: FixFormat): FixedPoint = (value match {
//     case NaN      => FixedPoint(0, fmt)  // Weird, but this is the behavior that Float and Double have in Scala
//     case Inf(neg) => if (neg) fmt.MIN_VALUE_FP else fmt.MAX_VALUE_FP
//     case Zero(_)  => FixedPoint(0, fmt)
//     case Value(v) => FixedPoint(v, fmt)
//   }).withValid(valid)

//   def toByte: Byte   = this.toFixedPoint(FixFormat(true,8,0)).toByte
//   def toShort: Short = this.toFixedPoint(FixFormat(true,16,0)).toShort
//   def toInt: Int     = this.toFixedPoint(FixFormat(true,32,0)).toInt
//   def toLong: Long   = this.toFixedPoint(FixFormat(true,64,0)).toLong

//   def toFloatPoint(fmt: FltFormat): FloatPoint = FloatPoint.clamped(value, valid, fmt)

//   def rawBitsAsInt: Int = {
//     println(s"Converting $this to bits: ${fancyBitString()}")
//     FixedPoint.fromBits(bits, FixFormat(true,32,0)).toInt
//   }

//   def bits: Array[Bool] = value.bits(fmt)
//   def bitString: String = "0b" + bits.reverse.map{x =>
//     if (x.valid && x.value) "1" else if (x.valid) "0" else "X"
//   }.mkString("")

//   def fancyBitString(grp: Int = 4): String = "0b" + bits.grouped(grp).toSeq.reverse.map{xs =>
//     xs.reverse.map{x => if (x.valid && x.value) "1" else if (x.valid) 0 else "X" }.mkString("")
//   }.mkString("|")

//   def withValid(v: Boolean): FloatPoint = new FloatPoint(value, valid=v, fmt)

//   def isNaN: Boolean = value == NaN
//   def isPositiveInfinity: Boolean = value == Inf(false)
//   def isNegativeInfinity: Boolean = value == Inf(true)
//   def isPosZero: Boolean = value == Zero(true)
//   def isNegZero: Boolean = value == Zero(false)
//   def isSubnormal: Boolean = value match {
//     case Value(v) => FloatPoint.clamp(v, fmt) match {
//       case Right((_,_,e)) => e == 0
//       case _ => false
//     }
//     case _ => false
//   }

//   override def hashCode(): Int = (value,valid,fmt).hashCode()

//   override def canEqual(that: Any): Boolean = that match {
//     case _: Byte => true
//     case _: Short => true
//     case _: Int => true
//     case _: Long => true
//     case _: Float => true
//     case _: Double => true
//     case _: FloatPoint => true
//     case _ => false
//   }

//   override def equals(that: Any): Boolean = that match {
//     case that: Byte   => this == FloatPoint(that, fmt) && that == this.toByte
//     case that: Short  => this == FloatPoint(that, fmt) && that == this.toShort
//     case that: Int    => this == FloatPoint(that, fmt) && that == this.toInt
//     case that: Long   => this == FloatPoint(that, fmt) && that == this.toLong
//     case that: Float  => this == FloatPoint(that, fmt) && that == this.toFloat
//     case that: Double => this == FloatPoint(that, fmt) && that == this.toDouble
//     case that: FloatPoint if this.fmt == that.fmt => (this === that).value
//     case _ => false
//   }

//   override def toString: String = if (valid) value.toString else "X"
// }

// object FloatPoint {
//   def apply(x: Byte, fmt: FltFormat): FloatPoint = FloatPoint.clamped(Value(BigDecimal(x)), valid=true, fmt)
//   def apply(x: Short, fmt: FltFormat): FloatPoint = FloatPoint.clamped(Value(BigDecimal(x)), valid=true, fmt)
//   def apply(x: Int, fmt: FltFormat): FloatPoint = FloatPoint.clamped(Value(BigDecimal(x)), valid=true, fmt)
//   def apply(x: Long, fmt: FltFormat): FloatPoint = FloatPoint.clamped(Value(BigDecimal(x)), valid=true, fmt)
//   def apply(x: BigInt, fmt: FltFormat): FloatPoint = FloatPoint.clamped(Value(BigDecimal(x)), valid=true, fmt)

//   def apply(x: Float, fmt: FltFormat): FloatPoint = FloatPoint.clamped(FloatValue(x), valid=true, fmt)
//   def apply(x: Double, fmt: FltFormat): FloatPoint = FloatPoint.clamped(FloatValue(x), valid=true, fmt)
//   def apply(x: BigDecimal, fmt: FltFormat): FloatPoint = FloatPoint.clamped(FloatValue(x), valid=true, fmt)
//   def apply(x: String, fmt: FltFormat): FloatPoint = x match {
//     case "NaN"  => FloatPoint.clamped(NaN, valid=true, fmt)
//     case "-Inf" => FloatPoint.clamped(Inf(negative=true), valid=true, fmt)
//     case "Inf"  => FloatPoint.clamped(Inf(negative=false), valid=true, fmt)
//     case "-0.0" => FloatPoint.clamped(Zero(negative=true), valid=true, fmt)
//     case "0.0"  => FloatPoint.clamped(Zero(negative=false), valid=true, fmt)
//     case _      => FloatPoint.clamped(Value(BigDecimal(x)), valid=true, fmt)
//   }

//   def invalid(fmt: FltFormat): FloatPoint = new FloatPoint(NaN, false, fmt)

//   /**
//     * Stolen from https://stackoverflow.com/questions/6827516/logarithm-for-biginteger/7982137#7982137
//     */
//   private val LOG2 = Math.log(2.0)
//   def log2BigInt(value: BigInt): Double = {
//     val blex = value.bitLength - 1022 // any value in 60 .. 1023 is ok
//     val shifted = if (blex > 0) value >> blex else value
//     val res = Math.log(shifted.doubleValue) / LOG2
//     if (blex > 0) res + blex else res
//   }

//   def log2BigDecimal(value: BigDecimal): Double = {
//     if (value.abs >= 1) {
//       val fracPart = value % 1
//       val intPart = if (value < 0) value + fracPart else value - fracPart
//       log2BigInt(intPart.toBigInt)
//     }
//     else {
//       -log2BigInt((1/value).toBigInt)
//     }
//   }

//   implicit class BigIntOps(x: BigInt) {
//     def bits(n: Int): Array[Boolean] = Array.tabulate(n){i => x.testBit(i) }
//     def fancyBits(n: Int): String = bits(n).grouped(4).toSeq.reverse.map{xs =>
//       xs.reverse.map{x => if (x) "1" else 0 }.mkString("")
//     }.mkString("|")
//   }

//   def clamp(value: BigDecimal, fmt: FltFormat): Either[FloatValue, (Boolean,BigInt,BigInt)] = {

//     if (value == 0) {
//       Left(Zero(negative = false))
//     }
//     else {
//       var y = Math.floor(log2BigDecimal(value.abs)).toInt
//       var x = value.abs / BigDecimal(2).pow(y)
//       //      println("CLAMPING: ")
//       //      println(s"exp: $y [min: ${fmt.MIN_E}, max: ${fmt.MAX_E}, sub: ${fmt.SUB_E}]")
//       //      println(s"man: $x")
//       //      println("y < fmt.SUB_E: " + (y < fmt.SUB_E))
//       //      println("x > 1.9: " + (x > 1.9))

//       //println(s"          ==> exp: $y [min: ${fmt.MIN_E}, max: ${fmt.MAX_E}, sub: ${fmt.SUB_E}]")
//       //println(s"          ==> man: $x")

//       if (y < fmt.SUB_E && x > 1.9) {
//         //println("Adjusting y += 1 and x = 1")
//         y += 1
//         x = 1
//       }
//       else if (x >= 2) {
//         y += 1
//         x = value.abs / BigDecimal(2).pow(y)
//       }
//       val cutoff = if (y < 0) BigDecimal(1) - BigDecimal(2).pow(-fmt.sbits) else BigDecimal(1)

//       //println(s"          ==> cutoff: x < $cutoff [${x < cutoff}]")

//       if (x < cutoff) {
//         y -= 1
//         x = value.abs / BigDecimal(2).pow(y)
//       }

//       //      println("FINAL: ")
//       //      println(s"exp: $y [min: ${fmt.MIN_E}, max: ${fmt.MAX_E}, sub: ${fmt.SUB_E}]")
//       //      println(s"man: $x")


//       val result = if (y > fmt.MAX_E) {
//         Left(Inf(negative = value < 0))
//       }
//       else if (y >= fmt.MIN_E) {
//         val mantissaP1 = ((x - 1) * BigDecimal(2).pow(fmt.sbits + 1)).toBigInt
//         val mantissa = (mantissaP1 + (if (mantissaP1.testBit(0)) 1 else 0)) >> 1
//         val expPart = y + fmt.bias

//         //println(s"          ==> NORMAL ")
//         //println(s"          ==> mantissa: ${BigDecimal(mantissaP1)/BigDecimal(2).pow(fmt.sbits+1)} ($mantissaP1 [${mantissaP1.fancyBits(fmt.sbits+1)}])")
//         //println(s"          ==> rounded:  ${BigDecimal(mantissa)/BigDecimal(2).pow(fmt.sbits)} ($mantissa [${mantissa.fancyBits(fmt.sbits)}])")

//         Right((value < 0, mantissa, expPart))
//       }
//       else if (y < fmt.MIN_E && y >= fmt.SUB_E) {
//         val mantissa = (x * BigDecimal(2).pow(fmt.sbits + 1)).toBigInt
//         val expBits = BigInt(0)
//         val shift = (fmt.MIN_E - y + 1).toInt
//         val shiftedMantissa = (mantissa >> shift) + (if (mantissa.testBit(shift-1)) 1 else 0)

//         //println(s"          ==> SUB! ")
//         //println(s"          ==> mantissa: $mantissa [${mantissa.bits(fmt.sbits + 1)}]")
//         //println(s"          ==> shift: $shift")
//         //println(s"          ==> shiftedM: $mantissa [${shiftedMantissa.bits(fmt.sbits + 1)}]")

//         //println(s"mantissa: " + Array.tabulate(fmt.sbits+1){i => mantissa.testBit(i) }.map{x => if (x) 1 else 0}.reverse.mkString(""))
//         //println(s"mantissa: " + Array.tabulate(fmt.sbits+1){i => shiftedMantissa.testBit(i) }.map{x => if (x) 1 else 0}.reverse.mkString(""))
//         //println(s"shift: $shift")
//         if (shiftedMantissa > 0) {
//           Right((value < 0, shiftedMantissa, expBits))
//         }
//         else {
//           Left(Zero(negative = value < 0))  // Underflow
//         }
//       }
//       else {
//         Left(Zero(negative = value < 0))  // Underflow
//       }
//       result
//     }
//   }
//   def convertBackToValue(x: Either[FloatValue, (Boolean,BigInt,BigInt)], fmt: FltFormat): FloatValue = x match {
//     case Right((s,m,e)) =>
//       if (e > 0) {
//         val y = e.toInt - fmt.bias.toInt
//         val x = BigDecimal(m) / BigDecimal(2).pow(fmt.sbits) + 1 //+ (if (e == 1) 0 else 1)
//         val sign = if (s) -1 else 1
//         //println(s"$sign * $x * 2^$y")
//         Value(x * BigDecimal(2).pow(y) * sign)
//       }
//       else {
//         val x = BigDecimal(m) / BigDecimal(2).pow(fmt.sbits - 1)
//         val y = BigDecimal(2).pow(fmt.MIN_E.toInt - 1)
//         val sign = if (s) -1 else 1
//         Value(sign * x * y)
//       }
//     case Left(value) => value
//   }

//   def clamped(value: FloatValue, valid: Boolean, fmt: FltFormat): FloatPoint = value match {
//     case NaN | _: Inf | _:Zero =>
//       val result = new FloatPoint(value, valid, fmt)
//       //println(s"Clamping: $value ==> $result")
//       result

//     case Value(v) =>
//       //println(s"Clamping: $value ")
//       val m = clamp(v, fmt)
//       val actualValue = convertBackToValue(m, fmt)
//       val result = new FloatPoint(actualValue, valid, fmt)

//       //println(s"          ==> $m ")
//       //println(s"          ==> $actualValue")
//       //println(s"          ==> $result")
//       result
//   }

//   def fromBits(bits: Array[Bool], fmt: FltFormat): FloatPoint = {
//     val sign = bits.last
//     val exp = bits.slice(fmt.sbits,fmt.sbits+fmt.ebits)
//     val man = bits.slice(0, fmt.sbits)
//     val value = {
//       if (!exp.exists(_.value) && !man.exists(_.value)) Zero(sign.value)
//       else if (exp.forall(_.value)) {
//         if (man.exists(_.value)) NaN
//         else Inf(sign.value)
//       }
//       else {
//         var exponent = BigInt(0)
//         exp.zipWithIndex.foreach{case (bit, i) => if (bit.value) exponent = exponent.setBit(i) }
//         var mantissa = BigInt(0)
//         man.zipWithIndex.foreach{case (bit, i) => if (bit.value) mantissa = mantissa.setBit(i) }

//         //println("exponent: " + exponent)
//         //println("mantissa: " + mantissa)
//         //exponent += fmt.bias

//         convertBackToValue(Right((sign.value,mantissa,exponent)), fmt)
//       }
//     }
//     new FloatPoint(value, true, fmt)
//   }

//   def fromByteArray(array: Array[Byte], fmt: FltFormat): FloatPoint = {
//     val bits = array.flatMap{byte =>
//       (0 until 8).map{i => Bool( (byte & (1 << i)) > 0) }
//     }
//     fromBits(bits, fmt)
//   }

//   /**
//     * Generate a pseudo-random floating point number, uniformly distributed between [0, 1)
//     * FIXME: Uses Double right now
//     * @param fmt The format for the fixed point number being generated
//     */
//   def random(fmt: FltFormat): FloatPoint = FloatPoint(scala.util.Random.nextDouble(), fmt)

//   /**
//     * Generate a pseudo-random floating point number, uniformly distributed between [0, max)
//     * @param max The maximum value of the range, non-inclusive
//     * @param fmt The format for the max and the fixed point number being generated
//     */
//   def random(max: FloatPoint, fmt: FltFormat): FloatPoint = FloatPoint.random(fmt) * max

// }
