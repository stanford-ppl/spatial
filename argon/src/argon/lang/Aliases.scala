package argon.lang

import argon.lang.types.CustomBitWidths

// No aliases of the form "type Foo = argon.lang.Foo" (creates a circular reference)
// Everything else is ok.
trait InternalAliases extends CustomBitWidths {
  type FixPt[S,I,F] = argon.lang.Fix[S,I,F]
  type Ind[W] = FixPt[TRUE,W,_0]
  type Idx = Ind[_]
  type Rng = argon.lang.Series[Idx]

  type I256 = FixPt[TRUE,_256,_0]
  type I240 = FixPt[TRUE,_240,_0]
  type I200 = FixPt[TRUE,_200,_0]
  type I160 = FixPt[TRUE,_160,_0]
  type I128 = FixPt[TRUE,_128,_0]
  type I120 = FixPt[TRUE,_120,_0]
  type I80 = FixPt[TRUE,_80,_0]
  type I64 = FixPt[TRUE,_64,_0]
  type I40 = FixPt[TRUE,_40,_0]
  type I32 = FixPt[TRUE,_32,_0]
  type I16 = FixPt[TRUE,_16,_0]
  type I15 = FixPt[TRUE,_15,_0]
  type I14 = FixPt[TRUE,_14,_0]
  type I13 = FixPt[TRUE,_13,_0]
  type I12 = FixPt[TRUE,_12,_0]
  type I11 = FixPt[TRUE,_11,_0]
  type I10 = FixPt[TRUE,_10,_0]
  type  I9 = FixPt[TRUE, _9,_0]
  type  I8 = FixPt[TRUE, _8,_0]
  type  I7 = FixPt[TRUE, _7,_0]
  type  I6 = FixPt[TRUE, _6,_0]
  type  I5 = FixPt[TRUE, _5,_0]
  type  I4 = FixPt[TRUE, _4,_0]
  type  I3 = FixPt[TRUE, _3,_0]
  type  I2 = FixPt[TRUE, _2,_0]

  type U256 = FixPt[FALSE,_256,_0]
  type U240 = FixPt[FALSE,_240,_0]
  type U200 = FixPt[FALSE,_200,_0]
  type U160 = FixPt[FALSE,_160,_0]
  type U128 = FixPt[FALSE,_128,_0]
  type U120 = FixPt[FALSE,_120,_0]
  type U80 = FixPt[FALSE,_80,_0]
  type U64 = FixPt[FALSE,_64,_0]
  type U40 = FixPt[FALSE,_40,_0]
  type U32 = FixPt[FALSE,_32,_0]
  type U16 = FixPt[FALSE,_16,_0]
  type U15 = FixPt[FALSE,_15,_0]
  type U14 = FixPt[FALSE,_14,_0]
  type U13 = FixPt[FALSE,_13,_0]
  type U12 = FixPt[FALSE,_12,_0]
  type U11 = FixPt[FALSE,_11,_0]
  type U10 = FixPt[FALSE,_10,_0]
  type  U9 = FixPt[FALSE, _9,_0]
  type  U8 = FixPt[FALSE, _8,_0]
  type  U7 = FixPt[FALSE, _7,_0]
  type  U6 = FixPt[FALSE, _6,_0]
  type  U5 = FixPt[FALSE, _5,_0]
  type  U4 = FixPt[FALSE, _4,_0]
  type  U3 = FixPt[FALSE, _3,_0]
  type  U2 = FixPt[FALSE, _2,_0]

  type Int64 = I64
  type Int32 = I32
  type Int16 = I16
  type Int15 = I15
  type Int14 = I14
  type Int13 = I13
  type Int12 = I12
  type Int11 = I11
  type Int10 = I10
  type Int9  = I9
  type Int8  = I8
  type Int7  = I7
  type Int6  = I6
  type Int5  = I5
  type Int4  = I4
  type Int3  = I3
  type Int2  = I2

  type UInt64 = U64
  type UInt32 = U32
  type UInt16 = U16
  type UInt15 = U15
  type UInt14 = U14
  type UInt13 = U13
  type UInt12 = U12
  type UInt11 = U11
  type UInt10 = U10
  type UInt9  = U9
  type UInt8  = U8
  type UInt7  = U7
  type UInt6  = U6
  type UInt5  = U5
  type UInt4  = U4
  type UInt3  = U3
  type UInt2  = U2

  type FltPt[M,E] = argon.lang.Flt[M,E]
  type F64 = FltPt[_53,_11]
  type F32 = FltPt[_24,_8]
  type F16 = FltPt[_11,_5]

  type Arith[T] = argon.lang.types.Arith[T]
  lazy val Arith = argon.lang.types.Arith

  type Bits[T] = argon.lang.types.Bits[T]
  lazy val Bits = argon.lang.types.Bits

  type Order[T] = argon.lang.types.Order[T]
  lazy val Order = argon.lang.types.Order

  type Num[T] = argon.lang.types.Num[T]
  lazy val Num = argon.lang.types.Num

}

/** Aliases for application use (but no shadowing aliases). */
trait ExternalAliases extends InternalAliases {
  type Bit = argon.lang.Bit
  lazy val Bit = argon.lang.Bit
  lazy val BitType = argon.lang.BitType

  type FixFmt[S,I,F] = argon.lang.FixFmt[S,I,F]
  lazy val FixFmt = argon.lang.FixFmt

  type Fix[S,I,F] = argon.lang.Fix[S,I,F]
  lazy val Fix = argon.lang.Fix
  lazy val I32 = argon.lang.I32
  lazy val FixPtType = argon.lang.FixPtType

  type FltFmt[M,E] = argon.lang.FltFmt[M,E]
  lazy val FltFmt = argon.lang.FltFmt

  type Flt[M,E] = argon.lang.Flt[M,E]
  lazy val Flt = argon.lang.Flt
  lazy val FltPtType = argon.lang.FltPtType
  lazy val HalfType = argon.lang.HalfType
  lazy val FloatType = argon.lang.FloatType
  lazy val DoubleType = argon.lang.DoubleType

  type Series[A] = argon.lang.Series[A]
  lazy val Series = argon.lang.Series

  type Struct[A] = argon.lang.Struct[A]
  lazy val Struct = argon.lang.Struct

  type Text = argon.lang.Text
  lazy val Text = argon.lang.Text

  type Top[A] = argon.lang.Top[A]

  type Tup2[A,B] = argon.lang.Tup2[A,B]
  lazy val Tup2 = argon.lang.Tup2

  type Var[A] = argon.lang.Var[A]
  lazy val Var = argon.lang.Var

  type Vec[A] = argon.lang.Vec[A]
  lazy val Vec = argon.lang.Vec

  type Void = argon.lang.Void
  lazy val Void = argon.lang.Void

}

/** Aliases which shadow original Scala types. */
trait ShadowingAliases extends ExternalAliases {

}
