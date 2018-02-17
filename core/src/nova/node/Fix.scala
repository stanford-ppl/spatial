package nova.node

import forge.tags._
import nova.core._
import nova.lang._

/** Fix **/
@op case class FixNeg[A:Fix](a: A) extends Primitive[A]
@op case class FixInv[A:Fix](a: A) extends Primitive[A]
@op case class FixAnd[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixOr[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixXor[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixAdd[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixSub[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixMul[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixDiv[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixMod[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixSLA[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixSRA[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixSRU[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixLst[A:Fix](a: A, b: A) extends Primitive[Bit]
@op case class FixLeq[A:Fix](a: A, b: A) extends Primitive[Bit]
@op case class FixEql[A:Fix](a: A, b: A) extends Primitive[Bit]
@op case class FixNeq[A:Fix](a: A, b: A) extends Primitive[Bit]

@op case class FixMin[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixMax[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixAbs[A:Fix](a: A) extends Primitive[A]
@op case class FixCeil[A:Fix](a: A) extends Primitive[A]
@op case class FixFloor[A:Fix](a: A) extends Primitive[A]
@op case class FixPow[A:Fix](b: A, e: A) extends Primitive[A]
@op case class FixExp[A:Fix](a: A) extends Primitive[A]
@op case class FixLn[A:Fix](a: A) extends Primitive[A]
@op case class FixSqrt[A:Fix](a: A) extends Primitive[A]

@op case class FixSin[A:Fix](a: A) extends Primitive[A]
@op case class FixCos[A:Fix](a: A) extends Primitive[A]
@op case class FixTan[A:Fix](a: A) extends Primitive[A]
@op case class FixSinh[A:Fix](a: A) extends Primitive[A]
@op case class FixCosh[A:Fix](a: A) extends Primitive[A]
@op case class FixTanh[A:Fix](a: A) extends Primitive[A]
@op case class FixAsin[A:Fix](a: A) extends Primitive[A]
@op case class FixAcos[A:Fix](a: A) extends Primitive[A]
@op case class FixAtan[A:Fix](a: A) extends Primitive[A]

@op case class FixInvSqrt[A:Fix](a: A) extends Primitive[A]
@op case class FixSigmoid[A:Fix](a: A) extends Primitive[A]

@op case class FixRandom[A:Fix](max: Option[A]) extends Primitive[A] {
  override def effects: Effects = Effects.Simple
}
