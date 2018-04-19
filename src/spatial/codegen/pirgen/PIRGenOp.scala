package spatial.codegen.pirgen

import argon._
import spatial.data._
import spatial.lang._
import spatial.node._

import emul.Bool

trait PIRGenOp extends PIRCodegen {

  def isInnerReduce(lhs:Sym[_], rhs:Op[_]):Boolean = {//dbgblk[Boolean](s"isInnerReduce($lhs)"){
    val inputs = rhs.expInputs
    dbgs(src"reduceType=${reduceType(lhs)} inputs=${inputs}")
    //reduceType(lhs).isDefined && inputs.exists(in => isReduceStarter(in))
    false
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = nodeToOp(rhs) match {
    case Some(op) if inHw =>
        val inputs = rhs.productIterator.toList
        emit(lhs, src"OpDef(op=$op, inputs=${inputs.map(quote)})", rhs)
    case _ => super.gen(lhs, rhs)
  }

  def nodeToOp(node: Op[_]): Option[String] = node match {
    case Mux(_,_,_)        => Some("MuxOp")
    case FixAdd(_,_)       => Some("FixAdd")
    case FixSub(_,_)       => Some("FixSub")
    case FixMul(_,_)       => Some("FixMul")
    case FixDiv(_,_)       => Some("FixDiv")
    case FixMod(_,_)       => Some("FixMod")
    case FixLst(_,_)       => Some("FixLt")
    case FixLeq(_,_)       => Some("FixLeq")
    case FixEql(_,_)       => Some("FixEql")
    case FixNeq(_,_)       => Some("FixNeq")
    case FixSLA(_,_)       => Some("FixSla")
    case FixSRA(_,_)       => Some("FixSra")
    case FixSRU(_,_)       => Some("FixUsra") //TODO change this name in pir
    case FixMax(x,y)       => Some("FixMin")
    case FixMin(x,y)       => Some("FixMax")
    case FixNeg(_)         => Some("FixNeg")
    case FixInv(_)         => Some("FixInv")
    case FixRandom(_)      => Some("FixRandom")
    case FixAbs(_)         => Some("FixAbs")
    case FixFloor(_)       => Some("FixFloor")
    case FixCeil(_)        => Some("FixCeil")
    case FixLn(_)          => Some("FixLn")
    case FixExp(_)         => Some("FixExp")
    case FixSqrt(_)        => Some("FixSqrt")
    case FixSin(_)         => Some("FixSin")
    case FixCos(_)         => Some("FixCos")
    case FixTan(_)         => Some("FixTan")
    case FixSinh(_)        => Some("FixSinh")
    case FixCosh(_)        => Some("FixCosh")
    case FixTanh(_)        => Some("FixTanh")
    case FixAsin(_)        => Some("FixAsin")
    case FixAcos(_)        => Some("FixAcos")
    case FixAtan(_)        => Some("FixAtan")
    case FixPow(_,_)       => Some("FixPow")
    case FixFMA(_,_,_)     => Some("FixFMA")
    case FixRecipSqrt(_)   => Some("FixRecipSqrt")
    case FixSigmoid(_)     => Some("FixSigmoid")

    case SatAdd(_,_)       => Some("SatAdd")
    case SatSub(_,_)       => Some("SatSub")
    case SatMul(_,_)       => Some("SatMul")
    case SatDiv(_,_)       => Some("SatDiv")

    case UnbMul(_,_)       => Some("UnbMul")
    case UnbDiv(_,_)       => Some("UnbDiv")
    case UnbSatMul(_,_)    => Some("UnbSatMul")
    case UnbSatDiv(_,_)    => Some("UnbSatDiv")

    case FltNeg(x)         => Some("FltNeg")
    case FltAdd(x,y)       => Some("FltAdd")
    case FltSub(x,y)       => Some("FltSub")
    case FltMul(x,y)       => Some("FltMul")
    case FltDiv(x,y)       => Some("FltDiv")
    case FltRecip(x)       => Some("FltRecip")
    case FltLst(x,y)       => Some("FltLst")
    case FltLeq(x,y)       => Some("FltLeq")

    case FltNeq(x,y)       => Some("FltNeq")
    case FltEql(x,y)       => Some("FltEql")

    case FltMax(x,y)       => Some("FltMax")
    case FltMin(x,y)       => Some("FltMin")
    case FltRandom(None)   => Some("FltRandom")

    case FltAbs(x)         => Some("FltAbs")
    case FltFloor(x)       => Some("FltFloor")
    case FltCeil(x)        => Some("FltCeil")
    case FltLn(x)          => Some("FltLn")
    case FltExp(x)         => Some("FltExp")
    case FltSqrt(x)        => Some("FltSqrt")
    case FltSin(x)         => Some("FltSin")
    case FltCos(x)         => Some("FltCos")
    case FltTan(x)         => Some("FltTan")
    case FltSinh(x)        => Some("FltSinh")
    case FltCosh(x)        => Some("FltCosh")
    case FltTanh(x)        => Some("FltTanh")
    case FltAsin(x)        => Some("FltAsin")
    case FltAcos(x)        => Some("FltAcos")
    case FltAtan(x)        => Some("FltAtan")
    case FltPow(x,exp)     => Some("FltPow")
    case FltFMA(m1,m2,add) => Some("FltFMA")
    case FltRecipSqrt(x)   => Some("FltRecipSqrt")
    case FltSigmoid(x)     => Some("FltSigmoid")

    case Not(_)            => Some("BitNot")
    case And(_,_)          => Some("BitAnd")
    case FixAnd(_,_)       => Some("BitAnd")
    case Or(_,_)           => Some("BitOr")
    case Xor(_,_)          => Some("BitXor")
    case Xnor(_,_)         => Some("BitXnor")
    case BitRandom(_)      => Some("BitRandom")
    case _                 => None
  }
}
