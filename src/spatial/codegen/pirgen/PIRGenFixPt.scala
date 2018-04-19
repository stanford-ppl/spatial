package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import emul.FixedPoint

trait PIRGenFixPt extends PIRGenBits {

  override protected def remap(tp: Type[_]): String = tp match {
    case _:Fix[_,_,_] => "FixedPoint"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp,c) match {
    case (FixPtType(sign,int,frac), c: FixedPoint) =>
      if(int > 32 | (!sign & int == 32)) s"""FixedPoint(BigDecimal("$c"),FixFormat($sign,$int,$frac))"""
      else s"""FixedPoint(BigDecimal("$c"),FixFormat($sign,$int,$frac))"""
    case _ => super.quoteConst(tp,c)
  }

  override def invalid(tp: Type[_]): String = tp match {
    case FixPtType(s,i,f) => src"FixedPoint.invalid(FixFormat($s,$i,$f))"
    case _ => super.invalid(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    //case FixInv(x)   =>
    //case FixNeg(x)   => 
    //case FixAdd(x,y) => 
    //case FixSub(x,y) => 
    //case FixMul(x,y) => 
    //case FixDiv(x,y) => 
    //case FixRecip(x) => emit(src"val $lhs = Number.recip($x)") //TODO
    //case FixMod(x,y) => 
    //case FixAnd(x,y) => // same as BitAnd in plasticine
    //case FixOr(x,y)  => // same as BitOr in plasticine
    //case FixLst(x,y) => 
    //case FixLeq(x,y) => 
    //case FixXor(x,y) => // same as BitXor in plasticine

    //case FixSLA(x,y) => 
    //case FixSRA(x,y) => 
    //case FixSRU(x,y) => 

    //case SatAdd(x,y) => 
    //case SatSub(x,y) => 
    //case SatMul(x,y) =>
    //case SatDiv(x,y) =>
    //case UnbMul(x,y) =>
    //case UnbDiv(x,y) =>
    //case UnbSatMul(x,y) =>
    //case UnbSatDiv(x,y) => 

    //case FixNeq(x,y) =>
    //case FixEql(x,y) =>

    //case FixMax(x,y) =>
    //case FixMin(x,y) =>

    case FixToFix(x, fmt) =>
      emit(src"val $lhs = $x.toFixedPoint(FixFormat(${fmt.sign},${fmt.ibits},${fmt.fbits}))") //TODO

    case FixToFlt(x, fmt) =>
      emit(src"val $lhs = $x.toFloatPoint(FltFormat(${fmt.mbits-1},${fmt.ebits}))") //TODO

    case FixToText(x) => emitDummy(lhs, rhs)

    case TextToFix(x, _) => emitDummy(lhs, rhs)

    //case FixRandom(Some(max)) =>

    //case FixRandom(None) =>

    //case FixAbs(x)     =>
    //case FixFloor(x)   =>
    //case FixCeil(x)    =>
    //case FixLn(x)      =>
    //case FixExp(x)     =>
    //case FixSqrt(x)    =>
    //case FixSin(x)     =>
    //case FixCos(x)     =>
    //case FixTan(x)     =>
    //case FixSinh(x)    =>
    //case FixCosh(x)    =>
    //case FixTanh(x)    =>
    //case FixAsin(x)    =>
    //case FixAcos(x)    =>
    //case FixAtan(x)    =>
    //case FixPow(x,exp) =>
    //case FixFMA(m1,m2,add) =>
    //case FixRecipSqrt(x)   =>
    //case FixSigmoid(x)     =>

    case _ => super.gen(lhs, rhs)
  }
}
