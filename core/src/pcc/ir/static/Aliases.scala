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

  type DRAM[T] = pcc.ir.DRAM[T]
  type DRAM1[T] = pcc.ir.DRAM1[T]
  type DRAM2[T] = pcc.ir.DRAM2[T]
}
