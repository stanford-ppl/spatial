package spatial.node

import forge.tags._
import argon._
import spatial.lang._

@op case class VarNew[A:Type](init: Option[A]) extends Alloc[Var[A]] {
  val A: Type[A] = Type[A]
  override def aliases  = Nil
  override def contains = syms(init)
  override def extracts = Nil
  override def effects  = Effects.Mutable
  override val debugOnly = true
}
@op case class VarRead[A:Type](v: Var[A]) extends Primitive[A] {
  val A: Type[A] = Type[A]
  override def aliases = Nil
  override def contains = Nil
  override def extracts = syms(v)
  override val isTransient: Boolean = true
  override val debugOnly: Boolean = true
}
@op case class VarAssign[A:Type](v: Var[A], x: A) extends Primitive[Void] {
  val A: Type[A] = Type[A]
  override def aliases = Nil
  override def contains = syms(x)
  override def extracts = syms(v)
  override val debugOnly: Boolean = true
}
