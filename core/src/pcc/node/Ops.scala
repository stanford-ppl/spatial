package pcc.node

import forge.op
import pcc.lang._

/** Bit **/
@op case class Not(a: Bit) extends Primitive[Bit]
@op case class And(a: Bit, b: Bit) extends Primitive[Bit]
@op case class Or(a: Bit, b: Bit) extends Primitive[Bit]

/** Fix **/
@op case class FixNeg[A:Fix](a: A) extends Primitive[A]
@op case class FixAdd[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixSub[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixMul[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixDiv[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixMod[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixSLA[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixSRA[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixLst[A:Fix](a: A, b: A) extends Primitive[Bit]
@op case class FixLeq[A:Fix](a: A, b: A) extends Primitive[Bit]
@op case class FixEql[A:Fix](a: A, b: A) extends Primitive[Bit]
@op case class FixNeq[A:Fix](a: A, b: A) extends Primitive[Bit]

/** Flt **/
@op case class FltNeg[A:Flt](a: A) extends Primitive[A]
@op case class FltAdd[A:Flt](a: A, b: A) extends Primitive[A]
@op case class FltSub[A:Flt](a: A, b: A) extends Primitive[A]
@op case class FltMul[A:Flt](a: A, b: A) extends Primitive[A]
@op case class FltDiv[A:Flt](a: A, b: A) extends Primitive[A]
@op case class FltLst[A:Flt](a: A, b: A) extends Primitive[Bit]
@op case class FltLeq[A:Flt](a: A, b: A) extends Primitive[Bit]
@op case class FltEql[A:Flt](a: A, b: A) extends Primitive[Bit]
@op case class FltNeq[A:Flt](a: A, b: A) extends Primitive[Bit]


/** Math **/
@op case class Mux[A:Bits](s: Bit, a: A, b: A) extends Primitive[A]

@op case class FixMin[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixMax[A:Fix](a: A, b: A) extends Primitive[A]
@op case class FixSig[A:Fix](a: A) extends Primitive[A]
@op case class FixExp[A:Fix](a: A) extends Primitive[A]
@op case class FixLog[A:Fix](a: A) extends Primitive[A]
@op case class FixSqt[A:Fix](a: A) extends Primitive[A]
@op case class FixAbs[A:Fix](a: A) extends Primitive[A]

@op case class FltMin[A:Flt](a: A, b: A) extends Primitive[A]
@op case class FltMax[A:Flt](a: A, b: A) extends Primitive[A]
@op case class FltSig[A:Flt](a: A) extends Primitive[A]
@op case class FltExp[A:Flt](a: A) extends Primitive[A]
@op case class FltLog[A:Flt](a: A) extends Primitive[A]
@op case class FltSqt[A:Flt](a: A) extends Primitive[A]
@op case class FltAbs[A:Flt](a: A) extends Primitive[A]
