package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import emul.FloatPoint

trait PIRGenFltPt extends PIRCodegen {

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case FltFmt(m, e) => src"$m, $e"
    case arg => super.quoteOrRemap(arg)
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FltIsPosInf(x)       => genOp(lhs)
    case FltIsNegInf(x)       => genOp(lhs)
    case FltIsNaN(x)          => genOp(lhs)
    case FltNeg(x)            => genOp(lhs)
    case FltAdd(x,y)          => genOp(lhs)
    case FltSub(x,y)          => genOp(lhs)
    case FltMul(x,y)          => genOp(lhs)
    case FltDiv(x,y)          => genOp(lhs)
    case FltMod(x,y)          => genOp(lhs)
    case FltRecip(x)          => genOp(lhs)
    case FltLst(x,y)          => genOp(lhs)
    case FltLeq(x,y)          => genOp(lhs)
    case FltNeq(x,y)          => genOp(lhs)
    case FltEql(x,y)          => genOp(lhs)
    case FltMax(x,y)          => genOp(lhs)
    case FltMin(x,y)          => genOp(lhs)
    case FltToFlt(x, fmt)     => genOp(lhs, inputs=Some(Seq(x)))
    case FltToFix(x, fmt)     => genOp(lhs, inputs=Some(Seq(x)))
    //case TextToFlt(x,fmt)     => genOp(lhs)
    case FltToText(x,format)  => genOp(lhs, inputs=Some(Seq(x)))
    case FltRandom(Some(max)) => genOp(lhs)
    case FltRandom(None)      => genOp(lhs)
    case FltAbs(x)            => genOp(lhs)
    case FltFloor(x)          => genOp(lhs)
    case FltCeil(x)           => genOp(lhs)
    case FltLn(x)             => genOp(lhs)
    case FltExp(x)            => genOp(lhs)
    case FltSqrt(x)           => genOp(lhs)
    case FltSin(x)            => genOp(lhs)
    case FltCos(x)            => genOp(lhs)
    case FltTan(x)            => genOp(lhs)
    case FltSinh(x)           => genOp(lhs)
    case FltCosh(x)           => genOp(lhs)
    case FltTanh(x)           => genOp(lhs)
    case FltAsin(x)           => genOp(lhs)
    case FltAcos(x)           => genOp(lhs)
    case FltAtan(x)           => genOp(lhs)
    case FltPow(x,exp)        => genOp(lhs)
    case FltFMA(m1,m2,add)    => genOp(lhs)
    case FltRecipSqrt(x)      => genOp(lhs)
    case FltSigmoid(x)        => genOp(lhs)
    case _ => super.genAccel(lhs, rhs)
  }

}
