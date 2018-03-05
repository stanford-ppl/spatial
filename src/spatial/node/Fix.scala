package spatial.node

import forge.tags._
import core._
import spatial.lang._

abstract class FixOp2[S:BOOL,I:INT,F:INT,R:Type] extends Primitive[R] {
  lazy val fmt: FixFmt[S,I,F] = FixFmt[S,I,F]
}
abstract class FixOp1[S:BOOL,I:INT,F:INT] extends FixOp2[S,I,F,Fix[S,I,F]]

/** Fix **/
@op case class FixNeg[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixInv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixAnd[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixOr[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixXor[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixAdd[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixSub[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixMul[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixDiv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixMod[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixSLA[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixSRA[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixSRU[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixLst[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp2[S,I,F,Bit]
@op case class FixLeq[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp2[S,I,F,Bit]
@op case class FixEql[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp2[S,I,F,Bit]
@op case class FixNeq[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp2[S,I,F,Bit]

@op case class FixMin[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixMax[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixAbs[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixCeil[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixFloor[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixPow[S:BOOL,I:INT,F:INT](b: Fix[S,I,F], e: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixExp[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixLn[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixSqrt[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]

@op case class FixSin[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixCos[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixTan[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixSinh[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixCosh[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixTanh[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixAsin[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixAcos[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixAtan[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]

@op case class FixInvSqrt[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class FixSigmoid[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp1[S,I,F]

@op case class FixToFix[S1:BOOL,I1:INT,F1:INT,S2:BOOL,I2:INT,F2:INT](
    a:  Fix[S1,I1,F1],
    f2: FixFmt[S2,I2,F2])
  extends FixOp2[S1,I1,F1,Fix[S2,I2,F2]] {
  override val isTransient = true
}
@op case class FixToFlt[S1:BOOL,I1:INT,F1:INT,M2:INT,E2:INT](
    a:  Fix[S1,I1,F1],
    f2: FltFmt[M2,E2])
  extends FixOp2[S1,I1,F1,Flt[M2,E2]]

@op case class TextToFix[S:BOOL,I:INT,F:INT](t: Text, f: FixFmt[S,I,F]) extends FixOp1[S,I,F] {
  override val debugOnly: Boolean = true
}
@op case class FixToText[S:BOOL,I:INT,F:INT](a: Fix[S,I,F]) extends FixOp2[S,I,F,Text] {
  override val debugOnly: Boolean = true
}

@op case class FixRandom[S:BOOL,I:INT,F:INT](max: Option[Fix[S,I,F]]) extends FixOp1[S,I,F] {
  override def effects: Effects = Effects.Simple
}


/** Saturating and Unbiased Rounding Math **/
@op case class SatAdd[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class SatSub[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class SatMul[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class SatDiv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class UnbMul[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class UnbDiv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class UnbSatMul[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
@op case class UnbSatDiv[S:BOOL,I:INT,F:INT](a: Fix[S,I,F], b: Fix[S,I,F]) extends FixOp1[S,I,F]
