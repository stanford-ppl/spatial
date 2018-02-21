package spatial.node

import forge.tags._
import core._
import spatial.lang._

/** DRAM **/
@op case class DRAMNew[A:Bits](dims: Seq[I32]) extends MemAlloc[DRAM[A]] {
  override def effects: Effects = Effects.Mutable
}

/** FIFO **/
@op case class FIFONew[A:Bits](depth: I32) extends MemAlloc[FIFO[A]] {
  override def effects: Effects = Effects.Mutable
  def dims = Seq(depth)
}

/** LIFO **/
@op case class LIFONew[A:Bits](depth: I32) extends MemAlloc[LIFO[A]] {
  override def effects: Effects = Effects.Mutable
  def dims = Seq(depth)
}

/** Reg **/
@op case class RegNew[T:Bits](reset: Bits[T]) extends MemAlloc[Reg[T]] {
  override def effects: Effects = Effects.Mutable
  def dims = Nil
}

@op case class ArgInNew[T:Bits](init: Bits[T]) extends MemAlloc[Reg[T]] {
  def dims = Nil
}
@op case class ArgOutNew[T:Bits](init: Bits[T]) extends MemAlloc[Reg[T]] {
  override def effects: Effects = Effects.Mutable
  def dims = Nil
}

@op case class RegWrite[T:Bits](reg: Reg[T], data: Bits[T], ens: Seq[Bit]) extends Writer(reg,data,Nil,ens)

@op case class RegRead[T:Bits](reg: Reg[T]) extends Reader(reg,Nil,Nil) {
  override val isStateless = true
  override def ens: Seq[Bit] = Nil
}

/** SRAM **/
@op case class SRAMNew[A:Bits](dims: Seq[I32]) extends MemAlloc[SRAM[A]] {
  override def effects: Effects = Effects.Mutable
}
@op case class SRAMRead[A:Bits](sram: SRAM[A], addr: Seq[I32], ens: Seq[Bit]) extends Reader[A,A](sram,addr,ens)
@op case class SRAMWrite[A:Bits](sram: SRAM[A], data: Bits[A], addr: Seq[I32], ens: Seq[Bit]) extends Writer[A](sram,data,addr,ens)

@op case class SRAMDim(sram: SRAM[_], d: Int) extends Primitive[I32] {
  override val isStateless: Boolean = true
}
@op case class SRAMRank(sram: SRAM[_]) extends Primitive[I32] {
  override val isStateless: Boolean = true
}
