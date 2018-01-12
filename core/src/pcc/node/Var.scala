package pcc.node

import forge._
import pcc.core._
import pcc.lang._

@op case class NewVar[T:Sym](init: T)(implicit val tp: Sym[Var[T]]) extends Op[Var[T]] {
  override def aliases = Nil
  override def contains = syms(init)
  override def extracts = Nil
}
@op case class ReadVar[T:Sym](v: Var[T]) extends Op[T] {
  override def aliases = Nil
  override def contains = Nil
  override def extracts = syms(v)
}
@op case class AssignVar[T:Sym](v: Var[T], x: T) extends Op[Void] {
  override def aliases = Nil
  override def contains = syms(x)
  override def extracts = syms(v)
}
