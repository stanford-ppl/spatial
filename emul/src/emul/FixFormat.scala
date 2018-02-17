package emul

case class FixFormat(sign: Boolean, ibits: Int, fbits: Int) {
  lazy val bits: Int = ibits + fbits
  lazy val MAX_FRACTIONAL_VALUE: BigInt = (BigInt(1) << (fbits - 1)) - 1

  lazy val MAX_INTEGRAL_VALUE: BigInt = (if (sign) (BigInt(1) << (ibits-1)) - 1      else (BigInt(1) << ibits) - 1) << fbits
  lazy val MIN_INTEGRAL_VALUE: BigInt = if (sign) -(BigInt(1) << (ibits-1)) << fbits else BigInt(0)

  lazy val MAX_VALUE: BigInt = if (sign) (BigInt(1) << (ibits+fbits-1)) - 1 else (BigInt(1) << (ibits+fbits)) - 1
  lazy val MIN_VALUE: BigInt = if (sign) -(BigInt(1) << (ibits+fbits-1))    else BigInt(0)

  lazy val MAX_INTEGRAL_VALUE_FP: FixedPoint = FixedPoint.clamped(MAX_INTEGRAL_VALUE, valid=true, this)
  lazy val MIN_INTEGRAL_VALUE_FP: FixedPoint = FixedPoint.clamped(MIN_INTEGRAL_VALUE, valid=true, this)
  lazy val MAX_VALUE_FP: FixedPoint = FixedPoint.clamped(MAX_VALUE, valid=true, this)
  lazy val MIN_VALUE_FP: FixedPoint = FixedPoint.clamped(MIN_VALUE, valid=true, this)
  lazy val MIN_POSITIVE_VALUE_FP: FixedPoint = FixedPoint.clamped(BigInt(1), valid=true, this)

  def isExactInt: Boolean  = fbits == 0 && ((sign && ibits <= 32) || ibits <= 31)
  def isExactLong: Boolean = fbits == 0 && ((sign && ibits <= 64) || ibits <= 63)
  def combine(that: FixFormat): FixFormat = {
    val sign = this.sign || that.sign
    val ibits = Math.max(this.ibits + (if (sign && !this.sign) 1 else 0),
      that.ibits + (if (sign && !that.sign) 1 else 0)
    )
    val fbits = Math.max(this.fbits, that.fbits)
    FixFormat(sign,ibits,fbits)
  }
}
