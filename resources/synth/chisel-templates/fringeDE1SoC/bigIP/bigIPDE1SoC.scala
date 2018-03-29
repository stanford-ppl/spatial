package fringe.fringeDE1SoC.bigIP
import fringe.FringeGlobals
import fringe.bigIP.BigIP
import chisel3._
import chisel3.util._
import scala.collection.mutable.Set

class BigIPDE1SoC extends BigIP {
  def divide(dividend: UInt, divisor: UInt, latency: Int): UInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) =>
        dividend / bigNum.U
      case None =>
        dividend / divisor
    }
  }

  def divide(dividend: SInt, divisor: SInt, latency: Int): SInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) =>
        dividend / bigNum.S
      case None =>
        dividend / divisor
    }
  }

  def mod(dividend: UInt, divisor: UInt, latency: Int): UInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) =>
        dividend % bigNum.U
      case None =>
        dividend % divisor
    }
  }

  def mod(dividend: SInt, divisor: SInt, latency: Int): SInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) =>
        dividend % bigNum.S
      case None =>
        dividend % divisor
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
      a * b
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
      a * b
    }
  }

  def fadd(a: UInt, b: UInt, mw: Int, e: Int): UInt = {
    // val m = Module(new FAdd(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a
  }

  def fsub(a: UInt, b: UInt, mw: Int, e: Int): UInt = {
    // val m = Module(new FSub(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a
  }
  def fmul(a: UInt, b: UInt, mw: Int, e: Int): UInt = {
    // val m = Module(new FMul(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a
  }
  def fdiv(a: UInt, b: UInt, mw: Int, e: Int): UInt = {
    // val m = Module(new FDiv(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a
  }
  def flt(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    // val m = Module(new FLt(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a < b
  }
  def feq(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    // val m = Module(new FEq(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a < b
  }
  def fgt(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    // val m = Module(new FGt(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a < b
  }
  def fge(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    // val m = Module(new FGe(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a < b
  }
  def fle(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    // val m = Module(new FLe(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a < b
  }
  def fne(a: UInt, b: UInt, mw: Int, e: Int): Bool = {
    // val m = Module(new FNe(mw, e))
    // m.io.a := a
    // m.io.b := b
    // m.io.out
    a < b
  }

}
