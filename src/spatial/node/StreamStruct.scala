package spatial.node

import argon._
import argon.node.{Alloc, EnPrimitive, Primitive, StructAlloc}
import forge.tags._
import spatial.lang._

@op case class SimpleStreamStruct[S:StreamStruct](elems: Seq[(String,Sym[_])]) extends Primitive[S] {
  override val isTransient: Boolean = true
}

@op case class FieldDeq[S,A:Bits](struct: StreamStruct[S], field: String, ens: Set[Bit]) extends EnPrimitive[A] {
  override val isTransient: Boolean = false
  override def effects: Effects = Effects.Writes(struct)
}

// TODO: FieldEnq may not actually be used anywhere
@op case class FieldEnq[S,A:Type](struct: StreamStruct[S], field: String, data: A) extends Primitive[Void] {
  override def effects: Effects = Effects.Writes(struct)
  override val canAccel: Boolean = true
}
