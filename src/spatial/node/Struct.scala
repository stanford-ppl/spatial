package spatial.node

import argon._
import forge.tags._

import spatial.lang._

abstract class StructAlloc[S:Struct] extends Primitive[S] {
  def elems: Seq[(String,Sym[_])]

  override def reads  = Nil
  override def aliases = Nul
  override def contains = syms(elems.map(_._2))

  override val isTransient: Boolean = true
}

@op case class SimpleStruct[S:Struct](elems: Seq[(String,Sym[_])]) extends StructAlloc[S]

@op case class FieldApply[S,A:Type](struct: Struct[S], field: String) extends Primitive[A] {
  override val isTransient: Boolean = true

  @rig override def rewrite: A = struct match {
    case Op(SimpleStruct(fields)) if !struct.isMutable =>
      fields.find(_._1 == field) match {
        case Some((_,f)) => f.asInstanceOf[A]
        case None =>
          warn(ctx, s"$field does not appear to be a field of ${struct.tp}")
          warn(ctx)
          super.rewrite
      }
    case _ => super.rewrite
  }
}

@op case class FieldUpdate[S,A:Type](struct: Struct[S], field: String, data: A) extends Primitive[Void] {
  override def effects: Effects = Effects.Writes(struct)
  override val debugOnly: Boolean = true
}
