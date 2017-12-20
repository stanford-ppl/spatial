package pcc
package ir

case class Mux[T:Bits](s: Bit, a: T, b: T) extends Op[T]
case class FixMin[T:Fix](a: T, b: T) extends Op[T]
case class FixMax[T:Fix](a: T, b: T) extends Op[T]
case class FltMin[T:Flt](a: T, b: T) extends Op[T]
case class FltMax[T:Flt](a: T, b: T) extends Op[T]

case class FixNeg[T:Fix](a: T) extends Op[T]
case class FixAdd[T:Fix](a: T, b: T) extends Op[T]
case class FixSub[T:Fix](a: T, b: T) extends Op[T]
case class FixMul[T:Fix](a: T, b: T) extends Op[T]
case class FixDiv[T:Fix](a: T, b: T) extends Op[T]
case class FixMod[T:Fix](a: T, b: T) extends Op[T]

case class FixLst[T:Fix](a: T, b: T) extends Op[Bit]  // <
case class FixLeq[T:Fix](a: T, b: T) extends Op[Bit]  // <=
case class FixEql[T:Fix](a: T, b: T) extends Op[Bit]  // ==
case class FixNeq[T:Fix](a: T, b: T) extends Op[Bit]  // !=
case class FixSLA[T:Fix](a: T, b: T) extends Op[T]    // Shift Left Arithmetic
case class FixSRA[T:Fix](a: T, b: T) extends Op[T]    // Shift Right Arithmetic

case class FixSig[T:Fix](a: T) extends Op[T]
case class FixExp[T:Fix](a: T) extends Op[T]
case class FixLog[T:Fix](a: T) extends Op[T]
case class FixSqt[T:Fix](a: T) extends Op[T]
case class FixAbs[T:Fix](a: T) extends Op[T]

case class FltNeg[T:Flt](a: T) extends Op[T]
case class FltAdd[T:Flt](a: T, b: T) extends Op[T]
case class FltSub[T:Flt](a: T, b: T) extends Op[T]
case class FltMul[T:Flt](a: T, b: T) extends Op[T]
case class FltDiv[T:Flt](a: T, b: T) extends Op[T]

case class FltLst[T:Flt](a: T, b: T) extends Op[Bit]  // <
case class FltLeq[T:Flt](a: T, b: T) extends Op[Bit]  // <=
case class FltEql[T:Flt](a: T, b: T) extends Op[Bit]  // ==
case class FltNeq[T:Flt](a: T, b: T) extends Op[Bit]  // !=

case class FltSig[T:Flt](a: T) extends Op[T]
case class FltExp[T:Flt](a: T) extends Op[T]
case class FltLog[T:Flt](a: T) extends Op[T]
case class FltSqt[T:Flt](a: T) extends Op[T]
case class FltAbs[T:Flt](a: T) extends Op[T]

case class Not(a: Bit) extends Op[Bit]
case class And(a: Bit, b: Bit) extends Op[Bit]
case class Or(a: Bit, b: Bit) extends Op[Bit]
