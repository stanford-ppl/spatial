package spatial.node

import forge.tags._
import argon._
import spatial.lang._

@op case class VarNew[T:Type](init: Option[T]) extends Alloc[Var[T]] {
  override def aliases  = Nil
  override def contains = syms(init)
  override def extracts = Nil
  override def effects  = Effects.Mutable
  override val debugOnly = true
}
@op case class VarRead[T:Type](v: Var[T]) extends Primitive[T] {
  override def aliases = Nil
  override def contains = Nil
  override def extracts = syms(v)
  override val isTransient: Boolean = true
  override val debugOnly: Boolean = true
}
@op case class VarAssign[T:Type](v: Var[T], x: T) extends Primitive[Void] {
  override def aliases = Nil
  override def contains = syms(x)
  override def extracts = syms(v)
  override val debugOnly: Boolean = true
}
