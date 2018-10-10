package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import emul.FixedPoint

trait PIRGenFixPt extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixInv(x)            => genOp(lhs, rhs)
    case FixNeg(x)            => genOp(lhs, rhs)
    case FixAdd(x,y)          => genOp(lhs, rhs)
    case FixSub(x,y)          => genOp(lhs, rhs)
    case FixMul(x,y)          => genOp(lhs, rhs)
    case FixDiv(x,y)          => genOp(lhs, rhs)
    case FixRecip(x)          => genOp(lhs, rhs)
    case FixMod(x,y)          => genOp(lhs, rhs)
    case FixAnd(x,y)          => genOp(lhs, rhs)
    case FixOr(x,y)           => genOp(lhs, rhs)
    case FixLst(x,y)          => genOp(lhs, rhs)
    case FixLeq(x,y)          => genOp(lhs, rhs)
    case FixXor(x,y)          => genOp(lhs, rhs)
    case FixSLA(x,y)          => genOp(lhs, rhs)
    case FixSRA(x,y)          => genOp(lhs, rhs)
    case FixSRU(x,y)          => genOp(lhs, rhs)
    case SatAdd(x,y)          => genOp(lhs, rhs)
    case SatSub(x,y)          => genOp(lhs, rhs)
    case SatMul(x,y)          => genOp(lhs, rhs)
    case SatDiv(x,y)          => genOp(lhs, rhs)
    case UnbMul(x,y)          => genOp(lhs, rhs)
    case UnbDiv(x,y)          => genOp(lhs, rhs)
    case UnbSatMul(x,y)       => genOp(lhs, rhs)
    case UnbSatDiv(x,y)       => genOp(lhs, rhs)
    case FixNeq(x,y)          => genOp(lhs, rhs)
    case FixEql(x,y)          => genOp(lhs, rhs)
    case FixMax(x,y)          => genOp(lhs, rhs)
    case FixMin(x,y)          => genOp(lhs, rhs)
    case FixToFix(x, fmt)     => genOp(lhs, rhs)
    case FixToFlt(x, fmt)     => genOp(lhs, rhs)
    case FixRandom(Some(max)) => genOp(lhs, rhs)
    case FixRandom(None)      => genOp(lhs, rhs)
    case FixAbs(x)            => genOp(lhs, rhs)
    case FixFloor(x)          => genOp(lhs, rhs)
    case FixCeil(x)           => genOp(lhs, rhs)
    case FixLn(x)             => genOp(lhs, rhs)
    case FixExp(x)            => genOp(lhs, rhs)
    case FixSqrt(x)           => genOp(lhs, rhs)
    case FixSin(x)            => genOp(lhs, rhs)
    case FixCos(x)            => genOp(lhs, rhs)
    case FixTan(x)            => genOp(lhs, rhs)
    case FixSinh(x)           => genOp(lhs, rhs)
    case FixCosh(x)           => genOp(lhs, rhs)
    case FixTanh(x)           => genOp(lhs, rhs)
    case FixAsin(x)           => genOp(lhs, rhs)
    case FixAcos(x)           => genOp(lhs, rhs)
    case FixAtan(x)           => genOp(lhs, rhs)
    case FixPow(x,exp)        => genOp(lhs, rhs)
    case FixFMA(m1,m2,add)    => genOp(lhs, rhs)
    case FixRecipSqrt(x)      => genOp(lhs, rhs)
    case FixSigmoid(x)        => genOp(lhs, rhs)

    case _ => super.genAccel(lhs, rhs)
  }

  override protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    //case FixInv(x)            => 
    //case FixNeg(x)            =>
    //case FixAdd(x,y)          =>
    //case FixSub(x,y)          =>
    //case FixMul(x,y)          =>
    //case FixDiv(x,y)          =>
    //case FixRecip(x)          =>
    //case FixMod(x,y)          =>
    //case FixAnd(x,y)          =>
    //case FixOr(x,y)           =>
    //case FixLst(x,y)          =>
    //case FixLeq(x,y)          =>
    //case FixXor(x,y)          =>
    //case FixSLA(x,y)          =>
    //case FixSRA(x,y)          =>
    //case FixSRU(x,y)          =>
    //case SatAdd(x,y)          =>
    //case SatSub(x,y)          =>
    //case SatMul(x,y)          =>
    //case SatDiv(x,y)          =>
    //case UnbMul(x,y)          =>
    //case UnbDiv(x,y)          =>
    //case UnbSatMul(x,y)       =>
    //case UnbSatDiv(x,y)       =>
    //case FixNeq(x,y)          =>
    //case FixEql(x,y)          =>
    //case FixMax(x,y)          =>
    //case FixMin(x,y)          =>
    //case FixToFix(x, fmt)     =>
    //case FixToFlt(x, fmt)     =>
    //case FixToText(x)         =>
    //case TextToFix(x, _)      =>
    //case FixRandom(Some(max)) =>
    //case FixRandom(None)      =>
    //case FixAbs(x)            => 
    //case FixFloor(x)          => 
    //case FixCeil(x)           => 
    //case FixLn(x)             => 
    //case FixExp(x)            => 
    //case FixSqrt(x)           => 
    //case FixSin(x)            => 
    //case FixCos(x)            => 
    //case FixTan(x)            => 
    //case FixSinh(x)           => 
    //case FixCosh(x)           => 
    //case FixTanh(x)           => 
    //case FixAsin(x)           => 
    //case FixAcos(x)           => 
    //case FixAtan(x)           => 
    //case FixPow(x,exp)        => 
    //case FixFMA(m1,m2,add)    => 
    //case FixRecipSqrt(x)      => 
    //case FixSigmoid(x)        => 
    case _ => super.genHost(lhs, rhs)
  }
}
