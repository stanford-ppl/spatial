package fringe.fringeZynq.bigIP
import fringe.FringeGlobals
import fringe.bigIP.BigIP
import templates.Utils
import chisel3._
import chisel3.util._
import scala.collection.mutable.Set

class BigIPZynq extends BigIP with ZynqBlackBoxes {
  def divide(dividend: UInt, divisor: UInt, latency: Int, flow: Bool): UInt = {
    getConst(divisor) match { 
      case Some(bigNum) =>
        Utils.getRetimed(dividend / bigNum.U, latency, flow)
      case None =>
        val m = Module(new Divider(dividend.getWidth, divisor.getWidth, false, latency))
        m.io.dividend := dividend
        m.io.divisor := divisor
        m.io.flow := flow
        m.io.out
    }
  }

  def divide(dividend: SInt, divisor: SInt, latency: Int, flow: Bool): SInt = {
    getConst(divisor) match { 
      case Some(bigNum) =>
        Utils.getRetimed(dividend / bigNum.S, latency, flow)
      case None =>
        val m = Module(new Divider(dividend.getWidth, divisor.getWidth, true, latency))
        m.io.dividend := dividend.asUInt
        m.io.divisor := divisor.asUInt
        m.io.flow := flow
        m.io.out.asSInt
    }
  }

  def mod(dividend: UInt, divisor: UInt, latency: Int, flow: Bool): UInt = {
    getConst(divisor) match { 
      case Some(bigNum) =>
        Utils.getRetimed(dividend % bigNum.U, latency, flow)
      case None =>
        val m = Module(new Modulo(dividend.getWidth, divisor.getWidth, false, latency))
        m.io.dividend := dividend
        m.io.divisor := divisor
        m.io.flow := flow
        m.io.out
    }
  }

  def mod(dividend: SInt, divisor: SInt, latency: Int, flow: Bool): SInt = {
    getConst(divisor) match { 
      case Some(bigNum) =>
        Utils.getRetimed(dividend % bigNum.S, latency, flow)
      case None =>
        val m = Module(new Modulo(dividend.getWidth, divisor.getWidth, true, latency))
        m.io.dividend := dividend.asUInt
        m.io.divisor := divisor.asUInt
        m.io.flow := flow
        m.io.out.asSInt
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
        Utils.getRetimed(const.U * other, latency, flow)
      }
    } else {
      val m = Module(new Multiplier(a.getWidth, b.getWidth, math.max(a.getWidth, b.getWidth), false, latency))
      m.io.a := a
      m.io.b := b
      m.io.flow := flow
      m.io.out
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
        Utils.getRetimed(const.S * other, latency, flow)
      }
    } else {
      val m = Module(new Multiplier(a.getWidth, b.getWidth, math.max(a.getWidth, b.getWidth), true, latency))
      m.io.a := a.asUInt
      m.io.b := b.asUInt
      m.io.flow := flow
      m.io.out.asSInt
    }
  }

  override def sqrt(a: UInt, latency: Int): UInt = {
    val m = Module(new SquareRooter(a.getWidth, false, latency))
    m.io.a := a
    m.io.out
  }

  def fadd(a: UInt, b: UInt, mw: Int, e: Int, latency: Int): UInt = {
    val m = Module(new FAdd(mw, e, latency))
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
  override def fabs(a: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FAbs(mw, e))
    m.io.a := a
    m.io.out
  }
  override def fexp(a: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FExp(mw, e))
    m.io.a := a
    m.io.out
  }
  override def flog(a: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FLog(mw, e))
    m.io.a := a
    m.io.out
  }
  override def fsqrt(a: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FSqrt(mw, e))
    m.io.a := a
    m.io.out
  }
  override def frec(a: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FRec(mw, e))
    m.io.a := a
    m.io.out
  }
  override def frsqrt(a: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FRSqrt(mw, e))
    m.io.a := a
    m.io.out
  }
  override def ffma(a: UInt, b: UInt, c: UInt, mw: Int, e: Int): UInt = {
    val m = Module(new FFma(mw, e))
    m.io.a := a
    m.io.b := b
    m.io.c := c
    m.io.out
  }
  override def fix2flt(a: UInt, sign: Boolean, dec: Int, frac: Int, mw: Int, e: Int): UInt = {
    val m = Module(new Fix2Float(dec, frac, mw, e))
    m.io.a := a
    m.io.out
  }
  override def flt2fix(a: UInt, mw: Int, e: Int, sign: Boolean, dec: Int, frac: Int): UInt = {
    val m = Module(new Float2Fix(mw, e, sign, dec, frac))
    m.io.a := a
    m.io.out
  }
  override def flt2flt(a: UInt, mwa: Int, ea: Int, mw_out: Int, e_out: Int): UInt = {
    val m = Module(new Float2Float(mwa, ea, mw_out, e_out))
    m.io.a := a
    m.io.out
  }
  override def fltaccum(a: UInt, en: Bool, last: Bool, mw: Int, e: Int): UInt = {
    val m = Module(new FAccum(mw, e))
    m.io.a := a
    m.io.en := en
    m.io.last := last
    m.io.out
  }

}
