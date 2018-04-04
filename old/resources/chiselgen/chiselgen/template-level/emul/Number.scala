package emul

abstract class Number extends Equals {
  def bits: Array[Bool]

  def toByteArray: Array[Byte] = {
    val bts = this.bits
    bts.sliding(8,8).map{bits =>
      var x: Byte = 0
      bits.zipWithIndex.foreach{case (b,i) => if (b.value) { x = (x | (1 << i)).toByte } }
      x
    }.toArray
  }
}

object Number {
  // Round positive direction always
  def ceil(x: FixedPoint): FixedPoint = {
    val y = FixedPoint(x.value >> x.fmt.fbits, x.fmt)
    if (x.value >= 0 && (x > y).value) y + FixedPoint(1,x.fmt) else y
  }
  def floor(x: FixedPoint): FixedPoint = {
    val y = FixedPoint(x.value >> x.fmt.fbits, x.fmt)
    if (x.value < 0 && (x < y).value) y - FixedPoint(1,x.fmt) else y
  }

  def abs(x: FixedPoint): FixedPoint = if ((x < FixedPoint(0,x.fmt)).value) -x else x
  def min(x: FixedPoint, y: FixedPoint): FixedPoint = if ((x < y).value) x else y
  def max(x: FixedPoint, y: FixedPoint): FixedPoint = if ((x > y).value) x else y

  // TODO: These all rely on Double implementation right now
  def sqrt(x: FixedPoint): FixedPoint = FixedPoint(Math.sqrt(x.toDouble), x.fmt).withValid(x.valid)
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
  def sqrt(x: FloatPoint): FloatPoint = FloatPoint(Math.sqrt(x.toDouble), x.fmt).withValid(x.valid)
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
