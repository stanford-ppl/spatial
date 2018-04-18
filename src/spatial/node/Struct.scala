package spatial.node

import argon._
import forge.tags._

import spatial.lang._

abstract class StructAlloc[S:Struct] extends Primitive[S] {
  def elems: Seq[(String,Sym[_])]

  override def inputs = syms(elems.map(_._2))
  override def reads  = Nil
  override def aliases = Nil
  override def contains = syms(elems.map(_._2))
  override val isEphemeral: Boolean = true
}

@op case class SimpleStruct[S:Struct](elems: Seq[(String,Sym[_])]) extends StructAlloc[S]

@op case class FieldApply[S,A:Type](struct: Struct[S], field: String) extends Primitive[A] {
  override val isEphemeral: Boolean = true
}

@op case class FieldUpdate[S,A:Type](struct: Struct[S], field: String, data: A) extends Primitive[Void] {
  override def effects: Effects = Effects.Writes(struct)
  override val debugOnly: Boolean = true
}
