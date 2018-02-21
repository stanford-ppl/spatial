package spatial.node

import forge.tags._
import core._
import nova.data._
import spatial.lang._

abstract class StructAlloc[S:Struct] extends Primitive[S] {
  def elems: Seq[(String,Sym[_])]

  override def inputs = syms(elems.map(_._2))
  override def reads  = Nil
  override def aliases = Nil
  override def contains = syms(elems.map(_._2))
  override val isStateless: Boolean = true
}

@op case class SimpleStruct[S:Struct](elems: Seq[(String,Sym[_])]) extends StructAlloc[S]

@op case class FieldApply[S:Struct,A:Type](struct: S, name: String) extends Primitive[A] {
  override val isStateless: Boolean = true
}

@op case class FieldUpdate[S:Struct,A:Type](struct: S, name: String, data: A) extends Primitive[Void] {
  override def effects: Effects = Effects.Writes(typ[S].viewAsSym(struct))
  override val debugOnly: Boolean = true
}
