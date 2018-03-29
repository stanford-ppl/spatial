package fringe.bigIP
import chisel3._
import chisel3.util._
import templates.Utils
import types._

class BigIPSim extends BigIP {
  def divide(dividend: UInt, divisor: UInt, latency: Int): UInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) => dividend / bigNum.U
      case None => Utils.getRetimed(dividend/divisor, latency)
    }
  }

  def divide(dividend: SInt, divisor: SInt, latency: Int): SInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) => dividend / bigNum.S
      case None => Utils.getRetimed(dividend/divisor, latency)
    }
  }

  def mod(dividend: UInt, divisor: UInt, latency: Int): UInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) => dividend % bigNum.U
      case None => Utils.getRetimed(dividend % divisor, latency)
    }
  }

  def mod(dividend: SInt, divisor: SInt, latency: Int): SInt = {
    getConst(divisor) match { // Use combinational Verilog divider and ignore latency if divisor is constant
      case Some(bigNum) => dividend % bigNum.S
      case None => Utils.getRetimed(dividend % divisor, latency)
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
        Utils.getRetimed(const.U * other, 0)
      }
    } else {
      Utils.getRetimed(a * b, latency)
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
        Utils.getRetimed(const.S * other, 0)
      }
    } else {
      Utils.getRetimed(a * b, latency)
    }
  }

  def fadd(a: UInt, b: UInt, m: Int, e: Int): UInt = {
    val result = Wire(new FloatingPoint(m, e))
    val fma = Module(new MulAddRecFN(m, e))
    fma.io.a := recFNFromFN(m, e, a)
    fma.io.b := recFNFromFN(m, e, Utils.getFloatBits(1.0f).S((m+e).W))
    fma.io.c := recFNFromFN(m, e, b)
    fma.io.op := 0.U(2.W)
    fNFromRecFN(m, e, fma.io.out)
  }

  def fsub(a: UInt, b: UInt, m: Int, e: Int): UInt = {
    val fma = Module(new MulAddRecFN(m, e))
    fma.io.a := recFNFromFN(m, e, a)
    fma.io.b := recFNFromFN(m, e, Utils.getFloatBits(1.0f).S((m+e).W))
    fma.io.c := recFNFromFN(m, e, b)
    fma.io.op := 1.U(2.W)
    fNFromRecFN(m, e, fma.io.out)
  }
  def fmul(a: UInt, b: UInt, m: Int, e: Int): UInt = {
    val fma = Module(new MulAddRecFN(m, e))
    fma.io.a := recFNFromFN(m, e, a)
    fma.io.b := recFNFromFN(m, e, b)
    fma.io.c := recFNFromFN(m, e, Utils.getFloatBits(0.0f).S((m+e).W))
    fma.io.op := 0.U(2.W)
    fNFromRecFN(m, e, fma.io.out)
  }
  def fdiv(a: UInt, b: UInt, m: Int, e: Int): UInt = {
    val fma = Module(new DivSqrtRecFN_small(m, e, 0))
    fma.io.a := recFNFromFN(m, e, a)
    fma.io.b := recFNFromFN(m, e, b)
    fma.io.inValid := true.B // TODO: What should this be?
    fma.io.sqrtOp := false.B // TODO: What should this be?
    fma.io.roundingMode := 0.U(3.W) // TODO: What should this be?
    fma.io.detectTininess := false.B // TODO: What should this be?
    fNFromRecFN(m, e, fma.io.out)
  }

  def flt(a: UInt, b: UInt, m: Int, e: Int): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(m, e))
    comp.io.a := recFNFromFN(m, e, a)
    comp.io.b := recFNFromFN(m, e, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.lt
    result
  }
  def feq(a: UInt, b: UInt, m: Int, e: Int): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(m, e))
    comp.io.a := recFNFromFN(m, e, a)
    comp.io.b := recFNFromFN(m, e, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.eq
    result
  }
  def fgt(a: UInt, b: UInt, m: Int, e: Int): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(m, e))
    comp.io.a := recFNFromFN(m, e, a)
    comp.io.b := recFNFromFN(m, e, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.gt
    result
  }
  def fge(a: UInt, b: UInt, m: Int, e: Int): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(m, e))
    comp.io.a := recFNFromFN(m, e, a)
    comp.io.b := recFNFromFN(m, e, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.gt | comp.io.eq
    result
  }
  def fle(a: UInt, b: UInt, m: Int, e: Int): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(m, e))
    comp.io.a := recFNFromFN(m, e, a)
    comp.io.b := recFNFromFN(m, e, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.lt | comp.io.eq
    result
  }
  def fne(a: UInt, b: UInt, m: Int, e: Int): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(m, e))
    comp.io.a := recFNFromFN(m, e, a)
    comp.io.b := recFNFromFN(m, e, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := ~comp.io.eq
    result
  }
}


