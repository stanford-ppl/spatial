package pcc.lang.static

trait InternalAliases {
  type Mem[A,C[_]] = pcc.lang.memories.Mem[A,C]
  type LocalMem[A,C[_<:A]] = pcc.lang.memories.LocalMem[A,C]
  type RemoteMem[A,C[_<:A]] = pcc.lang.memories.RemoteMem[A,C]

  type DRAM[T] = pcc.lang.memories.DRAM[T]
  lazy val DRAM = pcc.lang.memories.DRAM
  type SRAM[T] = pcc.lang.memories.SRAM[T]
  lazy val SRAM = pcc.lang.memories.SRAM
  type FIFO[T] = pcc.lang.memories.FIFO[T]
  lazy val FIFO = pcc.lang.memories.FIFO
  type LIFO[T] = pcc.lang.memories.LIFO[T]
  lazy val LIFO = pcc.lang.memories.LIFO
  type Reg[T] = pcc.lang.memories.Reg[T]
  lazy val Reg = pcc.lang.memories.Reg
  lazy val ArgIn = pcc.lang.memories.ArgIn
  lazy val ArgOut = pcc.lang.memories.ArgOut

  type Num[T] = pcc.lang.types.Num[T]
  type Bits[T] = pcc.lang.types.Bits[T]

  lazy val Accel = pcc.lang.control.Accel
  lazy val Foreach = pcc.lang.control.Foreach
  lazy val BlackBox = pcc.lang.units.BlackBox
}

trait Aliases extends InternalAliases {
  type I32 = pcc.lang.I32
  lazy val I32 = pcc.lang.I32
  type I16 = pcc.lang.I16
  lazy val I16 = pcc.lang.I16
  type I8  = pcc.lang.I8
  lazy val I8 = pcc.lang.I8

  type F32 = pcc.lang.F32
  lazy val F32 = pcc.lang.F32
  type F16 = pcc.lang.F16
  lazy val F16 = pcc.lang.F16

  type Bit = pcc.lang.Bit
  lazy val Bit = pcc.lang.Bit
  type Void = pcc.lang.Void
  lazy val Void = pcc.lang.Void
  type Text = pcc.lang.Text
  lazy val Text = pcc.lang.Text

  type Series = pcc.lang.Series
  lazy val Series = pcc.lang.Series
  type Counter = pcc.lang.Counter
  lazy val Counter = pcc.lang.Counter
  type CounterChain = pcc.lang.CounterChain
  lazy val CounterChain = pcc.lang.CounterChain

  type Overload0 = pcc.lang.Overload0
  type Overload1 = pcc.lang.Overload1
  type Overload2 = pcc.lang.Overload2
  type Overload3 = pcc.lang.Overload3
}
