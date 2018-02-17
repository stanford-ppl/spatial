package emul

case class FltFormat(sbits: Int, ebits: Int) {
  lazy val bits: Int = sbits + ebits + 1
  lazy val bias: BigInt = BigInt(2).pow(ebits - 1) - 1
  lazy val MIN_E: BigInt = -bias + 1      // Represented as exponent of 1
  lazy val MAX_E: BigInt = bias
  lazy val SUB_E: BigInt = MIN_E - sbits  // Exponent is all 0s

  lazy val MAX_VALUE_FP: FloatPoint = {
    val x = Array.tabulate(bits){i => if (i == bits-1 || i == bits-1-ebits) Bool(false) else Bool(true) }
    //println(x.toStr)
    FloatPoint.fromBits(x, this)
  }
  lazy val MIN_VALUE_FP: FloatPoint = -MAX_VALUE_FP

  lazy val MIN_POSITIVE_VALUE: FloatPoint = {
    val exp = BigInt(0)
    val man = BigInt(1)
    val value = FloatPoint.convertBackToValue(Right(false,man,exp), this)
    new FloatPoint(value, true, this)
  }

  def combine(that: FltFormat): FltFormat = {
    val sbits = Math.max(this.sbits,that.sbits)
    val ebits = Math.max(this.ebits,that.ebits)
    FltFormat(sbits, ebits)
  }
}
