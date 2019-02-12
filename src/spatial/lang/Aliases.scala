package spatial.lang

/** Aliases visible to spatial.lang.* and outside
  *
  * No aliases of the form "type Foo = spatial.lang.Foo" (creates a circular reference)
  */
trait InternalAliases extends argon.lang.ExternalAliases {
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

/** Aliases for external use (application writers).
  *
  * No shadowing aliases (names that shadow Scala names).
  */
trait ExternalAliases extends InternalAliases {
  type SpatialApp = spatial.SpatialApp
  type SpatialTest = spatial.SpatialTest
  type SpatialTestbench = spatial.SpatialTestbench

  // --- Memories

  type DRAM[A,C[T]] = spatial.lang.DRAM[A,C]
  type DRAM1[A] = spatial.lang.DRAM1[A]
  type DRAM2[A] = spatial.lang.DRAM2[A]
  type DRAM3[A] = spatial.lang.DRAM3[A]
  type DRAM4[A] = spatial.lang.DRAM4[A]
  type DRAM5[A] = spatial.lang.DRAM5[A]
  lazy val DRAM1 = spatial.lang.DRAM1
  lazy val DRAM2 = spatial.lang.DRAM2
  lazy val DRAM3 = spatial.lang.DRAM3
  lazy val DRAM4 = spatial.lang.DRAM4
  lazy val DRAM5 = spatial.lang.DRAM5
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

  type MergeBuffer[A] = spatial.lang.MergeBuffer[A]
  lazy val MergeBuffer = spatial.lang.MergeBuffer
  type LineBuffer[A] = spatial.lang.LineBuffer[A]
  lazy val LineBuffer = spatial.lang.LineBuffer

  type FIFO[A] = spatial.lang.FIFO[A]
  lazy val FIFO = spatial.lang.FIFO

  type LIFO[A] = spatial.lang.LIFO[A]
  lazy val LIFO = spatial.lang.LIFO

  type Reg[A] = spatial.lang.Reg[A]
  type FIFOReg[A] = spatial.lang.FIFOReg[A]
  lazy val Reg = spatial.lang.Reg
  lazy val FIFOReg = spatial.lang.FIFOReg
  lazy val ArgIn = spatial.lang.ArgIn
  lazy val ArgOut = spatial.lang.ArgOut
  lazy val HostIO = spatial.lang.HostIO

  type StreamIn[A] = spatial.lang.StreamIn[A]
  lazy val StreamIn = spatial.lang.StreamIn

  type StreamOut[A] = spatial.lang.StreamOut[A]
  lazy val StreamOut = spatial.lang.StreamOut

  // --- Primitives


  type Counter[F] = spatial.lang.Counter[F]
  lazy val Counter = spatial.lang.Counter

  type CounterChain = spatial.lang.CounterChain
  lazy val CounterChain = spatial.lang.CounterChain

  type Wildcard = spatial.lang.Wildcard

}

/** Remaining aliases that shadow original Scala types. */
trait ShadowingAliases extends ExternalAliases {
  type Char = argon.lang.Fix[FALSE,_8,_0]
  type Byte = argon.lang.Fix[TRUE,_8,_0]
  type Short = argon.lang.Fix[TRUE,_16,_0]
  type Int   = argon.lang.Fix[TRUE,_32,_0]
  type Long  = argon.lang.Fix[TRUE,_64,_0]

  type Half  = argon.lang.Flt[_11,_5]
  type Float = argon.lang.Flt[_24,_8]
  type Double = argon.lang.Flt[_53,_11]

  type Boolean = argon.lang.Bit
  type String = argon.lang.Text
  type Label = java.lang.String

  type Array[A] = spatial.lang.host.Array[A]
  lazy val Array = spatial.lang.host.Array
  type Matrix[A] = spatial.lang.host.Matrix[A]
  lazy val Matrix = spatial.lang.host.Matrix

  type Tuple2[A,B] = argon.lang.Tup2[A,B]

  type Unit = argon.lang.Void

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

