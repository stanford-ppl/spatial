package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import emul.FloatPoint

trait PIRGenFltPt extends PIRGenBits {

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
    //case FltNeg(x)   => 
    //case FltAdd(x,y) => 
    //case FltSub(x,y) => 
    //case FltMul(x,y) => 
    //case FltDiv(x,y) => 
    //case FltRecip(x) => 
    //case FltLst(x,y) => 
    //case FltLeq(x,y) => 

    //case FltNeq(x,y)   => 
    //case FltEql(x,y)   => 

    //case FltMax(x,y) => 
    //case FltMin(x,y) => 

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

    //case FltAbs(x)     => 
    //case FltFloor(x)   => 
    //case FltCeil(x)    => 
    //case FltLn(x)      => 
    //case FltExp(x)     => 
    //case FltSqrt(x)    => 
    //case FltSin(x)     => 
    //case FltCos(x)     => 
    //case FltTan(x)     => 
    //case FltSinh(x)    => 
    //case FltCosh(x)    => 
    //case FltTanh(x)    => 
    //case FltAsin(x)    => 
    //case FltAcos(x)    => 
    //case FltAtan(x)    => 
    //case FltPow(x,exp) => 
    //case FltFMA(m1,m2,add) =>
    //case FltRecipSqrt(x)   =>
    //case FltSigmoid(x)     =>

    case _ => super.gen(lhs, rhs)
  }

}
