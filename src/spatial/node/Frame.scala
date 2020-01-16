package spatial.node

import argon._
import argon.node._
import forge.tags._

import spatial.lang._

abstract class FrameNew[A:Bits,C[T]](implicit C: Type[C[A]]) extends MemAlloc[A,C]

@op case class FrameHostNew[A:Bits,C[T]](dims: Seq[I32], zero: A, stream: Sym[_])(implicit tp: Type[C[A]]) extends FrameNew[A,C]

@op case class FrameAddress[A:Bits,C[T]](dram: Frame[A,C]) extends Primitive[I64] {
  val A: Bits[A] = Bits[A]
}

@op case class FrameIsAlloc[A:Bits,C[T]](dram: Frame[A,C]) extends Primitive[Bit]

@op case class FrameAlloc[A:Bits,C[T]](dram: Frame[A,C], dims: Seq[I32]) extends EnPrimitive[Void] {
  val A: Bits[A] = Bits[A]
  override var ens: Set[Bit] = Set.empty
  override def effects: Effects = Effects.Writes(dram)
}

@op case class FrameDealloc[A:Bits,C[T]](dram: Frame[A,C]) extends EnPrimitive[Void] {
  override var ens: Set[Bit] = Set.empty
  override def effects: Effects = Effects.Writes(dram)
}

@op case class SetFrame[A:Bits,C[T]](dram: Frame[A,C], data: Tensor1[A]) extends Op2[A,Void] {
  override def effects: Effects = Effects.Writes(dram)
}
@op case class GetFrame[A:Bits,C[T]](dram: Frame[A,C], data: Tensor1[A]) extends Op2[A,Void] {
  override def effects: Effects = Effects.Writes(data)
}

