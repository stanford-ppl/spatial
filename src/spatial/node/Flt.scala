package spatial.node

import forge.tags._
import core._
import spatial.lang._

abstract class FltOp2[F:FltFmt,R:Type] extends Primitive[R] {
  lazy val fmt: FltFmt[F] = FltFmt[F]
}
abstract class FltOp1[F:FltFmt] extends FltOp2[F,Flt[F]]

@op case class FltNeg[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltAdd[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp1[F]
@op case class FltSub[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp1[F]
@op case class FltMul[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp1[F]
@op case class FltDiv[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp1[F]
@op case class FltMod[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp1[F]
@op case class FltLst[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp2[F,Bit]
@op case class FltLeq[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp2[F,Bit]
@op case class FltEql[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp2[F,Bit]
@op case class FltNeq[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp2[F,Bit]

@op case class FltMin[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp1[F]
@op case class FltMax[F:FltFmt](a: Flt[F], b: Flt[F]) extends FltOp1[F]
@op case class FltAbs[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltCeil[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltFloor[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltPow[F:FltFmt](b: Flt[F], e: Flt[F]) extends FltOp1[F]
@op case class FltExp[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltLn[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltSqrt[F:FltFmt](a: Flt[F]) extends FltOp1[F]

@op case class FltSin[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltCos[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltTan[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltSinh[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltCosh[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltTanh[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltAsin[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltAcos[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltAtan[F:FltFmt](a: Flt[F]) extends FltOp1[F]

@op case class FltInvSqrt[F:FltFmt](a: Flt[F]) extends FltOp1[F]
@op case class FltSigmoid[F:FltFmt](a: Flt[F]) extends FltOp1[F]

@op case class FltToFlt[F1:FltFmt,F2:FltFmt](a: Flt[F1], f2: FltFmt[F2]) extends FltOp2[F1,Flt[F2]]
@op case class FltToFix[F1:FltFmt,F2:FixFmt](a: Flt[F1], f2: FixFmt[F2]) extends FltOp2[F1,Fix[F2]]

@op case class TextToFlt[F:FltFmt](t: Text, fm: FltFmt[F]) extends FltOp1[F] {
  override val debugOnly: Boolean = true
}
@op case class FltToText[F:FltFmt](a: Flt[F]) extends FltOp2[F,Text] {
  override val debugOnly: Boolean = true
}

@op case class FltRandom[F:FltFmt](max: Option[Flt[F]]) extends FltOp1[F] {
  override def effects: Effects = Effects.Simple
}
