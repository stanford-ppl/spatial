package spatial.lang.static

import core._
import spatial.lang.types.CustomBitWidths

// No aliases of the form "type Foo = spatial.lang.Foo" (creates a circular reference)
// Everything else is ok.
trait InternalAliases extends CustomBitWidths {

  type FixPt[S,I,F] = spatial.lang.Fix[(S,I,F)]
  type I64 = FixPt[TRUE,_64,_0]

  type I32 = FixPt[TRUE,_32,_0]
  type Idx = I32

  type I16 = FixPt[TRUE,_16,_0]
  type  I8 = FixPt[TRUE, _8,_0]
  type U32 = FixPt[FALSE,_32,_0]
  type U16 = FixPt[FALSE,_16,_0]
  type  U8 = FixPt[FALSE, _8,_0]

  type FltPt[M,E] = spatial.lang.Flt[(M,E)]
  type F64 = FltPt[_53,_11]
  type F32 = FltPt[_24,_8]
  type F16 = FltPt[_11,_5]

  type Mem[A,C[_]] = spatial.lang.types.Mem[A,C]
  type LocalMem[A,C[_]] = spatial.lang.types.LocalMem[A,C]
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
}

trait ExternalAliases extends InternalAliases {
  type SpatialApp = spatial.SpatialApp

  type Top[A] = spatial.lang.Top[A]

  // --- Memories

  type DRAM[T] = spatial.lang.DRAM[T]
  lazy val DRAM = spatial.lang.DRAM
  type SRAM[T] = spatial.lang.SRAM[T]
  lazy val SRAM = spatial.lang.SRAM
  type FIFO[T] = spatial.lang.FIFO[T]
  lazy val FIFO = spatial.lang.FIFO
  type LIFO[T] = spatial.lang.LIFO[T]
  lazy val LIFO = spatial.lang.LIFO
  type Reg[T] = spatial.lang.Reg[T]
  lazy val Reg = spatial.lang.Reg
  lazy val ArgIn = spatial.lang.ArgIn
  lazy val ArgOut = spatial.lang.ArgOut


  lazy val I32 = spatial.lang.I32

  type Bit = spatial.lang.Bit
  lazy val Bit = spatial.lang.Bit
  type Void = spatial.lang.Void
  lazy val Void = spatial.lang.Void
  type Text = spatial.lang.Text
  lazy val Text = spatial.lang.Text

  type FixFmt[F] = spatial.lang.FixFmt[F]
  lazy val FixFmt = spatial.lang.FixFmt
  type FltFmt[F] = spatial.lang.FltFmt[F]
  lazy val FltFmt = spatial.lang.FltFmt

  type Fix[F] = spatial.lang.Fix[F]
  lazy val Fix = spatial.lang.Fix
  type Flt[F] = spatial.lang.Flt[F]
  lazy val Flt = spatial.lang.Flt

  type Vec[T] = spatial.lang.Vec[T]
  lazy val Vec = spatial.lang.Vec

  type Series[A] = spatial.lang.Series[A]
  lazy val Series = spatial.lang.Series
  type Counter = spatial.lang.Counter
  lazy val Counter = spatial.lang.Counter
  type CounterChain = spatial.lang.CounterChain
  lazy val CounterChain = spatial.lang.CounterChain
}
