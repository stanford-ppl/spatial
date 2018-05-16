package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._
import emul.FloatPoint

trait ScalaGenFltPt extends ScalaGenBits {

  override protected def remap(tp: Type[_]): String = tp match {
    case FltPtType(_,_) => "FloatPoint"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp, c) match {
    case (FltPtType(g,e), c: FloatPoint) => s"""FloatPoint(BigDecimal("$c"), FltFormat(${g-1},$e))"""
    case _ => super.quoteConst(tp,c)
  }

  override def invalid(tp: Type[_]): String = tp match {
    case FltPtType(g,e) => src"FloatPoint.invalid(FltFormat(${g-1},$e))"
    case _ => super.invalid(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FltIsPosInf(x) => emit(src"val $lhs = Bool($x.isPositiveInfinity, $x.valid)")
    case FltIsNegInf(x) => emit(src"val $lhs = Bool($x.isNegativeInfinity, $x.valid)")
    case FltIsNaN(x)    => emit(src"val $lhs = Bool($x.isNaN, $x.valid)")

    case FltNeg(x)   => emit(src"val $lhs = -$x")
    case FltAdd(x,y) => emit(src"val $lhs = $x + $y")
    case FltSub(x,y) => emit(src"val $lhs = $x - $y")
    case FltMul(x,y) => emit(src"val $lhs = $x * $y")
    case FltDiv(x,y) => emit(src"val $lhs = $x / $y")
    case FltMod(x,y) => emit(src"val $lhs = $x / $y")
    case FltRecip(x) => emit(src"val $lhs = Number.recip($x)")
    case FltLst(x,y) => emit(src"val $lhs = $x < $y")
    case FltLeq(x,y) => emit(src"val $lhs = $x <= $y")

    case FltNeq(x,y)   => emit(src"val $lhs = $x !== $y")
    case FltEql(x,y)   => emit(src"val $lhs = $x === $y")

    case FltMax(x,y) => emit(src"val $lhs = Number.max($x,$y)")
    case FltMin(x,y) => emit(src"val $lhs = Number.min($x,$y)")

    case FltToFlt(x, fmt) =>
      emit(src"val $lhs = $x.toFloatPoint(FltFormat(${fmt.mbits-1},${fmt.ebits}))")

    case FltToFix(x, fmt) =>
      emit(src"val $lhs = $x.toFixedPoint(FixFormat(${fmt.sign},${fmt.ibits},${fmt.fbits}))")

    case TextToFlt(x,fmt) =>
      emit(src"val $lhs = FloatPoint($x, FltFormat(${fmt.mbits-1},${fmt.ebits}))")

    case FltToText(x) => emit(src"val $lhs = $x.toString")

    case FltRandom(Some(max)) =>
      val FltPtType(g,e) = lhs.tp
      emit(src"val $lhs = FloatPoint.random($max, FltFormat(${g-1},$e))")

    case FltRandom(None) =>
      val FltPtType(g,e) = lhs.tp
      emit(src"val $lhs = FloatPoint.random(FltFormat(${g-1},$e))")

    case FltAbs(x)     => emit(src"val $lhs = Number.abs($x)")
    case FltFloor(x)   => emit(src"val $lhs = Number.floor($x)")
    case FltCeil(x)    => emit(src"val $lhs = Number.ceil($x)")
    case FltLn(x)      => emit(src"val $lhs = Number.ln($x)")
    case FltExp(x)     => emit(src"val $lhs = Number.exp($x)")
    case FltSqrt(x)    => emit(src"val $lhs = Number.sqrt($x)")
    case FltSin(x)     => emit(src"val $lhs = Number.sin($x)")
    case FltCos(x)     => emit(src"val $lhs = Number.cos($x)")
    case FltTan(x)     => emit(src"val $lhs = Number.tan($x)")
    case FltSinh(x)    => emit(src"val $lhs = Number.sinh($x)")
    case FltCosh(x)    => emit(src"val $lhs = Number.cosh($x)")
    case FltTanh(x)    => emit(src"val $lhs = Number.tanh($x)")
    case FltAsin(x)    => emit(src"val $lhs = Number.asin($x)")
    case FltAcos(x)    => emit(src"val $lhs = Number.acos($x)")
    case FltAtan(x)    => emit(src"val $lhs = Number.atan($x)")
    case FltPow(x,exp) => emit(src"val $lhs = Number.pow($x, $exp)")
    case FltFMA(m1,m2,add) => emit(src"val $lhs = ($m1 * $m2) + $add")
    case FltRecipSqrt(x)   => emit(src"val $lhs = Number.recipSqrt($x)")
    case FltSigmoid(x)     => emit(src"val $lhs = ${one(x.tp)} / (Number.exp(-$x) + ${one(x.tp)})")


    case _ => super.gen(lhs, rhs)
  }

}
