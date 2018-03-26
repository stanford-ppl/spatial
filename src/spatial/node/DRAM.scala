package spatial.node

import argon._
import forge.tags._

import spatial.lang._

@op case class DRAMNew[A:Bits,C[T]](dims: Seq[I32])(implicit tp: Type[C[A]]) extends MemAlloc[A,C]

@op case class GetDRAMAddress[A:Bits,C[T]](dram: DRAM[A,C]) extends Primitive[I64]

@op case class SetMem[A:Bits,C[T]](dram: DRAM[A,C], data: Tensor1[A]) extends Op2[A,Void] {
  override def effects: Effects = Effects.Writes(dram)
}
@op case class GetMem[A:Bits,C[T]](dram: DRAM[A,C], data: Tensor1[A]) extends Op2[A,Void] {
  override def effects: Effects = Effects.Writes(data)
}
