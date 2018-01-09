package pcc
package ir

import forge._
import pcc.data.Effects

abstract class Struct[T:Struct](eid: Int)(implicit ev: T <:< Struct[T]) extends Sym[T](eid) {
  @internal def field[A:Sym](name: String): A = Struct.field[T,A](me, name)
}

object Struct {
  @internal def apply[S:Struct](fields: (String,Sym[_])*): S = stage(SimpleStruct[S](fields))
  @internal def field[S:Struct,A:Sym](struct: S, name: String): A = stage(FieldApply[S,A](struct,name))
  @internal def field_update[S:Struct,A:Sym](struct: S, name: String, data: A): Void = stage(FieldUpdate[S,A](struct,name,data))
}


abstract class StructAlloc[S:Struct] extends Op[S] {
  def elems: Seq[(String,Sym[_])]

  override def inputs = syms(elems.map(_._2))
  override def reads  = Nil
  override def aliases = Nil
  override def contains = syms(elems.map(_._2))
}

case class SimpleStruct[S:Struct](elems: Seq[(String,Sym[_])]) extends StructAlloc[S]

case class FieldApply[S:Struct,A:Sym](struct: S, name: String) extends Op[A]

case class FieldUpdate[S:Struct,A:Sym](struct: S, name: String, data: A) extends Op[Void] {
  override def effects: Effects = Effects.Writes(struct.asSym)
}
