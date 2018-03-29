package fringe.fringeZynq.bigIP
import fringe.FringeGlobals
import fringe.bigIP.BigIP
import chisel3._
import chisel3.util._
import scala.collection.mutable.Set

class BigIPZynq extends BigIP with ZynqBlackBoxes {
  def divide(dividend: UInt, divisor: UInt, latency: Int): UInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) =>
        dividend / bigNum.U
      case None =>
        val m = Module(new Divider(dividend.getWidth, divisor.getWidth, false, latency))
        m.io.dividend := dividend
        m.io.divisor := divisor
        m.io.out
    }
  }

  def divide(dividend: SInt, divisor: SInt, latency: Int): SInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) =>
        dividend / bigNum.S
      case None =>
        val m = Module(new Divider(dividend.getWidth, divisor.getWidth, true, latency))
        m.io.dividend := dividend.asUInt
        m.io.divisor := divisor.asUInt
        m.io.out.asSInt
    }
  }

  def mod(dividend: UInt, divisor: UInt, latency: Int): UInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) =>
        dividend % bigNum.U
      case None =>
        val m = Module(new Modulo(dividend.getWidth, divisor.getWidth, false, latency))
        m.io.dividend := dividend
        m.io.divisor := divisor
        m.io.out
    }
  }

  def mod(dividend: SInt, divisor: SInt, latency: Int): SInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) =>
        dividend % bigNum.S
      case None =>
        val m = Module(new Modulo(dividend.getWidth, divisor.getWidth, true, latency))
        m.io.dividend := dividend.asUInt
        m.io.divisor := divisor.asUInt
        m.io.out.asSInt
    }
  }

  def multiply(a: UInt, b: UInt, latency: Int): UInt = {
    val aconst = getConst(a)
    val bconst = getConst(b)
    if (aconst.isDefined | bconst.isDefined) { // Constant optimization
      if (aconst.isDefined && bconst.isDefined) { (aconst.get * bconst.get).U }
      else {
        val const = if (aconst.isDefined) aconst.get else bconst.get
        val other = if (aconst.isDefined) b else a
        const.U * other
      }
    } else {
      val m = Module(new Multiplier(a.getWidth, b.getWidth, math.max(a.getWidth, b.getWidth), false, latency))
      m.io.a := a
      m.io.b := b
      m.io.out
    }
  }

  def multiply(a: SInt, b: SInt, latency: Int): SInt = {
    val aconst = getConst(a)
    val bconst = getConst(b)
    if (aconst.isDefined | bconst.isDefined) { // Constant optimization
      if (aconst.isDefined && bconst.isDefined) { (aconst.get * bconst.get).S }
      else {
        val const = if (aconst.isDefined) aconst.get else bconst.get
        val other = if (aconst.isDefined) b else a
        const.S * other
      }
    } else {
      val m = Module(new Multiplier(a.getWidth, b.getWidth, math.max(a.getWidth, b.getWidth), true, latency))
      m.io.a := a.asUInt
      m.io.b := b.asUInt
      m.io.out.asSInt
    }
  }

  def fadd(a: UInt, b: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FAdd(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }

  def fsub(a: UInt, b: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FSub(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fmul(a: UInt, b: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FMul(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fdiv(a: UInt, b: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FDiv(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def flt(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    val m = Module(new FLt(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def feq(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    val m = Module(new FEq(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fgt(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    val m = Module(new FGt(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fge(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    val m = Module(new FGe(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fle(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    val m = Module(new FLe(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }
  def fne(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    val m = Module(new FNe(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.out
  }

}
