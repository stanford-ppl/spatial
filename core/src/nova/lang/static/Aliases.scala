package nova.lang.static

// No aliases of the form type X = nova.lang.X (creates a circular reference)
// Everything else is ok.
trait InternalAliases {
  type Mem[A,C[_]] = nova.lang.memories.Mem[A,C]
  type LocalMem[A,C[_<:A]] = nova.lang.memories.LocalMem[A,C]
  type RemoteMem[A,C[_<:A]] = nova.lang.memories.RemoteMem[A,C]

  type DRAM[T] = nova.lang.memories.DRAM[T]
  lazy val DRAM = nova.lang.memories.DRAM
  type SRAM[T] = nova.lang.memories.SRAM[T]
  lazy val SRAM = nova.lang.memories.SRAM
  type FIFO[T] = nova.lang.memories.FIFO[T]
  lazy val FIFO = nova.lang.memories.FIFO
  type LIFO[T] = nova.lang.memories.LIFO[T]
  lazy val LIFO = nova.lang.memories.LIFO
  type Reg[T] = nova.lang.memories.Reg[T]
  lazy val Reg = nova.lang.memories.Reg
  lazy val ArgIn = nova.lang.memories.ArgIn
  lazy val ArgOut = nova.lang.memories.ArgOut

  type Bits[T] = nova.lang.types.Bits[T]
  lazy val Bits = nova.lang.types.Bits
  type Num[T] = nova.lang.types.Num[T]

  lazy val Accel = nova.lang.control.Accel
  lazy val Foreach = nova.lang.control.Foreach
}

trait Aliases extends InternalAliases {
  type I32 = nova.lang.I32
  lazy val I32 = nova.lang.I32
  type I16 = nova.lang.I16
  lazy val I16 = nova.lang.I16
  type I8  = nova.lang.I8
  lazy val I8 = nova.lang.I8

  type F32 = nova.lang.F32
  lazy val F32 = nova.lang.F32
  type F16 = nova.lang.F16
  lazy val F16 = nova.lang.F16

  type Bit = nova.lang.Bit
  lazy val Bit = nova.lang.Bit
  type Void = nova.lang.Void
  lazy val Void = nova.lang.Void
  type Text = nova.lang.Text
  lazy val Text = nova.lang.Text

  type Fix[T] = nova.lang.Fix[T]
  type Flt[T] = nova.lang.Flt[T]

  type Vec[T] = nova.lang.Vec[T]
  lazy val Vec = nova.lang.Vec

  type Series = nova.lang.Series
  lazy val Series = nova.lang.Series
  type Counter = nova.lang.Counter
  lazy val Counter = nova.lang.Counter
  type CounterChain = nova.lang.CounterChain
  lazy val CounterChain = nova.lang.CounterChain

  type Overload0 = nova.lang.Overload0
  type Overload1 = nova.lang.Overload1
  type Overload2 = nova.lang.Overload2
  type Overload3 = nova.lang.Overload3
}
