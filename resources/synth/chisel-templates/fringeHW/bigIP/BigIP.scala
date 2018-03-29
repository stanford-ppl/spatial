package fringe.bigIP
import chisel3._
import chisel3.util._

/**
 * Target-specific IP
 */
abstract class BigIP {
  def getConst[T<:Data](sig: T): Option[BigInt] = sig match {
    case u: UInt => if (u.litArg.isDefined) Some(u.litArg.get.num) else None
    case s: SInt => if (s.litArg.isDefined) Some(s.litArg.get.num) else None
    case _ => None
  }

  def divide(dividend: UInt, divisor: UInt, latency: Int): UInt
  def divide(dividend: SInt, divisor: SInt, latency: Int): SInt
  def mod(dividend: UInt, divisor: UInt, latency: Int): UInt
  def mod(dividend: SInt, divisor: SInt, latency: Int): SInt
  def multiply(a: UInt, b: UInt, latency: Int): UInt
  def multiply(a: SInt, b: SInt, latency: Int): SInt

  def fadd(a: UInt, b: UInt, m: Int, e: Int): UInt
  def fsub(a: UInt, b: UInt, m: Int, e: Int): UInt
  def fmul(a: UInt, b: UInt, m: Int, e: Int): UInt
  def fdiv(a: UInt, b: UInt, m: Int, e: Int): UInt
  def flt(a: UInt, b: UInt, m: Int, e: Int): Bool
  def feq(a: UInt, b: UInt, m: Int, e: Int): Bool
  def fgt(a: UInt, b: UInt, m: Int, e: Int): Bool
  def fge(a: UInt, b: UInt, m: Int, e: Int): Bool
  def fle(a: UInt, b: UInt, m: Int, e: Int): Bool
  def fne(a: UInt, b: UInt, m: Int, e: Int): Bool
}


