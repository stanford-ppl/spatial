package spatial.codegen.pirgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import emul.FixedPoint

trait PIRGenFixPt extends PIRCodegen {

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case arg:BOOL[_] => s"Const(${arg.v})"
    case arg:INT[_] => s"Const(${arg.v})"
    case FixFmt(s, i, f) => src"$s, $i, $f"
    case arg => super.quoteOrRemap(arg)
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixInv(x)            => genOp(lhs)
    case FixNeg(x)            => genOp(lhs)
    case FixAdd(x,y)          => genOp(lhs)
    case FixSub(x,y)          => genOp(lhs)
    case FixMul(x,y)          => genOp(lhs)
    case FixDiv(x,y)          => genOp(lhs)
    case FixRecip(x)          => genOp(lhs)
    case FixMod(x,y)          => genOp(lhs)
    case FixAnd(x,y)          => genOp(lhs)
    case FixOr(x,y)           => genOp(lhs)
    case FixLst(x,y)          => genOp(lhs)
    case FixLeq(x,y)          => genOp(lhs)
    case FixXor(x,y)          => genOp(lhs)
    case FixSLA(x,y)          => genOp(lhs)
    case FixSRA(x,y)          => genOp(lhs)
    case FixSRU(x,y)          => genOp(lhs)
    case SatAdd(x,y)          => genOp(lhs)
    case SatSub(x,y)          => genOp(lhs)
    case SatMul(x,y)          => genOp(lhs)
    case SatDiv(x,y)          => genOp(lhs)
    case UnbMul(x,y)          => genOp(lhs)
    case UnbDiv(x,y)          => genOp(lhs)
    case UnbSatMul(x,y)       => genOp(lhs)
    case UnbSatDiv(x,y)       => genOp(lhs)
    case FixNeq(x,y)          => genOp(lhs)
    case FixEql(x,y)          => genOp(lhs)
    case FixMax(x,y)          => genOp(lhs)
    case FixMin(x,y)          => genOp(lhs)
    case FixToFix(x, fmt)     => genOp(lhs, inputs=Some(Seq(x)))
    case FixToFlt(x, fmt)     => genOp(lhs, inputs=Some(Seq(x)))
    case FixToFixUnb(x, fmt)  => genOp(lhs, inputs=Some(Seq(x)))
    case FixToFixSat(x, fmt)  => genOp(lhs, inputs=Some(Seq(x)))
    case FixToText(x)         => genOp(lhs)
    //case TextToFix(x, _) =>
    case FixRandom(Some(max)) => genOp(lhs)
    case FixRandom(None)      => genOp(lhs)
    case FixAbs(x)            => genOp(lhs)
    case FixFloor(x)          => genOp(lhs)
    case FixCeil(x)           => genOp(lhs)
    case FixLn(x)             => genOp(lhs)
    case FixExp(x)            => genOp(lhs)
    case FixSqrt(x)           => genOp(lhs)
    case FixSin(x)            => genOp(lhs)
    case FixCos(x)            => genOp(lhs)
    case FixTan(x)            => genOp(lhs)
    case FixSinh(x)           => genOp(lhs)
    case FixCosh(x)           => genOp(lhs)
    case FixTanh(x)           => genOp(lhs)
    case FixAsin(x)           => genOp(lhs)
    case FixAcos(x)           => genOp(lhs)
    case FixAtan(x)           => genOp(lhs)
    case FixPow(x,exp)        => genOp(lhs)
    case FixFMA(m1,m2,add)    => genOp(lhs)
    case FixRecipSqrt(x)      => genOp(lhs)
    case FixSigmoid(x)        => genOp(lhs)

    case _ => super.genAccel(lhs, rhs)
  }

}
