package fringe.utils

import chisel3._
import chisel3.util.Cat
import fringe.globals
import fringe.templates.memory._
import fringe.templates.math.{FixedPoint, FloatingPoint, Math, Truncate, RoundingMode, OverflowMode, Wrapping}


object implicits {

  implicit class ArrayOps[T](b: Array[FixedPoint]) {
    def raw: UInt = Cat(b.map(_.raw))
    def FP(s: Boolean, d: Int, f: Int): FixedPoint = raw.FP(s, d, f)
  }

  implicit class SeqIntOps[T](b: Seq[Int]) {
    def getOr1(idx: Int): Int = if (b.size > idx) b(idx) else 1
  }

  implicit class SeqBoolOps[T](b: Seq[Bool]) {
    def or: Bool  = if (b.isEmpty) false.B else b.reduce{_||_}
    def and: Bool = if (b.isEmpty) true.B else b.reduce{_&&_}
  }

  implicit class ArrayBoolOps[T](b: Array[Bool]) {
    def or: Bool = if (b.isEmpty) false.B else b.reduce{_||_}
    def and: Bool = if (b.isEmpty) true.B else b.reduce{_&&_}
    def raw: UInt = Cat(b.map(_.raw))
    def FP(s: Boolean, d: Int, f: Int): FixedPoint = raw.FP(s, d, f)
  }

  implicit class IndexedSeqOps[T](b: scala.collection.immutable.IndexedSeq[FixedPoint]) {
    def raw: UInt = Cat(b.map(_.raw))
    def FP(s: Boolean, d: Int, f: Int): FixedPoint = raw.FP(s, d, f)
  }

  implicit class VecOps[T <: Data](b: Vec[T]) {
    def raw = new VecUInt(b)
    def r = raw
    def FP(s: Boolean, d: Int, f: Int): FixedPoint = raw.raw.FP(s, d, f)
  }

  implicit class BoolOps(b: Bool) {
    def toSeq: Seq[Bool] = Seq(b)

    def D(delay: Int, retime_released: Bool): Bool = {
      Mux(retime_released, getRetimed(b, delay), false.B)
    }
    def D(delay: Double, retime_released: Bool): Bool = b.D(delay.toInt, retime_released)
    def D(delay: Double): Bool = b.D(delay.toInt, true.B)
    def reverse: Bool = b

    // Stream version
    def DS(delay: Int, retime_released: Bool, flow: Bool): Bool = {
      Mux(retime_released, getRetimed(b, delay, flow), false.B)
    }
    def DS(delay: Double, retime_released: Bool, flow: Bool): Bool = b.DS(delay.toInt, retime_released, flow)
    def DS(delay: Double, flow: Bool): Bool = b.DS(delay.toInt, true.B, flow)
  }

  implicit class UIntOps(b: UInt) {
    def toSeq: Seq[UInt] = Seq(b)

    // Define number so that we can be compatible with FixedPoint type
    def number: UInt = b
    def raw: UInt = b
    def rd: UInt = b
    def r: UInt = b
    def msb = b(b.getWidth-1)

    def <(c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) < c
    def <=(c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) <= c
    def > (c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) > c
    def >= (c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) >= c
    def === (c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) === c
    def =/= (c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) =/= c

    def ^(c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) ^ c
    def +(c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) + c
    def -(c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) - c

    // FIXME: Update operator
    def <+> (c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) <+> c
    def <-> (c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) <-> c

    // def mul(c: FixedPoint, delay: Option[Double], flow: Bool, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping, myName: String): FixedPoint = {
    //   Math.mul(b.trueFP(c.fmt), c, delay, flow, rounding, saturating, myName)
    // }

    // def div(c: FixedPoint, delay: Option[Double], flow: Bool, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping, myName: String): FixedPoint = {
    //   Math.div(b.trueFP(c.fmt), c, delay, flow, rounding, saturating, myName)
    // }

    // def mod(c: FixedPoint, delay: Option[Double], flow: Bool, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping, myName: String): FixedPoint = {
    //   Math.mod(b.trueFP(c.fmt), c, delay, flow, rounding, saturating, myName)
    // }

    // def mul(c: UInt, delay: Option[Double], flow: Bool, myName: String): UInt = {
    //   Math.mul(b, c, delay, flow, myName)
    // }

    // def div(c: UInt, delay: Option[Double], flow: Bool, myName: String): UInt = {
    //   Math.div(b, c, delay, flow, myName)
    // }

    // def mod(c: UInt, delay: Option[Double], flow: Bool, myName: String): UInt = {
    //   Math.mod(b, c, delay, flow, myName)
    // }

    /** Convert this to the given FixedPoint format, overflowing if outside the representable bounds. */
    def FP(s: Boolean, d: Int, f: Int): FixedPoint = FixedPoint(s, d, f, b)

    /** Convert this to the given FixedPoint format, overflowing if outside the representable bounds. */
    def FP(fmt: emul.FixFormat): FixedPoint = FixedPoint(fmt.sign, fmt.ibits, fmt.fbits, b)

    /** Convert this to the given FixedPoint format, but increase the number of integer bits if necessary. */
    def trueFP(fmt: emul.FixFormat): FixedPoint = FixedPoint(fmt.sign, fmt.ibits max b.getWidth, fmt.fbits, b)

    /** Set the value of c to be the value of the UInt b. */
    def cast(c: FixedPoint, sign_extend: Boolean = false): Unit = { c.r := FixedPoint(c.s,c.d,c.f,b, sign_extend).r }
    def cast(c: => UInt): Unit = { c.r := b.r }

    def toFixed(fmt: emul.FixFormat): FixedPoint = FixedPoint(fmt.sign, fmt.ibits, fmt.fbits, b)

  }

  implicit class SIntOps(val b:SInt) {
    def toSeq: Seq[SInt] = Seq(b)
    // Define number so that we can be compatible with FixedPoint type
    def number: UInt = b.asUInt
    def raw: UInt = b.asUInt
    def rd: UInt = b.asUInt
    def r: UInt = b.asUInt
    def msb = b(b.getWidth-1)

    def < (c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) < c
    def <=(c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) <= c
    def > (c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) > c
    def >=(c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) >= c
    def ===(c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) === c
    def =/=(c: FixedPoint): Bool = FixedPoint(c.s, b.getWidth max c.d, c.f, b) =/= c

    def ^(c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) ^ c
    def +(c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) + c
    def -(c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) - c


    def <+>(c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) <+> c
    def <->(c: FixedPoint): FixedPoint = FixedPoint(c.s, b.getWidth max c.d, c.f, b) <-> c

    // def mul(c: FixedPoint, delay: Option[Double], flow: Bool, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping, myName: String): FixedPoint = {
    //   Math.mul(b.trueFP(c.fmt), c, delay, flow, rounding, saturating, myName)
    // }

    // def div(c: FixedPoint, delay: Option[Double], flow: Bool, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping, myName: String): FixedPoint = {
    //   Math.div(b.trueFP(c.fmt), c, delay, flow, rounding, saturating, myName)
    // }

    // def mod(c: FixedPoint, delay: Option[Double], flow: Bool, rounding: RoundingMode = Truncate, saturating: OverflowMode = Wrapping, myName: String): FixedPoint = {
    //   Math.mod(b.trueFP(c.fmt), c, delay, flow, rounding, saturating, myName)
    // }

    // def mul(c: SInt, delay: Option[Double], flow: Bool, myName: String): SInt = {
    //   Math.mul(b, c, delay, flow, myName)
    // }

    // def div(c: SInt, delay: Option[Double], flow: Bool, myName: String): SInt = {
    //   Math.div(b, c, delay, flow, myName)
    // }

    // def mod(c: SInt, delay: Option[Double], flow: Bool, myName: String): SInt = {
    //   Math.mod(b, c, delay, flow, myName)
    // }

    def FP(s: Boolean, d: Int, f: Int): FixedPoint = FixedPoint(s, d, f, b)
    def FP(fmt: emul.FixFormat): FixedPoint = FixedPoint(fmt.sign, fmt.ibits, fmt.fbits, b)
    def trueFP(fmt: emul.FixFormat): FixedPoint = FixedPoint(fmt.sign, fmt.ibits max b.getWidth, fmt.fbits, b)

    def FlP(m: Int, e: Int): FloatingPoint = FloatingPoint(m, e, b)
    def FlP(fmt: emul.FltFormat): FloatingPoint = FloatingPoint(fmt.sbits+1, fmt.ebits, b)

    def cast(c: FixedPoint): Unit = { c.r := FixedPoint(c.s, c.d, c.f, b).r }

    def toFixed(fmt: emul.FixFormat): FixedPoint = FixedPoint(fmt.sign, fmt.ibits, fmt.fbits, b)

  }


  implicit class IntOps(v: Int) {
    def FP(s: Boolean, d: Int, f: Int): FixedPoint = FixedPoint(s, d, f, v)
    def FlP(m: Int, e: Int): FloatingPoint = FloatingPoint(m, e, v)
    def indices[T](func: Int => T): Seq[T] = (0 until v).map{i => func(i) }.toSeq
  }

  implicit class DoubleOps(v: Double) {
    def FP(s: Boolean, d: Int, f: Int): FixedPoint = FixedPoint(s, d, f, v)
    def FlP(m: Int, e: Int): FloatingPoint = FloatingPoint(m, e, v)
  }

  implicit def UIntLikeToUInt(UIntLike: UIntLike): UInt = UIntLike.asUInt
  implicit def ForceUInt[T](v: T): UInt = {
    v match {
      case u: UInt => u
      case ul: UIntLike => ul.asUInt
      case fallthrough: Data => fallthrough.asUInt()
    }
  }

  def ConvAndCat(elements: {def raw: UInt}*): UInt = {
    chisel3.util.Cat(elements map {
      _.raw
    })
  }
}
