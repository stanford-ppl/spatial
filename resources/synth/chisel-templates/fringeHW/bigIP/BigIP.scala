package fringe.bigIP
import chisel3._
import chisel3.util._

abstract class BigIP {
  def getConst[T<:Data](sig: T): Option[BigInt] = sig match {
    case u: UInt => if (u.litArg.isDefined) Some(u.litArg.get.num) else None
    case s: SInt => if (s.litArg.isDefined) Some(s.litArg.get.num) else None
    case _ => None
  }

  def divide(dividend: UInt, divisor: UInt, latency: Int, flow: Bool): UInt
  def divide(dividend: SInt, divisor: SInt, latency: Int, flow: Bool): SInt
  def mod(dividend: UInt, divisor: UInt, latency: Int, flow: Bool): UInt
  def mod(dividend: SInt, divisor: SInt, latency: Int, flow: Bool): SInt
  def multiply(a: UInt, b: UInt, latency: Int, flow: Bool): UInt
  def multiply(a: SInt, b: SInt, latency: Int, flow: Bool): SInt
  def sqrt(a: UInt, latency: Int): UInt = {
    throw new Exception("sqrt not implemented!")
  }
  def sin(a: UInt, latency: Int): UInt = {
    throw new Exception("sin not implemented!")
  }
  def cos(a: UInt, latency: Int): UInt = {
    throw new Exception("cos not implemented!")
  }
  def atan(a: UInt, latency: Int): UInt = {
    throw new Exception("atan not implemented!")
  }
  def sinh(a: UInt, latency: Int): UInt = {
    throw new Exception("sinh not implemented!")
  }
  def cosh(a: UInt, latency: Int): UInt = {
    throw new Exception("cosh not implemented!")
  }

  def fsqrt(a: UInt, latency: Int): UInt = {
    throw new Exception("fsqrt not implemented!")
  }
  def fadd(a: UInt, b: UInt, m: Int, e: Int, latency: Int): UInt
  def fsub(a: UInt, b: UInt, m: Int, e: Int): UInt
  def fmul(a: UInt, b: UInt, m: Int, e: Int): UInt
  def fdiv(a: UInt, b: UInt, m: Int, e: Int): UInt
  def flt(a: UInt, b: UInt, m: Int, e: Int): Bool
  def feq(a: UInt, b: UInt, m: Int, e: Int): Bool
  def fgt(a: UInt, b: UInt, m: Int, e: Int): Bool
  def fge(a: UInt, b: UInt, m: Int, e: Int): Bool
  def fle(a: UInt, b: UInt, m: Int, e: Int): Bool
  def fne(a: UInt, b: UInt, m: Int, e: Int): Bool
  def fabs(a: UInt, m: Int, e: Int): UInt = {
    throw new Exception("fabs not implemented!")
  }
  def fexp(a: UInt, m: Int, e: Int): UInt = {
    throw new Exception("fexp not implemented!")
  }
  def flog(a: UInt, m: Int, e: Int): UInt = {
    throw new Exception("flog not implemented!")
  }
  def fsqrt(a: UInt, m: Int, e: Int): UInt = {
    throw new Exception("fsqrt not implemented!")
  }
  def frec(a: UInt, m: Int, e: Int): UInt = {
    throw new Exception("frec not implemented!")
  }
  def frsqrt(a: UInt, m: Int, e: Int): UInt = {
    throw new Exception("frsqrt not implemented!")
  }
  def ffma(a: UInt, b: UInt, c: UInt, m: Int, e: Int): UInt = {
    throw new Exception("ffma not implemented!")
  }
  def fix2flt(a: UInt, sign: Boolean, dec: Int, frac: Int, mw: Int, e: Int): UInt = {
    throw new Exception("fix2flt not implemented!")
  }
  def fix2fix(a: UInt, sign: Boolean, dec: Int, frac: Int): UInt = {
    throw new Exception("fix2fix not implemented!")
  }
  def flt2fix(a: UInt, mw: Int, e: Int, sign: Boolean, dec: Int, frac: Int): UInt = {
    throw new Exception("flt2fix not implemented!")
  }
  def flt2flt(a: UInt, mwa: Int, ea: Int, mw_out: Int, e_out: Int): UInt = {
    throw new Exception("flt2flt not implemented!")
  }
  def fltaccum(a: UInt, en: Bool, last: Bool, m: Int, e: Int): UInt = {
    throw new Exception("fltaccum not implemented!")
  }

}


