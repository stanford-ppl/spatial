package spatial.lang.static

import spatial.lang.types.CustomBitWidths

// No aliases of the form "type Foo = spatial.lang.Foo" (creates a circular reference)
// Everything else is ok.
trait InternalAliases extends CustomBitWidths {
  type FixPt[S,I,F] = spatial.lang.Fix[S,I,F]
  type I[W] = FixPt[TRUE,W,_0]
  type Idx = I[_]
  type Rng = spatial.lang.Series[Idx]

  type I64 = FixPt[TRUE,_64,_0]
  type I32 = FixPt[TRUE,_32,_0]
  type I16 = FixPt[TRUE,_16,_0]
  type  I8 = FixPt[TRUE, _8,_0]
  type U32 = FixPt[FALSE,_32,_0]
  type U16 = FixPt[FALSE,_16,_0]
  type  U8 = FixPt[FALSE, _8,_0]

  type FltPt[M,E] = spatial.lang.Flt[M,E]
  type F64 = FltPt[_53,_11]
  type F32 = FltPt[_24,_8]
  type F16 = FltPt[_11,_5]

  type Mem[A,C[_]] = spatial.lang.types.Mem[A,C]
  type LocalMem[A,C[_]] = spatial.lang.types.LocalMem[A,C]
  type LocalMem0[A,C[T]<:LocalMem0[T,C]] = spatial.lang.types.LocalMem0[A,C]
  type LocalMem1[A,C[T]<:LocalMem1[T,C]] = spatial.lang.types.LocalMem1[A,C]
  type LocalMem2[A,C[T]<:LocalMem2[T,C]] = spatial.lang.types.LocalMem2[A,C]
  type LocalMem3[A,C[T]<:LocalMem3[T,C]] = spatial.lang.types.LocalMem3[A,C]
  type LocalMem4[A,C[T]<:LocalMem4[T,C]] = spatial.lang.types.LocalMem4[A,C]
  type LocalMem5[A,C[T]<:LocalMem5[T,C]] = spatial.lang.types.LocalMem5[A,C]

  type RemoteMem[A,C[_]] = spatial.lang.types.RemoteMem[A,C]
  
  type Bits[T] = spatial.lang.types.Bits[T]
  lazy val Bits = spatial.lang.types.Bits
  type Order[T] = spatial.lang.types.Order[T]
  lazy val Order = spatial.lang.types.Order
  type Arith[T] = spatial.lang.types.Arith[T]
  lazy val Arith = spatial.lang.types.Arith
  type Num[T] = spatial.lang.types.Num[T]
  lazy val Num = spatial.lang.types.Num

  lazy val Accel = spatial.lang.control.Accel
  lazy val Foreach = spatial.lang.control.Foreach
  lazy val Pipe = spatial.lang.control.Pipe
}

trait ExternalAliases extends InternalAliases {
  type SpatialApp = spatial.SpatialApp
  type Top[A] = spatial.lang.Top[A]

  // --- Memories

  type DRAM[A,C[T]] = spatial.lang.DRAM[A,C]
  type DRAM1[A] = spatial.lang.DRAM1[A]
  type DRAM2[A] = spatial.lang.DRAM2[A]
  type DRAM3[A] = spatial.lang.DRAM3[A]
  type DRAM4[A] = spatial.lang.DRAM4[A]
  type DRAM5[A] = spatial.lang.DRAM5[A]
  lazy val DRAM = spatial.lang.DRAM

  type SRAM[A,C[T]] = spatial.lang.SRAM[A,C]
  type SRAM1[A] = spatial.lang.SRAM1[A]
  type SRAM2[A] = spatial.lang.SRAM2[A]
  type SRAM3[A] = spatial.lang.SRAM3[A]
  type SRAM4[A] = spatial.lang.SRAM4[A]
  type SRAM5[A] = spatial.lang.SRAM5[A]
  lazy val SRAM = spatial.lang.SRAM

  type RegFile[A,C[T]] = spatial.lang.RegFile[A,C]
  type RegFile1[A] = spatial.lang.RegFile1[A]
  type RegFile2[A] = spatial.lang.RegFile2[A]
  type RegFile3[A] = spatial.lang.RegFile3[A]
  lazy val RegFile = spatial.lang.RegFile

  type FIFO[A] = spatial.lang.FIFO[A]
  lazy val FIFO = spatial.lang.FIFO

  type LIFO[A] = spatial.lang.LIFO[A]
  lazy val LIFO = spatial.lang.LIFO

  type Reg[A] = spatial.lang.Reg[A]
  lazy val Reg = spatial.lang.Reg

  type ArgIn[A] = spatial.lang.ArgIn[A]
  lazy val ArgIn = spatial.lang.ArgIn

  type ArgOut[A] = spatial.lang.ArgOut[A]
  lazy val ArgOut = spatial.lang.ArgOut

  // --- Primitives

  type Bit = spatial.lang.Bit
  lazy val Bit = spatial.lang.Bit

  type Void = spatial.lang.Void
  lazy val Void = spatial.lang.Void

  type Text = spatial.lang.Text
  lazy val Text = spatial.lang.Text

  type Fix[S,I,F] = spatial.lang.Fix[S,I,F]
  lazy val Fix = spatial.lang.Fix

  type Flt[M,E] = spatial.lang.Flt[M,E]
  lazy val Flt = spatial.lang.Flt

  type Vec[T] = spatial.lang.Vec[T]
  lazy val Vec = spatial.lang.Vec

  type Series[A] = spatial.lang.Series[A]
  lazy val Series = spatial.lang.Series

  type Counter[F] = spatial.lang.Counter[F]
  lazy val Counter = spatial.lang.Counter

  type CounterChain = spatial.lang.CounterChain
  lazy val CounterChain = spatial.lang.CounterChain
}
