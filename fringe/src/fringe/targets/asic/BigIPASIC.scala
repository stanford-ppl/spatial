package fringe.targets.asic

import chisel3._
import fringe.BigIP

class BigIPASIC extends BigIP with ASICBlackBoxes {
  // Use combinational Verilog divider and ignore latency if divisor is constant
  def divide(dividend: UInt, divisor: UInt, latency: Int, flow: Bool, myName: String): UInt = getConst(divisor) match {
    case Some(bigNum) => dividend / bigNum.U
    case None =>
      val m = Module(new Divider(dividend.getWidth, divisor.getWidth, false, latency))
      m.io.dividend := dividend
      m.io.divisor := divisor
      m.io.flow := flow
      m.io.out
  }

  // Use combinational Verilog divider and ignore latency if divisor is constant
  def divide(dividend: SInt, divisor: SInt, latency: Int, flow: Bool, myName: String): SInt = getConst(divisor) match {
    case Some(bigNum) => dividend / bigNum.S
    case None =>
      val m = Module(new Divider(dividend.getWidth, divisor.getWidth, true, latency))
      m.io.dividend := dividend.asUInt
      m.io.divisor := divisor.asUInt
      m.io.flow := flow
      m.io.out.asSInt
  }

  // Use combinational Verilog divider and ignore latency if divisor is constant
  def mod(dividend: UInt, divisor: UInt, latency: Int, flow: Bool, myName: String): UInt = getConst(divisor) match {
    case Some(bigNum) => dividend % bigNum.U
    case None =>
      val m = Module(new Modulo(dividend.getWidth, divisor.getWidth, false, latency))
      m.io.dividend := dividend
      m.io.divisor := divisor
      m.io.flow := flow
      m.io.out
  }

  // Use combinational Verilog divider and ignore latency if divisor is constant
  def mod(dividend: SInt, divisor: SInt, latency: Int, flow: Bool, myName: String): SInt = getConst(divisor) match {
    case Some(bigNum) => dividend % bigNum.S
    case None =>
      val m = Module(new Modulo(dividend.getWidth, divisor.getWidth, true, latency))
      m.io.dividend := dividend.asUInt
      m.io.divisor := divisor.asUInt
      m.io.flow := flow
      m.io.out.asSInt
  }

  def multiply(a: UInt, b: UInt, latency: Int, flow: Bool, myName: String): UInt = (getConst(a), getConst(b)) match {
    case (Some(ac), Some(bc)) => (ac * bc).U
    case (None,     Some(bc)) => a * bc.U
    case (Some(ac), None)     => ac.U * b
    case (None,     None)     =>
      val m = Module(new Multiplier(a.getWidth, b.getWidth, math.max(a.getWidth, b.getWidth), false, latency))
      m.io.a := a
      m.io.b := b
      m.io.flow := flow
      m.io.out
  }

  def multiply(a: SInt, b: SInt, latency: Int, flow: Bool, myName: String): SInt = (getConst(a), getConst(b)) match {
    case (Some(ac), Some(bc)) => (ac * bc).S
    case (None,     Some(bc)) => a * bc.S
    case (Some(ac), None)     => ac.S * b
    case (None,     None)     =>
      val m = Module(new Multiplier(a.getWidth, b.getWidth, math.max(a.getWidth, b.getWidth), true, latency))
      m.io.a := a.asUInt
      m.io.b := b.asUInt
      m.io.flow := flow
      m.io.out.asSInt
  }

  def fadd(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FAdd(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }

  def fsub(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FSub(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fmul(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FMul(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fdiv(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): UInt = {
    val m = Module(new FDiv(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def flt(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FLt(mw, e))
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def feq(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FEq(mw, e))
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fgt(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FGt(mw, e))
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fge(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FGe(mw, e))
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fle(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FLe(mw, e))
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fne(a: UInt, b: UInt, mw: Int, e: Int, latency: Int, flow: Bool, myName: String): Bool = {
    val m = Module(new FNe(mw, e))
    m.io.flow := flow
    m.io.a := a
    m.io.b := b
    m.io.out
  }

}
