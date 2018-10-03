package spatial.node

import argon._
import argon.node._
import forge.tags._

import spatial.lang._

@op case class DRAMStaticNew[A:Bits,C[T]](dims: Seq[I32], zero: A)(implicit tp: Type[C[A]]) extends MemAlloc[A,C]

@op case class DRAMDynNew[A:Bits,C[T]]()(implicit tp: Type[C[A]]) extends MemAlloc[A,C] {
  def dims = Nil
}

@op case class DRAMAddress[A:Bits,C[T]](dram: DRAM[A,C]) extends Primitive[I64] {
  val A: Bits[A] = Bits[A]
}

@op case class DRAMAlloc[A:Bits,C[T]](dram: DRAM[A,C], dims: Seq[I32]) extends EnPrimitive[Void] {
  val A: Bits[A] = Bits[A]
  override var ens: Set[Bit] = Set.empty
  override def effects: Effects = Effects.Writes(dram)
}

@op case class DRAMDealloc[A:Bits,C[T]](dram: DRAM[A,C]) extends EnPrimitive[Void] {
  override var ens: Set[Bit] = Set.empty
  override def effects: Effects = Effects.Writes(dram)
}

@op case class SetMem[A:Bits,C[T]](dram: DRAM[A,C], data: Tensor1[A]) extends Op2[A,Void] {
  override def effects: Effects = Effects.Writes(dram)
}
@op case class GetMem[A:Bits,C[T]](dram: DRAM[A,C], data: Tensor1[A]) extends Op2[A,Void] {
  override def effects: Effects = Effects.Writes(data)
}
