package pcc.node

import forge._
import pcc.core._
import pcc.lang._

@op case class NewVar[T:Sym](init: Option[T])(implicit val tp: Sym[Var[T]]) extends Alloc[Var[T]] {
  override def aliases = Nil
  override def contains = syms(init)
  override def extracts = Nil
  override val debugOnly = true
}
@op case class ReadVar[T:Sym](v: Var[T]) extends Primitive[T] {
  override def aliases = Nil
  override def contains = Nil
  override def extracts = syms(v)
  override val isStateless: Boolean = true
  override val debugOnly: Boolean = true
}
@op case class AssignVar[T:Sym](v: Var[T], x: T) extends Primitive[Void] {
  override def aliases = Nil
  override def contains = syms(x)
  override def extracts = syms(v)
  override val debugOnly: Boolean = true
}
