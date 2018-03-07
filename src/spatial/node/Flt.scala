package spatial.node

import forge.tags._
import core._
import spatial.lang._

abstract class FltOp2[M:INT,E:INT,R:Type] extends Primitive[R] {
  lazy val fmt: FltFmt[M,E] = FltFmt.from[M,E]
}
abstract class FltOp1[M:INT,E:INT] extends FltOp2[M,E,Flt[M,E]]

@op case class FltNeg[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltAdd[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp1[M,E]
@op case class FltSub[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp1[M,E]
@op case class FltMul[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp1[M,E]
@op case class FltDiv[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp1[M,E]
@op case class FltMod[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp1[M,E]
@op case class FltLst[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp2[M,E,Bit]
@op case class FltLeq[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp2[M,E,Bit]
@op case class FltEql[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp2[M,E,Bit]
@op case class FltNeq[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp2[M,E,Bit]

@op case class FltMin[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp1[M,E]
@op case class FltMax[M:INT,E:INT](a: Flt[M,E], b: Flt[M,E]) extends FltOp1[M,E]
@op case class FltAbs[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltCeil[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltFloor[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltPow[M:INT,E:INT](b: Flt[M,E], e: Flt[M,E]) extends FltOp1[M,E]
@op case class FltExp[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltLn[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltSqrt[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]

@op case class FltSin[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltCos[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltTan[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltSinh[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltCosh[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltTanh[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltAsin[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltAcos[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltAtan[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]

@op case class FltInvSqrt[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]
@op case class FltSigmoid[M:INT,E:INT](a: Flt[M,E]) extends FltOp1[M,E]

@op case class FltToFlt[M1:INT,E1:INT,M2:INT,E2:INT](
    a: Flt[M1,E1],
    f2: FltFmt[M2,E2])
  extends FltOp2[M1,E1,Flt[M2,E2]]

@op case class FltToFix[M1:INT,E1:INT,S2:BOOL,I2:INT,F2:INT](
    a:  Flt[M1,E1],
    f2: FixFmt[S2,I2,F2])
  extends FltOp2[M1,E1,Fix[S2,I2,F2]]

@op case class TextToFlt[M:INT,E:INT](t: Text, fm: FltFmt[M,E]) extends FltOp1[M,E] {
  override val debugOnly: Boolean = true
}
@op case class FltToText[M:INT,E:INT](a: Flt[M,E]) extends FltOp2[M,E,Text] {
  override val debugOnly: Boolean = true
}

@op case class FltRandom[M:INT,E:INT](max: Option[Flt[M,E]]) extends FltOp1[M,E] {
  override def effects: Effects = Effects.Simple
}
