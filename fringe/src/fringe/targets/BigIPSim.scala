package fringe.targets

import chisel3.core.IntParam
import chisel3._
import fringe.BigIP
import fringe.templates.hardfloat._
import fringe.templates.math._
import fringe.utils.implicits._
import fringe.templates.math.{OverflowMode, RoundingMode, FloatingPoint}
import fringe.utils.getRetimed

class BigIPSim extends BigIP with SimBlackBoxes {
  def divide(dividend: UInt, divisor: UInt, latency: Int, flow: Bool): UInt = {
    getConst(divisor) match { 
      case Some(bigNum) => getRetimed(dividend / bigNum.U, latency, flow)
      case None => getRetimed(dividend/divisor, latency, flow)
    }
  }

  def divide(dividend: SInt, divisor: SInt, latency: Int, flow: Bool): SInt = {
    getConst(divisor) match { 
      case Some(bigNum) => getRetimed(dividend / bigNum.S, latency, flow)
      case None => getRetimed(dividend/divisor, latency, flow)
    }
  }

  def mod(dividend: UInt, divisor: UInt, latency: Int, flow: Bool): UInt = {
    getConst(divisor) match { 
      case Some(bigNum) => getRetimed(dividend % bigNum.U, latency, flow)
      case None => getRetimed(dividend % divisor, latency, flow)
    }
  }

  def mod(dividend: SInt, divisor: SInt, latency: Int, flow: Bool): SInt = {
    getConst(divisor) match { 
      case Some(bigNum) => getRetimed(dividend % bigNum.S, latency, flow)
      case None => getRetimed(dividend % divisor, latency, flow)
    }
  }

  def multiply(a: UInt, b: UInt, latency: Int, flow: Bool): UInt = {
    val aconst = getConst(a)
    val bconst = getConst(b)
    if (aconst.isDefined | bconst.isDefined) { // Constant optimization
      if (aconst.isDefined && bconst.isDefined) { (aconst.get * bconst.get).U }
      else {
        val const = if (aconst.isDefined) aconst.get else bconst.get
        val other = if (aconst.isDefined) b else a
        getRetimed(const.U * other, latency, flow)
      }
    }
    else {
      getRetimed(a * b, latency, flow)
    }
  }
  def multiply(a: SInt, b: SInt, latency: Int, flow: Bool): SInt = {
    val aconst = getConst(a)
    val bconst = getConst(b)
    if (aconst.isDefined | bconst.isDefined) { // Constant optimization
      if (aconst.isDefined && bconst.isDefined) { (aconst.get * bconst.get).S }
      else {
        val const = if (aconst.isDefined) aconst.get else bconst.get
        val other = if (aconst.isDefined) b else a
        getRetimed(const.S * other, latency, flow)
      }
    } else {
      getRetimed(a * b, latency, flow)
    }
  }

  override def log2(a: UInt, latency: Int, flow: Bool): UInt = {
    val m = Module(new Log2Sim(a.getWidth, false, latency))
    m.io.x := a
    m.io.y
  }
  
  override def sqrt(num: UInt, latency: Int, flow: Bool): UInt = {
    val m = Module(new SqrtSim(num.getWidth, false, latency))
    m.io.x := num
    m.io.y
  }
  override def sin(num: UInt, latency: Int): UInt = {
    num // TODO    
  }
  override def cos(num: UInt, latency: Int): UInt = {
    num // TODO    
  }
  override def atan(num: UInt, latency: Int): UInt = {
    num // TODO    
  }
  override def sinh(num: UInt, latency: Int): UInt = {
    num // TODO    
  }
  override def cosh(num: UInt, latency: Int): UInt = {
    num // TODO    
  }

  class Log2Sim(val width: Int, val signed: Boolean, val latency: Int) extends Module {
    val io = IO(new Bundle{
      val x = Input(UInt(width.W))
      val y = Output(UInt(width.W))
    })
    val m = Module(new Log2BBox(width, signed, latency))
    m.io <> DontCare
    m.io.clk := clock
    m.io.reset := reset.toBool
    m.io.x := io.x
    io.y := m.io.y
  }

  class SqrtSim(val width: Int, val signed: Boolean, val latency: Int) extends Module {
    val io = IO(new Bundle{
      val x = Input(UInt(width.W))
      val y = Output(UInt(width.W))
    })
    val m = Module(new SqrtSimBBox(width, signed, latency))
    m.io <> DontCare
    m.io.clk := clock
    m.io.reset := reset.toBool | (getRetimed(io.x,1,true.B) =/= io.x) // Hacky but should be ok for now
    m.io.x := getRetimed(io.x,1,true.B)
    io.y := m.io.y
  }


  override def fsqrt(a: UInt, m: Int, e: Int, latency: Int, flow: Bool): UInt = {
    val fma = Module(new DivSqrtRecFN_small(e, m, 0))
    fma.io <> DontCare
    fma.io.a := recFNFromFN(e, m, a)
    fma.io.inValid := true.B // TODO: What should this be?
    fma.io.sqrtOp := true.B // TODO: What should this be?
    fma.io.roundingMode := 0.U(3.W) // TODO: What should this be?
    fma.io.detectTininess := false.B // TODO: What should this be?
    getRetimed(fNFromRecFN(e, m, fma.io.out), latency, flow)
  }

  def fadd(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): UInt = {
    val fma = Module(new MulAddRecFN(e, m))
    fma.io <> DontCare
    fma.io.a := recFNFromFN(e, m, a)
    fma.io.b := recFNFromFN(e, m, FloatingPoint.bits(1.0f).S((m+e).W))
    fma.io.c := recFNFromFN(e, m, b)
    fma.io.op := 0.U(2.W)
    getRetimed(fNFromRecFN(e, m, fma.io.out), latency, flow)
  }

  def fsub(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): UInt = {
    val fma = Module(new MulAddRecFN(e, m))
    fma.io <> DontCare
    fma.io.a := recFNFromFN(e, m, a)
    fma.io.b := recFNFromFN(e, m, FloatingPoint.bits(1.0f).S((m+e).W))
    fma.io.c := recFNFromFN(e, m, b)
    fma.io.op := 1.U(2.W)
    getRetimed(fNFromRecFN(e, m, fma.io.out), latency, flow)
  }
  def fmul(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): UInt = {
    val fma = Module(new MulAddRecFN(e, m))
    fma.io <> DontCare
    fma.io.a := recFNFromFN(e, m, a)
    fma.io.b := recFNFromFN(e, m, b)
    fma.io.c := recFNFromFN(e, m, FloatingPoint.bits(0.0f).S((m+e).W))
    fma.io.op := 0.U(2.W)
    getRetimed(fNFromRecFN(e, m, fma.io.out), latency, flow)
  }
  def fdiv(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): UInt = {
    val fma = Module(new DivSqrtRecFN_small(e, m, 0))
    fma.io <> DontCare
    fma.io.a := recFNFromFN(e, m, a)
    fma.io.b := recFNFromFN(e, m, b)
    fma.io.inValid := true.B // TODO: What should this be?
    fma.io.sqrtOp := false.B // TODO: What should this be?
    fma.io.roundingMode := 0.U(3.W) // TODO: What should this be?
    fma.io.detectTininess := false.B // TODO: What should this be?
    getRetimed(fNFromRecFN(e, m, fma.io.out), latency, flow)
  }

  def flt(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(e, m))
    comp.io <> DontCare
    comp.io.a := recFNFromFN(e, m, a)
    comp.io.b := recFNFromFN(e, m, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.lt
    getRetimed(result, latency, flow)
  }
  def feq(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(e, m))
    comp.io <> DontCare
    comp.io.a := recFNFromFN(e, m, a)
    comp.io.b := recFNFromFN(e, m, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.eq
    getRetimed(result, latency, flow)
  }
  def fgt(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(e, m))
    comp.io <> DontCare
    comp.io.a := recFNFromFN(e, m, a)
    comp.io.b := recFNFromFN(e, m, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.gt
    getRetimed(result, latency, flow)
  }
  def fge(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(e, m))
    comp.io <> DontCare
    comp.io.a := recFNFromFN(e, m, a)
    comp.io.b := recFNFromFN(e, m, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.gt | comp.io.eq
    getRetimed(result, latency, flow)
  }
  def fle(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(e, m))
    comp.io <> DontCare
    comp.io.a := recFNFromFN(e, m, a)
    comp.io.b := recFNFromFN(e, m, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := comp.io.lt | comp.io.eq
    getRetimed(result, latency, flow)
  }
  def fne(a: UInt, b: UInt, m: Int, e: Int, latency: Int, flow: Bool): Bool = {
    val result = Wire(Bool())
    val comp = Module(new CompareRecFN(e, m))
    comp.io <> DontCare
    comp.io.a := recFNFromFN(e, m, a)
    comp.io.b := recFNFromFN(e, m, b)
    comp.io.signaling := false.B // TODO: What is this bit for?
    result := ~comp.io.eq
    getRetimed(result, latency, flow)
  }

  override def fix2fix(src: UInt, s1: Boolean, d1: Int, f1: Int, s2: Boolean, d2: Int, f2: Int, latency: Int, flow: Bool, rounding: RoundingMode, saturating: OverflowMode): UInt = {
    if (src.litArg.isEmpty) {
      val fix2fixBox = Module(new fix2fixBox(s1, d1, f1, s2, d2, f2, rounding, saturating))
      fix2fixBox.io <> DontCare
      fix2fixBox.io.a := src
      fix2fixBox.io.expect_neg := false.B
      fix2fixBox.io.expect_pos := false.B
      getRetimed(fix2fixBox.io.b, latency, flow)
    }
    // Likely that there are mistakes here
    else {
      val f_gain = f2 - f1
      val d_gain = d2 - d1
      val salt = rounding match {
        case Unbiased if f_gain < 0 => BigInt((scala.math.random * (1 << -f_gain).toDouble).toLong)
        case _ => BigInt(0)
      }
      val newlit = saturating match {
        case Wrapping =>
          if (f_gain < 0 & d_gain >= 0)       (src.litArg.get.num + salt) >> -f_gain
          else if (f_gain >= 0 & d_gain >= 0) (src.litArg.get.num) << f_gain
          else if (f_gain >= 0 & d_gain < 0)  ((src.litArg.get.num + salt) >> -f_gain) & BigInt((1 << (d2 + f2 + 1)) - 1)
          else ((src.litArg.get.num) << f_gain) & BigInt((1 << (d2 + f2 + 1)) -1)
        case Saturating =>
          if (src.litArg.get.num > BigInt((1 << (d2 + f2 + 1))-1)) BigInt((1 << (d2 + f2 + 1))-1)
          else {
            if (f_gain < 0 & d_gain >= 0)       (src.litArg.get.num + salt) >> -f_gain
            else if (f_gain >= 0 & d_gain >= 0) (src.litArg.get.num) << f_gain
            else if (f_gain >= 0 & d_gain < 0)  ((src.litArg.get.num + salt) >> -f_gain) & BigInt((1 << (d2 + f2 + 1)) - 1)
            else ((src.litArg.get.num) << f_gain) & BigInt((1 << (d2 + f2 + 1)) -1)
          }
      }
      getRetimed(newlit.S((d2 + f2 + 1).W).asUInt.apply(d2 + f2 - 1, 0), latency, flow)
    }

  }

  override def fix2flt(a: UInt, sign: Boolean, dec: Int, frac: Int, man: Int, exp: Int, latency: Int, flow: Bool): UInt = {
    val conv = Module(new INToRecFN(a.getWidth,exp,man))
    conv.io <> DontCare
    conv.io.signedIn := sign.B
    conv.io.in := a
    conv.io.roundingMode := 0.U(3.W)
    conv.io.detectTininess := false.B
    getRetimed(fNFromRecFN(exp, man, conv.io.out), latency, flow)
  }

  override def flt2fix(a: UInt, man: Int, exp: Int, sign: Boolean, dec: Int, frac: Int, latency: Int, flow: Bool, rounding: RoundingMode, saturating: OverflowMode): UInt = {
    val conv = Module(new RecFNToIN(exp,man,a.getWidth))
    conv.io <> DontCare
    conv.io.signedOut := sign.B
    conv.io.in := recFNFromFN(exp, man, a)
    conv.io.roundingMode := 0.U(3.W)
    getRetimed(conv.io.out, latency, flow)
  }
}



  class SqrtSimBBox(val width: Int, val signed: Boolean, val latency: Int) extends BlackBox(
    Map("DWIDTH" -> IntParam(width))
  ) {
    val io = IO(new Bundle{
      val clk = Input(Clock())
      val rdy = Output(Bool())
      val reset = Input(Bool())
      val x = Input(UInt(width.W))
      val y = Output(UInt(width.W))
    })
  }
