package spatial.lang.static

import spatial.lang.types.CustomBitWidths

// No aliases of the form "type Foo = spatial.lang.Foo" (creates a circular reference)
// Everything else is ok.
trait InternalAliases extends CustomBitWidths {
  type FixPt[S,I,F] = spatial.lang.Fix[S,I,F]
  type Ind[W] = FixPt[TRUE,W,_0]
  type Idx = Ind[_]
  type Rng = spatial.lang.Series[Idx]

  type I64 = FixPt[TRUE,_64,_0]
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

  type U64 = FixPt[FALSE,_64,_0]
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

  type DRAMx[A] = spatial.lang.DRAM[A,C forSome{ type C[T] }]
  type SRAMx[A] = spatial.lang.SRAM[A,C forSome{ type C[T] }]
  type RegFilex[A] = spatial.lang.RegFile[A,C forSome{ type C[T] }]
  type LUTx[A] = spatial.lang.LUT[A,C forSome{ type C[T] }]

  type Bits[T] = spatial.lang.types.Bits[T]
  lazy val Bits = spatial.lang.types.Bits
  type Order[T] = spatial.lang.types.Order[T]
  lazy val Order = spatial.lang.types.Order
  type Arith[T] = spatial.lang.types.Arith[T]
  lazy val Arith = spatial.lang.types.Arith
  type Num[T] = spatial.lang.types.Num[T]
  lazy val Num = spatial.lang.types.Num

  lazy val Accel   = spatial.lang.control.Accel
  lazy val Foreach = spatial.lang.control.Foreach
  lazy val Reduce  = spatial.lang.control.Reduce
  lazy val Fold    = spatial.lang.control.Fold
  lazy val MemReduce = spatial.lang.control.MemReduce
  lazy val MemFold   = spatial.lang.control.MemFold
  lazy val FSM       = spatial.lang.control.FSM

  lazy val Parallel = spatial.lang.control.Parallel
  lazy val Pipe = spatial.lang.control.Pipe
  lazy val Sequential = spatial.lang.control.Sequential
  lazy val Stream = spatial.lang.control.Stream
  lazy val Named = spatial.lang.control.Named

  /** Host */
  type Tensor1[A] = spatial.lang.host.Array[A]
  lazy val Tensor1 = spatial.lang.host.Array
  type Tensor2[A] = spatial.lang.host.Matrix[A]
  lazy val Tensor2 = spatial.lang.host.Matrix
  type Tensor3[A] = spatial.lang.host.Tensor3[A]
  lazy val Tensor3 = spatial.lang.host.Tensor3
  type Tensor4[A] = spatial.lang.host.Tensor4[A]
  lazy val Tensor4 = spatial.lang.host.Tensor4
  type Tensor5[A] = spatial.lang.host.Tensor5[A]
  lazy val Tensor5 = spatial.lang.host.Tensor5
  type CSVFile = spatial.lang.host.CSVFile
  type BinaryFile = spatial.lang.host.BinaryFile
}

trait ExternalAliases extends InternalAliases {
  type SpatialApp = spatial.SpatialApp
  type SpatialTest = spatial.SpatialTest
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

  type LUT[A,C[T]] = spatial.lang.LUT[A,C]
  type LUT1[A] = spatial.lang.LUT1[A]
  type LUT2[A] = spatial.lang.LUT2[A]
  type LUT3[A] = spatial.lang.LUT3[A]
  type LUT4[A] = spatial.lang.LUT4[A]
  type LUT5[A] = spatial.lang.LUT5[A]
  lazy val LUT = spatial.lang.LUT

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
  lazy val ArgIn = spatial.lang.ArgIn
  lazy val ArgOut = spatial.lang.ArgOut
  lazy val HostIO = spatial.lang.HostIO

  type StreamIn[A] = spatial.lang.StreamIn[A]
  lazy val StreamIn = spatial.lang.StreamIn

  type StreamOut[A] = spatial.lang.StreamOut[A]
  lazy val StreamOut = spatial.lang.StreamOut

  // --- Primitives
  type Tup2[A,B] = spatial.lang.Tup2[A,B]

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

  type Wildcard = spatial.lang.Wildcard
}

trait ShadowingAliases extends ExternalAliases {
  type Char = spatial.lang.Fix[FALSE,_8,_0]
  type Byte = spatial.lang.Fix[TRUE,_8,_0]
  type Short = spatial.lang.Fix[TRUE,_16,_0]
  type Int   = spatial.lang.Fix[TRUE,_32,_0]
  type Long  = spatial.lang.Fix[TRUE,_64,_0]

  type Half  = spatial.lang.Flt[_11,_5]
  type Float = spatial.lang.Flt[_24,_8]
  type Double = spatial.lang.Flt[_53,_11]

  type Boolean = spatial.lang.Bit
  type String = spatial.lang.Text
  type Label = java.lang.String

  type Array[A] = spatial.lang.host.Array[A]
  lazy val Array = spatial.lang.host.Array
  type Matrix[A] = spatial.lang.host.Matrix[A]
  lazy val Matrix = spatial.lang.host.Matrix

  type Tuple2[A,B] = spatial.lang.Tup2[A,B]

  type Unit = spatial.lang.Void

  object gen {
    type Char = scala.Char
    type Byte = scala.Byte
    type Short = scala.Short
    type Int = scala.Int
    type Long = scala.Long
    type Float = scala.Float
    type Double = scala.Double
    type Boolean = scala.Boolean
    type String = java.lang.String
    type Array[A] = scala.Array[A]
    lazy val Array = scala.Array
    type Unit = scala.Unit
  }
}

