package pcc.node

import forge._
import pcc.core._
import pcc.data._
import pcc.lang._

@op case class SimpleStruct[S:Struct](elems: Seq[(String,Sym[_])]) extends StructAlloc[S]

@op case class FieldApply[S:Struct,A:Sym](struct: S, name: String) extends Op[A]

@op case class FieldUpdate[S:Struct,A:Sym](struct: S, name: String, data: A) extends Op[Void] {
  override def effects: Effects = Effects.Writes(struct.asSym)
}
