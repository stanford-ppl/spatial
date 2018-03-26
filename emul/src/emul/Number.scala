package emul

abstract class Number extends Equals {
  def bits: Array[Bool]
  final def bitString: String = "0b" + bits.reverse.map{x =>
    if (x.valid && x.value) "1" else if (x.valid) "0" else "X"
  }.mkString("")

  final def fancyBitString(grp: Int = 4): String = "0b" + bits.grouped(grp).toSeq.reverse.map{xs =>
    xs.reverse.map{x => if (x.valid && x.value) "1" else if (x.valid) 0 else "X" }.mkString("")
  }.mkString("|")

  final def toByteArray: Array[Byte] = {
    val bts = this.bits
    bts.sliding(8,8).map{bits =>
      var x: Byte = 0
      bits.zipWithIndex.foreach{case (b,i) => if (b.value) { x = (x | (1 << i)).toByte } }
      x
    }.toArray
  }

  def <(that: Int): Boolean
  def <=(that: Int): Boolean
  def >(that: Int): Boolean
  def >=(that: Int): Boolean
  def ===(that: Int): Boolean
  def !==(that: Int): Boolean

  def ===(that: Long): Boolean
  def ===(that: Float): Boolean
  def ===(that: Double): Boolean

  override def canEqual(that: Any): Boolean = that match {
    case _: Char   => true
    case _: Byte   => true
    case _: Short  => true
    case _: Int    => true
    case _: Long   => true
    case _: Float  => true
    case _: Double => true
    case _: java.lang.Character => true
    case _: java.lang.Byte    => true
    case _: java.lang.Short   => true
    case _: java.lang.Integer => true
    case _: java.lang.Long    => true
    case _: java.lang.Float   => true
    case _: java.lang.Double  => true
    case _: String => true
    case _: Symbol => true
    case _ => false
  }

  override def equals(that: Any): Boolean = that match {
    case x: java.lang.Character => this.equals(x.toChar)
    case x: java.lang.Byte    => this.equals(x.toByte)
    case x: java.lang.Short   => this.equals(x.toShort)
    case x: java.lang.Integer => this.equals(x.toInt)
    case x: java.lang.Long    => this.equals(x.toLong)
    case x: java.lang.Float   => this.equals(x.toFloat)
    case x: java.lang.Double  => this.equals(x.toDouble)
    case x: Symbol            => this.equals(x.name.toString)
    case _ => false
  }

  def toByte: Byte
  def toShort: Short
  def toInt: Int
  def toLong: Long
  def toFloat: Float
  def toDouble: Double
  def toBigDecimal: BigDecimal

  def isExactInt: Boolean = this === this.toInt
  def isExactLong: Boolean = this === this.toLong
  def isExactFloat: Boolean = this === this.toFloat
  def isExactDouble: Boolean = this === this.toDouble
}

object Number {
  // Round positive direction always
  def ceil(x: FixedPoint): FixedPoint = {
    val add = if (x > 0 && (x % 1 !== 0)) 1 else 0
    val clamp = (x.value >> x.fmt.fbits) << x.fmt.fbits
    FixedPoint.clamped(clamp, x.valid, x.fmt) + add
  }
  def floor(x: FixedPoint): FixedPoint = {
    val add = if (x < 0 && (x % 1 !== 0)) -1 else 0
    val clamp = (x.value >> x.fmt.fbits) << x.fmt.fbits
    FixedPoint.clamped(clamp, x.valid, x.fmt) + add
  }

  def abs(x: FixedPoint): FixedPoint = if (x < 0) -x else x
  def min(x: FixedPoint, y: FixedPoint): FixedPoint = if ((x < y).value) x else y
  def max(x: FixedPoint, y: FixedPoint): FixedPoint = if ((x > y).value) x else y

  // TODO: These all rely on Double implementation right now
  def recip(x: FixedPoint): FixedPoint = FixedPoint(1,x.fmt) / x
  def sqrt(x: FixedPoint): FixedPoint = FixedPoint(Math.sqrt(x.toDouble), x.fmt).withValid(x.valid)
  def recipSqrt(x: FixedPoint): FixedPoint = FixedPoint(1,x.fmt) / sqrt(x)
  def exp(x: FixedPoint): FixedPoint = FixedPoint(Math.exp(x.toDouble), x.fmt).withValid(x.valid)
  def log(x: FixedPoint): FixedPoint = FixedPoint(Math.log(x.toDouble), x.fmt).withValid(x.valid)
  def pow(x: FixedPoint, exp: FixedPoint) = FixedPoint(Math.pow(x.toDouble, exp.toDouble), x.fmt).withValid(x.valid)
  def sin(x: FixedPoint): FixedPoint = FixedPoint(Math.sin(x.toDouble), x.fmt).withValid(x.valid)
  def cos(x: FixedPoint): FixedPoint = FixedPoint(Math.cos(x.toDouble), x.fmt).withValid(x.valid)
  def tan(x: FixedPoint): FixedPoint = FixedPoint(Math.tan(x.toDouble), x.fmt).withValid(x.valid)
  def sinh(x: FixedPoint): FixedPoint = FixedPoint(Math.sinh(x.toDouble), x.fmt).withValid(x.valid)
  def cosh(x: FixedPoint): FixedPoint = FixedPoint(Math.cosh(x.toDouble), x.fmt).withValid(x.valid)
  def tanh(x: FixedPoint): FixedPoint = FixedPoint(Math.tanh(x.toDouble), x.fmt).withValid(x.valid)
  def asin(x: FixedPoint): FixedPoint = FixedPoint(Math.asin(x.toDouble), x.fmt).withValid(x.valid)
  def acos(x: FixedPoint): FixedPoint = FixedPoint(Math.acos(x.toDouble), x.fmt).withValid(x.valid)
  def atan(x: FixedPoint): FixedPoint = FixedPoint(Math.atan(x.toDouble), x.fmt).withValid(x.valid)

  def sigmoid(x: FixedPoint): FixedPoint = FixedPoint(1,x.fmt) / (exp(-x) + 1)

  def ceil(x: FloatPoint): FloatPoint = x.value match {
    case NaN      => x
    case Inf(_)   => x
    case Zero(_)  => x
    case Value(v) =>
      val remain = v % 1
      if (v >= 0 && remain.abs > 0) FloatPoint.clamped(Value(v - remain + 1),x.valid,x.fmt)
      else FloatPoint.clamped(Value(v - remain), x.valid, x.fmt)
  }
  def floor(x: FloatPoint): FloatPoint = x.value match {
    case NaN     => x
    case Inf(_)  => x
    case Zero(_) => x
    case Value(v) =>
      val remain = v % 1
      if (v < 0 && remain.abs > 0) FloatPoint.clamped(Value(v - remain - 1), x.valid, x.fmt)
      else FloatPoint.clamped(Value(v - remain), x.valid, x.fmt)
  }

  def abs(x: FloatPoint): FloatPoint = if ((x < FloatPoint(0,x.fmt)).value) -x else x
  def min(x: FloatPoint, y: FloatPoint): FloatPoint = if ((x < y).value) x else y
  def max(x: FloatPoint, y: FloatPoint): FloatPoint = if ((x > y).value) x else y

  // TODO: These all rely on Double implementation right now
  def recip(x: FloatPoint): FloatPoint = FloatPoint(1, x.fmt) / x
  def sqrt(x: FloatPoint): FloatPoint = FloatPoint(Math.sqrt(x.toDouble), x.fmt).withValid(x.valid)
  def recipSqrt(x: FloatPoint): FloatPoint = FloatPoint(1, x.fmt) / sqrt(x)
  def exp(x: FloatPoint): FloatPoint = FloatPoint(Math.exp(x.toDouble), x.fmt).withValid(x.valid)
  def log(x: FloatPoint): FloatPoint = FloatPoint(Math.log(x.toDouble), x.fmt).withValid(x.valid)
  def pow(x: FloatPoint, exp: FloatPoint) = FloatPoint(Math.pow(x.toDouble, exp.toDouble), x.fmt).withValid(x.valid)
  def sin(x: FloatPoint): FloatPoint = FloatPoint(Math.sin(x.toDouble), x.fmt).withValid(x.valid)
  def cos(x: FloatPoint): FloatPoint = FloatPoint(Math.cos(x.toDouble), x.fmt).withValid(x.valid)
  def tan(x: FloatPoint): FloatPoint = FloatPoint(Math.tan(x.toDouble), x.fmt).withValid(x.valid)
  def sinh(x: FloatPoint): FloatPoint = FloatPoint(Math.sinh(x.toDouble), x.fmt).withValid(x.valid)
  def cosh(x: FloatPoint): FloatPoint = FloatPoint(Math.cosh(x.toDouble), x.fmt).withValid(x.valid)
  def tanh(x: FloatPoint): FloatPoint = FloatPoint(Math.tanh(x.toDouble), x.fmt).withValid(x.valid)
  def asin(x: FloatPoint): FloatPoint = FloatPoint(Math.asin(x.toDouble), x.fmt).withValid(x.valid)
  def acos(x: FloatPoint): FloatPoint = FloatPoint(Math.acos(x.toDouble), x.fmt).withValid(x.valid)
  def atan(x: FloatPoint): FloatPoint = FloatPoint(Math.atan(x.toDouble), x.fmt).withValid(x.valid)
}

