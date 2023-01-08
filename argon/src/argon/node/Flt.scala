package argon.node

import forge.tags._
import argon._
import argon.lang._

import emul.{FloatPoint, Number}

abstract class FltOp[M:INT,E:INT,R:Type] extends Primitive[R] {
  lazy val fmt: FltFmt[M,E] = FltFmt.from[M,E]
}
abstract class FltToFltOp[M:INT,E:INT] extends FltOp[M,E,Flt[M,E]]

abstract class UnaryFltToFltOp[M: INT, E: INT](override val unstaged: FloatPoint => FloatPoint) extends FltToFltOp[M, E] with Unary[FloatPoint, Flt[M, E]]
abstract class BinaryFltToFltOp[M: INT, E: INT](override val unstaged: (FloatPoint, FloatPoint) => FloatPoint) extends FltToFltOp[M, E] with Binary[FloatPoint, Flt[M, E]]
abstract class FltComparison[M:INT, E:INT](override val unstaged: (FloatPoint, FloatPoint) => emul.Bool) extends FltOp[M, E, Bit] with Comparison[FloatPoint, Flt[M, E]]

@op case class FltIsPosInf[M:INT,E:INT](a: Flt[M,E]) extends FltOp[M,E,Bit]
@op case class FltIsNegInf[M:INT,E:INT](a: Flt[M,E]) extends FltOp[M,E,Bit]
@op case class FltIsNaN[M:INT,E:INT](a: Flt[M,E]) extends FltOp[M,E,Bit]

@op case class FltNeg[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](a => -a)
@op case class FltAdd[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends BinaryFltToFltOp[M,E](_ + _)
@op case class FltSub[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends BinaryFltToFltOp[M,E](_ - _)
@op case class FltMul[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends BinaryFltToFltOp[M,E](_ * _)
@op case class FltFMA[M:INT,E:INT](m0: Flt[M,E], m1: Flt[M,E], add: Flt[M,E]) extends FltToFltOp[M,E]
@op case class FltDiv[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends BinaryFltToFltOp[M,E](_ / _)
@op case class FltMod[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends BinaryFltToFltOp[M,E](_ % _)
@op case class FltLst[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltComparison[M, E](_ < _)
@op case class FltLeq[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltComparison[M, E](_ <= _)
@op case class FltEql[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltComparison[M, E](_ === _)
@op case class FltNeq[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltComparison[M, E](_ !== _)

@op case class FltMin[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends BinaryFltToFltOp[M,E](Number.min)
@op case class FltMax[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends BinaryFltToFltOp[M,E](Number.max)
@op case class FltAbs[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.abs)
@op case class FltCeil[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.ceil)
@op case class FltFloor[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.floor)
@op case class FltPow[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends BinaryFltToFltOp[M,E](Number.pow) {
  @rig override def rewrite: Flt[M,E] = (a,b) match {
    case (_, Literal(0))    => R.from(1)
    case (Literal(1), _)    => a
    case (_, Literal(1))    => a
    case (_, Literal(2))    => a * a
    case (_, Literal(0.5))  => stage(FltSqrt(a))
    case (_, Literal(-0.5)) => stage(FltRecipSqrt(a))
    case (_, Literal(-1))   => stage(FltRecip(a))
    case _ => super.rewrite
  }
}

@op case class FltExp[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.exp)
@op case class FltLn[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.ln)
@op case class FltSqrt[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.sqrt)

@op case class FltSin[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.sin)
@op case class FltCos[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.cos)
@op case class FltTan[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.tan)
@op case class FltSinh[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.sinh)
@op case class FltCosh[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.cosh)
@op case class FltTanh[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.tanh)
@op case class FltAsin[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.asin)
@op case class FltAcos[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.acos)
@op case class FltAtan[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.atan)

@op case class FltRecip[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.recip) {
  @rig override def rewrite: Flt[M,E] = a match {
    case Op(FltRecip(x)) => x
    case _ => super.rewrite
  }
}
@op case class FltRecipSqrt[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.recipSqrt)
@op case class FltSigmoid[M:INT,E:INT](a: Flt[M,E]) extends UnaryFltToFltOp[M,E](Number.sigmoid)

@op case class FltToFlt[M1:INT,E1:INT,M2:INT,E2:INT](
    a: Flt[M1,E1],
    f2: FltFmt[M2,E2])
  extends FltOp[M1,E1,Flt[M2,E2]]

@op case class FltToFix[M1:INT,E1:INT,S2:BOOL,I2:INT,F2:INT](
    a:  Flt[M1,E1],
    f2: FixFmt[S2,I2,F2])
  extends FltOp[M1,E1,Fix[S2,I2,F2]]

@op case class TextToFlt[M:INT,E:INT](t: Text, fm: FltFmt[M,E]) extends FltToFltOp[M,E] {
  override val canAccel: Boolean = true
}
@op case class FltToText[M:INT,E:INT](a: Flt[M,E], format:Option[String]) extends FltOp[M,E,Text] {
  override val canAccel: Boolean = true
}

@op case class FltRandom[M:INT,E:INT](max: Option[Flt[M,E]]) extends FltToFltOp[M,E] {
  override def effects: Effects = Effects.Simple
}
