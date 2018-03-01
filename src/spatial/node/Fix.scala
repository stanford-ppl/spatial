package spatial.node

import forge.tags._
import core._
import spatial.lang._

abstract class FixOp2[F:FixFmt,R:Type] extends Primitive[R] {
  lazy val fmt: FixFmt[F] = FixFmt[F]
}
abstract class FixOp1[F:FixFmt] extends FixOp2[F,Fix[F]]

/** Fix **/
@op case class FixNeg[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixInv[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixAnd[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixOr[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixXor[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixAdd[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixSub[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixMul[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixDiv[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixMod[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixSLA[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixSRA[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixSRU[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixLst[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp2[F,Bit]
@op case class FixLeq[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp2[F,Bit]
@op case class FixEql[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp2[F,Bit]
@op case class FixNeq[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp2[F,Bit]

@op case class FixMin[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixMax[F:FixFmt](a: Fix[F], b: Fix[F]) extends FixOp1[F]
@op case class FixAbs[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixCeil[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixFloor[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixPow[F:FixFmt](b: Fix[F], e: Fix[F]) extends FixOp1[F]
@op case class FixExp[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixLn[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixSqrt[F:FixFmt](a: Fix[F]) extends FixOp1[F]

@op case class FixSin[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixCos[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixTan[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixSinh[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixCosh[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixTanh[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixAsin[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixAcos[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixAtan[F:FixFmt](a: Fix[F]) extends FixOp1[F]

@op case class FixInvSqrt[F:FixFmt](a: Fix[F]) extends FixOp1[F]
@op case class FixSigmoid[F:FixFmt](a: Fix[F]) extends FixOp1[F]

@op case class FixToFix[F1:FixFmt,F2:FixFmt](a: Fix[F1], f2: FixFmt[F2]) extends FixOp2[F1,Fix[F2]]
@op case class FixToFlt[F1:FixFmt,F2:FltFmt](a: Fix[F1], f2: FltFmt[F2]) extends FixOp2[F1,Flt[F2]]

@op case class TextToFix[F:FixFmt](t: Text, f: FixFmt[F]) extends FixOp1[F] {
  override val debugOnly: Boolean = true
}
@op case class FixToText[F:FixFmt](a: Fix[F]) extends FixOp2[F,Text] {
  override val debugOnly: Boolean = true
}

@op case class FixRandom[F:FixFmt](max: Option[Fix[F]]) extends FixOp1[F] {
  override def effects: Effects = Effects.Simple
}
