package pcc.ir.static

trait Aliases {
  type I32 = pcc.ir.I32
  type I16 = pcc.ir.I16
  type I8  = pcc.ir.I8

  type F32 = pcc.ir.F32
  type F16 = pcc.ir.F16

  type Bit = pcc.ir.Bit
  type Void = pcc.ir.Void
  type Text = pcc.ir.Text

  type Series = pcc.ir.Series
  lazy val Series = pcc.ir.Series
  type Counter = pcc.ir.Counter
  lazy val Counter = pcc.ir.Counter
  type CounterChain = pcc.ir.CounterChain

  type DRAM[T] = pcc.ir.memories.DRAM[T]
  lazy val DRAM = pcc.ir.memories.DRAM
  type SRAM[T] = pcc.ir.memories.SRAM[T]
  lazy val SRAM = pcc.ir.memories.SRAM
  type FIFO[T] = pcc.ir.memories.FIFO[T]
  lazy val FIFO = pcc.ir.memories.FIFO
  type LIFO[T] = pcc.ir.memories.LIFO[T]
  lazy val LIFO = pcc.ir.memories.LIFO
}
