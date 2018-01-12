package pcc.node

import forge.op
import pcc.core._
import pcc.data._
import pcc.lang._

/** DRAM **/
@op case class DRAMAlloc[A:Bits](dims: Seq[I32]) extends Alloc[DRAM[A]] {
  override def effects: Effects = Effects.Mutable
}

/** FIFO **/
@op case class FIFOAlloc[A:Bits](depth: I32) extends Alloc[FIFO[A]] {
  override def effects: Effects = Effects.Mutable
}

/** LIFO **/
@op case class LIFOAlloc[A:Bits](depth: I32) extends Alloc[LIFO[A]] {
  override def effects: Effects = Effects.Mutable
}

/** Reg **/
@op case class RegAlloc[T:Bits](reset: T) extends Alloc[Reg[T]] {
  override def effects: Effects = Effects.Mutable
}

@op case class RegWrite[T:Bits](reg: Reg[T], data: T, ens: Seq[Bit]) extends Writer(reg,data.asSym,None,ens)

@op case class RegRead[T:Bits](reg: Reg[T]) extends Reader(reg,None,Nil) {
  override val isStateless = true
  override def ens: Seq[Bit] = Nil
}

/** SRAM **/
@op case class SRAMAlloc[A:Bits](dims: Seq[I32]) extends Alloc[SRAM[A]] {
  override def effects: Effects = Effects.Mutable
}
@op case class SRAMRead[A:Bits](sram: SRAM[A], addr: Seq[I32], ens: Seq[Bit]) extends Reader[A,A](sram,Some(addr),ens)
@op case class SRAMWrite[A:Bits](sram: SRAM[A], data: A, addr: Seq[I32], ens: Seq[Bit]) extends Writer[A](sram,data.asSym,Some(addr),ens)

@op case class SRAMDim(sram: SRAM[_], d: Int) extends Op[I32]
@op case class SRAMRank(sram: SRAM[_]) extends Op[I32]
