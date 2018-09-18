package argon.node

import argon._
import argon.lang._

import emul.{FixedPoint,Number}
import forge.tags._

abstract class FixOp[S:BOOL,I:INT,F:INT,R:Type] extends Primitive[R] {
  lazy val fmt: FixFmt[S,I,F] = FixFmt.from[S,I,F]
}
abstract class FixOp1[S:BOOL,I:INT,F:INT] extends FixOp[S,I,F,Fix[S,I,F]]
abstract class FixBinary[S:BOOL,I:INT,F:INT](
    override val unstaged: (FixedPoint, FixedPoint) => FixedPoint)
  extends FixOp1[S,I,F]
     with Binary[FixedPoint,Fix[S,I,F]]

abstract class FixUnary[S:BOOL,I:INT,F:INT](
    override val unstaged: FixedPoint => FixedPoint)
  extends FixOp1[S,I,F]
     with Unary[FixedPoint,Fix[S,I,F]]

/** Negation of a fixed point value */
@op case class FixNeg[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](a => -a) {
  @rig override def rewrite: Fix[S,I,F] = a match {
    case Op(FixNeg(x)) => x
    case _ => super.rewrite
  }
}

/** Bitwise inversion of a fixed point value */
@op case class FixInv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](a => ~a) {
  @rig override def rewrite: Fix[S, I, F] = a match {
    case Op(FixInv(x)) => x
    case _ => super.rewrite
  }
}

/** Bitwise AND of two fixed point values */
@op case class FixAnd[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_&_) {
  override def absorber: Option[Fix[S,I,F]] = Some(R.uconst(0))
  override def identity: Option[Fix[S,I,F]] = Some(a.ones)
  override def isAssociative: Boolean = true
}

/** Bitwise OR of two fixed point values */
@op case class FixOr[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_|_) {
  override def absorber: Option[Fix[S,I,F]] = Some(a.ones)
  override def identity: Option[Fix[S,I,F]] = Some(R.uconst(0))
  override def isAssociative: Boolean = true
}

/** Bitwise XOR of two fixed point values */
@op case class FixXor[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_^_) {
  override def absorber: Option[Fix[S,I,F]] = Some(a.ones)
  override def identity: Option[Fix[S,I,F]] = Some(R.uconst(0))
  override def isAssociative: Boolean = true
}

/** Fixed point addition */
@op case class FixAdd[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_+_) {
  override def identity: Option[Fix[S,I,F]] = Some(R.uconst(0))
  override def isAssociative: Boolean = true

  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (Op(FixSub(x,c)), _) if c == b => x  // (x - b) + b = x
    case (_, Op(FixSub(x,c))) if c == a => x  // a + (x - a) = x
    case _ => super.rewrite
  }
}

/** Fixed point subtraction */
@op case class FixSub[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_-_) {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (_, Literal(0)) => a
    case (Literal(0), _) => -b

    case (Op(FixAdd(x,c)),_) if c == b => x  // (x + b) - b = x
    case (Op(FixAdd(c,x)),_) if c == b => x  // (b + x) - b = x

    case (_,Op(FixAdd(x,c))) if c == a => -x // a - (x + a) = -x
    case (_,Op(FixAdd(c,x))) if c == a => -x // a - (a + x) = -x
    case _ => super.rewrite
  }
}

/** Fixed point multiplication */
@op case class FixMul[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_*_) {
  override def absorber: Option[Fix[S,I,F]] = Some(R.uconst(0))
  override def identity: Option[Fix[S,I,F]] = Some(R.uconst(1))
  override def isAssociative: Boolean = true
}

/** Fixed fused multiply add */
@op case class FixFMA[S:BOOL,I:INT,F:INT](m0: Fix[S,I,F], m1: Fix[S,I,F], add: Fix[S,I,F]) extends FixOp1[S,I,F]

/** Fixed point division */
@op case class FixDiv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_/_) {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (_, Literal(0)) if state.isStaging =>
      warn(ctx, s"Constant division by 0")
      warn(ctx)
      null
    case (Const(q), Const(r)) => R.from(q/r)
    case (_, Literal(1)) => a
    case (Literal(0), _) => a
    case (Literal(1), _) => stage(FixRecip(b))
    case (_, Const(r)) if r.isPow2 && r > 0 => a >> Type[Fix[S,I,_0]].from(Number.log2(r))
    case (_, Const(r)) if r.isPow2 && r < 0 => -a >> Type[Fix[S,I,_0]].from(Number.log2(-r))
    case _ => super.rewrite
  }
}

/** Fixed point modulus */
@op case class FixMod[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_%_) {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (_, Literal(0)) if state.isStaging =>
      warn(ctx, s"Constant modulus by 0")
      warn(ctx)
      null
    case (Literal(0), _) => a
    case _ => super.rewrite
  }
}

/** Fixed point arithmetic shift left */
@op case class FixSLA[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,_0]) extends FixOp1[S,I,F] {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (Const(x), Const(y)) => R.from(x << y)
    case (x, Literal(0))      => x
    case _ => super.rewrite
  }
}

/** Fixed point arithmetic shift right */
@op case class FixSRA[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,_0]) extends FixOp1[S,I,F] {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (Const(x), Const(y)) => R.from(x >> y)
    case (x, Literal(0))      => x
    case _ => super.rewrite
  }
}

/** Fixed point logical (unsigned) shift right */
@op case class FixSRU[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,_0]) extends FixOp1[S,I,F] {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (Const(x), Const(y)) => R.from(x >>> y)
    case (x, Literal(0))      => x
    case _ => super.rewrite
  }
}

/** Fixed point less than comparison */
@op case class FixLst[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp[S,I,F,Bit] {
  @rig override def rewrite: Bit = (a,b) match {
    case (Const(x), Const(y)) => R.from(x < y)
    case _ => super.rewrite
  }
}

/** Fixed point less than or equal comparison */
@op case class FixLeq[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp[S,I,F,Bit] {
  @rig override def rewrite: Bit = (a,b) match {
    case (Const(x), Const(y)) => R.from(x <= y)
    case _ => super.rewrite
  }
}

/** Fixed point equality comparison */
@op case class FixEql[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp[S,I,F,Bit] {
  @rig override def rewrite: Bit = (a,b) match {
    case (Const(x), Const(y)) => R.from(x === y)
    case _ => super.rewrite
  }
}

/** Fixed point inequality comparison */
@op case class FixNeq[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp[S,I,F,Bit] {
  @rig override def rewrite: Bit = (a,b) match {
    case (Const(x), Const(y)) => R.from(x !== y)
    case _ => super.rewrite
  }
}

/** Fixed point select minimum */
@op case class FixMin[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](Number.min) {
  override def absorber: Option[Fix[S,I,F]] = Some(a.minValue)
  override def identity: Option[Fix[S,I,F]] = Some(a.maxValue)
  override def isAssociative: Boolean = true
}

/** Fixed point select maximum */
@op case class FixMax[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](Number.max) {
  override def absorber: Option[Fix[S,I,F]] = Some(a.maxValue)
  override def identity: Option[Fix[S,I,F]] = Some(a.minValue)
  override def isAssociative: Boolean = true
}

/** Fixed point absolute value */
@op case class FixAbs[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.abs) {
  @rig override def rewrite: Fix[S,I,F] = a match {
    case x if !BOOL[S].v => x // Unsigned absolute value
    case _ => super.rewrite
  }
}

/** Fixed point ceiling (round away from 0) */
@op case class FixCeil[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.ceil) {
  @rig override def rewrite: Fix[S,I,F] = a match {
    case x if INT[F].v == 0 => x // Ceiling of integer
    case _ => super.rewrite
  }
}

/** Fixed point floor (round towards 0) */
@op case class FixFloor[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.floor) {
  @rig override def rewrite: Fix[S,I,F] = a match {
    case x if INT[F].v == 0 => x // floor of integer
    case _ => super.rewrite
  }
}

/** Fixed point power (a raised to power b) */
@op case class FixPow[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](Number.pow) {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (_, Literal(0))    => R.from(1)
    case (Literal(1), _)    => a
    case (_, Literal(1))    => a
    case (_, Literal(2))    => a * a
    case (_, Literal(0.5))  => stage(FixSqrt(a))
    case (_, Literal(-0.5)) => stage(FixRecipSqrt(a))
    case (_, Literal(-1))   => stage(FixInv(a))
    case _ => super.rewrite
  }
}

/** Fixed point exponential (Euler's number raised to power a) */
@op case class FixExp[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.exp)

/** Fixed point natural log */
@op case class FixLn[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.ln)

/** Fixed point square root */
@op case class FixSqrt[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.sqrt)

/** Fixed point sine */
@op case class FixSin[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.sin)

/** Fixed point cosine */
@op case class FixCos[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.cos)

/** Fixed point tangent */
@op case class FixTan[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.tan)

/** Fixed point hyperbolic sine */
@op case class FixSinh[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.sinh)

/** Fixed point hyperbolic cosine */
@op case class FixCosh[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.cosh)

/** Fixed point hyperbolic tangent */
@op case class FixTanh[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.tanh)

@op case class FixAsin[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.asin)
@op case class FixAcos[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.acos)
@op case class FixAtan[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.atan)

/** Fixed point reciprocal **/
@op case class FixRecip[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.recip)

/** Fixed point inverse square root */
@op case class FixRecipSqrt[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.recipSqrt)

/** Fixed point sigmoid */
@op case class FixSigmoid[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixUnary[S,I,F](Number.sigmoid)

/** Fixed point type conversion */
@op case class FixToFix[S1:BOOL,I1:INT,F1:INT,S2:BOOL,I2:INT,F2:INT](
    a:  Fix[S1,I1,F1],
    f2: FixFmt[S2,I2,F2])
  extends FixOp[S1,I1,F1,Fix[S2,I2,F2]] {
  override val isTransient = true
  @rig override def rewrite :Fix[S2,I2,F2] = (a,f2) match {
    case (Const(c),_) => const[Fix[S2,I2,F2]](c.toFixedPoint(f2.toEmul))
    case (_, fmt2) if fmt2 == a.fmt => a.asInstanceOf[Fix[S2,I2,F2]]
    case _ => super.rewrite
  }
}

@op case class FixToFixSat[S1:BOOL,I1:INT,F1:INT,S2:BOOL,I2:INT,F2:INT](
    a:  Fix[S1,I1,F1],
    f2: FixFmt[S2,I2,F2])
  extends FixOp[S1,I1,F1,Fix[S2,I2,F2]] {
  override val isTransient = true
  @rig override def rewrite :Fix[S2,I2,F2] = (a,f2) match {
    case (Const(c),_) => const[Fix[S2,I2,F2]](c.toFixedPoint(f2.toEmul))
    case (_, fmt2) if fmt2 == a.fmt => a.asInstanceOf[Fix[S2,I2,F2]]
    case _ => super.rewrite
  }
}

@op case class FixToFixUnb[S1:BOOL,I1:INT,F1:INT,S2:BOOL,I2:INT,F2:INT](
    a:  Fix[S1,I1,F1],
    f2: FixFmt[S2,I2,F2])
  extends FixOp[S1,I1,F1,Fix[S2,I2,F2]] {
  override val isTransient = true
  @rig override def rewrite :Fix[S2,I2,F2] = (a,f2) match {
    case (Const(c),_) => const[Fix[S2,I2,F2]](c.toFixedPoint(f2.toEmul))
    case (_, fmt2) if fmt2 == a.fmt => a.asInstanceOf[Fix[S2,I2,F2]]
    case _ => super.rewrite
  }
}

@op case class FixToFixUnbSat[S1:BOOL,I1:INT,F1:INT,S2:BOOL,I2:INT,F2:INT](
    a:  Fix[S1,I1,F1],
    f2: FixFmt[S2,I2,F2])
  extends FixOp[S1,I1,F1,Fix[S2,I2,F2]] {
  override val isTransient = true
  @rig override def rewrite :Fix[S2,I2,F2] = (a,f2) match {
    case (Const(c),_) => const[Fix[S2,I2,F2]](c.toFixedPoint(f2.toEmul))
    case (_, fmt2) if fmt2 == a.fmt => a.asInstanceOf[Fix[S2,I2,F2]]
    case _ => super.rewrite
  }
}

/** Fixed point type conversion to floating point */
@op case class FixToFlt[S1:BOOL,I1:INT,F1:INT,M2:INT,E2:INT](
    a:  Fix[S1,I1,F1],
    f2: FltFmt[M2,E2])
  extends FixOp[S1,I1,F1,Flt[M2,E2]] {
  @rig override def rewrite: Flt[M2,E2] = a match {
    case Const(c) => const[Flt[M2,E2]](c.toFloatPoint(f2.toEmul))
    case _ => super.rewrite
  }
}

/** Fixed point parsing from text */
@op case class TextToFix[S:BOOL,I:INT,F:INT](t: Text, f: FixFmt[S,I,F]) extends FixOp1[S,I,F] {
  override val canAccel: Boolean = true

  @rig override def rewrite: Fix[S,I,F] = t match {
    case Const(str) => R.from(FixedPoint(str,f.toEmul))
    case _ => super.rewrite
  }
}

/** Fixed point conversion to text */
@op case class FixToText[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp[S,I,F,Text] {
  override val canAccel: Boolean = true

  @rig override def rewrite: Text = a match {
    case Const(c) => R.from(c.toString)
    case _ => super.rewrite
  }
}

@op case class FixRandom[S:BOOL,I:INT,F:INT](max: Option[Fix[S,I,F]]) extends FixOp1[S,I,F] {
  override def effects: Effects = Effects.Simple
}


/** Saturating and Unbiased Rounding Math */
// TODO[5]: Is saturating and unbiased math associative?
@op case class SatAdd[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_+!_) {
  override def identity: Option[Fix[S, I, F]] = Some(R.uconst(0))
}
@op case class SatSub[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_-!_) {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (_, Literal(0)) => a
    case (Literal(0), _) => -b
    case _ => super.rewrite
  }
}
@op case class SatMul[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_*!_) {
  override def absorber: Option[Fix[S,I,F]] = Some(R.uconst(0))
  override def identity: Option[Fix[S,I,F]] = Some(R.uconst(1))
}
@op case class SatDiv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_/!_) {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (_, Literal(0)) if state.isStaging =>
      warn(ctx, "Constant division by zero")
      warn(ctx)
      null
    case (_, Literal(1)) => a
    case (Literal(0), _) => a
    case _ => super.rewrite
  }
}
@op case class UnbMul[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_*&_) {
  override def absorber: Option[Fix[S,I,F]] = Some(R.uconst(0))
  override def identity: Option[Fix[S,I,F]] = Some(R.uconst(1))
}
@op case class UnbDiv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_/&_) {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (_, Literal(0)) if state.isStaging =>
      warn(ctx, "Constant division by zero")
      warn(ctx)
      null
    case (_, Literal(1)) => a
    case (Literal(0), _) => a
    case _ => super.rewrite
  }
}
@op case class UnbSatMul[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_*&!_) {
  override def absorber: Option[Fix[S,I,F]] = Some(R.uconst(0))
  override def identity: Option[Fix[S,I,F]] = Some(R.uconst(1))
}
@op case class UnbSatDiv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixBinary[S,I,F](_/&!_) {
  @rig override def rewrite: Fix[S,I,F] = (a,b) match {
    case (_, Literal(0)) if state.isStaging =>
      warn(ctx, "Constant division by zero")
      warn(ctx)
      null
    case (_, Literal(1)) => a
    case (Literal(0), _) => a
    case _ => super.rewrite
  }
}
