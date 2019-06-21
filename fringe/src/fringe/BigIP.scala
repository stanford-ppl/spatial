package fringe

import chisel3._
import fringe.templates.math.{OverflowMode, RoundingMode}

abstract class BigIP {
  case class Unimplemented(op: String) extends Exception(s"$op is not implemented for the given target.")

  def getConst[T<:Data](sig: T): Option[BigInt] = sig match {
    case u: UInt => if (u.litOption.isDefined) Some(u.litOption.get) else None
    case s: SInt => if (s.litOption.isDefined) Some(s.litOption.get) else None
    case _ => None
  }

  def divide(dividend: UInt, divisor: UInt, latency: Int, flow: Bool, myName: String): UInt
  def divide(dividend: SInt, divisor: SInt, latency: Int, flow: Bool, myName: String): SInt
  def mod(dividend: UInt, divisor: UInt, latency: Int, flow: Bool, myName: String): UInt
  def mod(dividend: SInt, divisor: SInt, latency: Int, flow: Bool, myName: String): SInt
  def multiply(a: UInt, b: UInt, latency: Int, flow: Bool, myName: String): UInt
  def multiply(a: SInt, b: SInt, latency: Int, flow: Bool, myName: String): SInt

  // TODO: ???
  def sqrt(a: UInt, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("sqrt")

  def sin(a: UInt, latency: Int, myName: String): UInt = throw Unimplemented("sin")
  def cos(a: UInt, latency: Int, myName: String): UInt = throw Unimplemented("cos")
  def atan(a: UInt, latency: Int, myName: String): UInt = throw Unimplemented("ata")
  def sinh(a: UInt, latency: Int, myName: String): UInt = throw Unimplemented("sin")
  def cosh(a: UInt, latency: Int, myName: String): UInt = throw Unimplemented("cos")

  def log2(a: UInt, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("log2")

  /** Floating point addition. */
  def fadd(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt

  /** Floating point subtraction. */
  def fsub(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt

  /** Floating point multiplication. */
  def fmul(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt

  /** Floating point division. */
  def fdiv(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt

  /** Floating point less-than comparison. */
  def flt(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool

  /** Floating point greater-than comparison. */
  def fgt(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool

  /** Floating point greater-than or equal comparison. */
  def fge(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool

  /** Floating point less-than or equal comparison. */
  def fle(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool

  /** Floating point inequality comparison. */
  def fne(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool

  /** Floating point equality comparison. */
  def feq(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool

  /** Floating point absolute value. */
  def fabs(a: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("fabs")

  /** Floating point natural exponentiation (out = e ** a). */
  def fexp(a: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("fexp")

  /** Floating point hyperbolic tangent. */
  def ftanh(a: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("ftanh")

  /** Floating point sigmoid. */
  def fsigmoid(a: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("fsigmoid")

  /** Floating point natural log. */
  def fln(a: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("fln")

  /** Floating point reciprocal (out = 1/x). */
  def frec(a: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("frec")

  /** Floating point square root (out = sqrt(x)). */
  // TODO: Why do we have two variants here?
  def fsqrt(a: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("fsqrt")

  /** Floating point reciprocal square root. */
  def frsqrt(a: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("frsqrt")

  /** Floating point fused multiply add. */
  def ffma(a: UInt, b: UInt, c: UInt, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("ffma")

  /** Conversion from fixed point (sign, dec, frac) to floating point (man, exp). */
  def fix2flt(a: UInt, sign: Boolean, dec: Int, frac: Int, man: Int, exp: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("fix2flt")

  /** Conversion from one fixed point type to another fixed point type. */
  def fix2fix(a: UInt, sign1: Boolean, dec1: Int, frac1: Int, sign2: Boolean, dec2: Int, frac2: Int, latency: Int, flow: Bool, rounding: RoundingMode, saturating: OverflowMode, myName: String): UInt = throw Unimplemented("fix2fix")

  /** Conversion from floating point (man, exp) to fixed point (sign, dec, frac). */
  def flt2fix(a: UInt, man: Int, exp: Int, sign: Boolean, dec: Int, frac: Int, latency: Int, flow: Bool, rounding: RoundingMode, saturating: OverflowMode, myName: String): UInt = throw Unimplemented("flt2fix")

  /** Conversion from one floating point type (man1, exp1) to another floating point type (man2, exp2). */
  def flt2flt(a: UInt, man1: Int, exp1: Int, man2: Int, exp2: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("flt2flt")

  /** Floating point accumulation. */
  def fltaccum(a: UInt, en: Bool, last: Bool, m: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = throw Unimplemented("fltaccum")
}
