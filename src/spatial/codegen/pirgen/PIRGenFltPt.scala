package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import emul.FloatPoint

trait PIRGenFltPt extends PIRCodegen {

  //override protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = 

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FltIsPosInf(x)       => genOp(lhs, rhs)
    case FltIsNegInf(x)       => genOp(lhs, rhs)
    case FltIsNaN(x)          => genOp(lhs, rhs)
    case FltNeg(x)            => genOp(lhs, rhs)
    case FltAdd(x,y)          => genOp(lhs, rhs)
    case FltSub(x,y)          => genOp(lhs, rhs)
    case FltMul(x,y)          => genOp(lhs, rhs)
    case FltDiv(x,y)          => genOp(lhs, rhs)
    case FltMod(x,y)          => genOp(lhs, rhs)
    case FltRecip(x)          => genOp(lhs, rhs)
    case FltLst(x,y)          => genOp(lhs, rhs)
    case FltLeq(x,y)          => genOp(lhs, rhs)
    case FltNeq(x,y)          => genOp(lhs, rhs)
    case FltEql(x,y)          => genOp(lhs, rhs)
    case FltMax(x,y)          => genOp(lhs, rhs)
    case FltMin(x,y)          => genOp(lhs, rhs)
    case FltToFlt(x, fmt)     => genOp(lhs, rhs)
    case FltToFix(x, fmt)     => genOp(lhs, rhs)
    case TextToFlt(x,fmt)     => genOp(lhs, rhs)
    case FltToText(x)         => genOp(lhs, rhs)
    case FltRandom(Some(max)) => genOp(lhs, rhs)
    case FltRandom(None)      => genOp(lhs, rhs)
    case FltAbs(x)            => genOp(lhs, rhs)
    case FltFloor(x)          => genOp(lhs, rhs)
    case FltCeil(x)           => genOp(lhs, rhs)
    case FltLn(x)             => genOp(lhs, rhs)
    case FltExp(x)            => genOp(lhs, rhs)
    case FltSqrt(x)           => genOp(lhs, rhs)
    case FltSin(x)            => genOp(lhs, rhs)
    case FltCos(x)            => genOp(lhs, rhs)
    case FltTan(x)            => genOp(lhs, rhs)
    case FltSinh(x)           => genOp(lhs, rhs)
    case FltCosh(x)           => genOp(lhs, rhs)
    case FltTanh(x)           => genOp(lhs, rhs)
    case FltAsin(x)           => genOp(lhs, rhs)
    case FltAcos(x)           => genOp(lhs, rhs)
    case FltAtan(x)           => genOp(lhs, rhs)
    case FltPow(x,exp)        => genOp(lhs, rhs)
    case FltFMA(m1,m2,add)    => genOp(lhs, rhs)
    case FltRecipSqrt(x)      => genOp(lhs, rhs)
    case FltSigmoid(x)        => genOp(lhs, rhs)
    case _ => super.genAccel(lhs, rhs)
  }

}
