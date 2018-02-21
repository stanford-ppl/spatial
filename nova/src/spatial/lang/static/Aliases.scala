package spatial.lang.static

// No aliases of the form type X = spatial.lang.X (creates a circular reference)
// Everything else is ok.
trait InternalAliases {
  type Mem[A,C[_]] = spatial.lang.memories.Mem[A,C]
  type LocalMem[A,C[_<:A]] = spatial.lang.memories.LocalMem[A,C]
  type RemoteMem[A,C[_<:A]] = spatial.lang.memories.RemoteMem[A,C]

  type DRAM[T] = spatial.lang.memories.DRAM[T]
  lazy val DRAM = spatial.lang.memories.DRAM
  type SRAM[T] = spatial.lang.memories.SRAM[T]
  lazy val SRAM = spatial.lang.memories.SRAM
  type FIFO[T] = spatial.lang.memories.FIFO[T]
  lazy val FIFO = spatial.lang.memories.FIFO
  type LIFO[T] = spatial.lang.memories.LIFO[T]
  lazy val LIFO = spatial.lang.memories.LIFO
  type Reg[T] = spatial.lang.memories.Reg[T]
  lazy val Reg = spatial.lang.memories.Reg
  lazy val ArgIn = spatial.lang.memories.ArgIn
  lazy val ArgOut = spatial.lang.memories.ArgOut

  type Idx = spatial.lang.I32
  
  type Bits[T] = spatial.lang.types.Bits[T]
  lazy val Bits = spatial.lang.types.Bits
  type Num[T] = spatial.lang.types.Num[T]

  lazy val Accel = spatial.lang.control.Accel
  lazy val Foreach = spatial.lang.control.Foreach
}

trait ExternalAliases extends InternalAliases {
  type Ref[T] = spatial.lang.Ref[T]

  type I32 = spatial.lang.I32
  lazy val I32 = spatial.lang.I32
  type I16 = spatial.lang.I16
  lazy val I16 = spatial.lang.I16
  type I8  = spatial.lang.I8
  lazy val I8 = spatial.lang.I8

  type F32 = spatial.lang.F32
  lazy val F32 = spatial.lang.F32
  type F16 = spatial.lang.F16
  lazy val F16 = spatial.lang.F16

  type Bit = spatial.lang.Bit
  lazy val Bit = spatial.lang.Bit
  type Void = spatial.lang.Void
  lazy val Void = spatial.lang.Void
  type Text = spatial.lang.Text
  lazy val Text = spatial.lang.Text

  type Fix[T] = spatial.lang.Fix[T]
  type Flt[T] = spatial.lang.Flt[T]

  type Vec[T] = spatial.lang.Vec[T]
  lazy val Vec = spatial.lang.Vec

  type Series = spatial.lang.Series
  lazy val Series = spatial.lang.Series
  type Counter = spatial.lang.Counter
  lazy val Counter = spatial.lang.Counter
  type CounterChain = spatial.lang.CounterChain
  lazy val CounterChain = spatial.lang.CounterChain
}
