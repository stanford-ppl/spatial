package spatial.node

import forge.tags._
import argon._
import spatial.lang._

@op case class VarNew[A:Type](init: Option[A]) extends Alloc[Var[A]] {
  val A: Type[A] = Type[A]
  override def aliases  = Nul
  override def contains = syms(init)
  override def extracts = Nul
  override def effects  = Effects.Mutable
  override val debugOnly = true
}
@op case class VarRead[A:Type](v: Var[A]) extends Primitive[A] {
  val A: Type[A] = Type[A]
  override def aliases = Nul
  override def contains = Nul
  override def extracts = syms(v)
  override val isEphemeral: Boolean = true
  override val debugOnly: Boolean = true
}
@op case class VarAssign[A:Type](v: Var[A], x: A) extends Primitive[Void] {
  val A: Type[A] = Type[A]
  override def aliases = Nul
  override def contains = syms(x)
  override def extracts = syms(v)
  override val debugOnly: Boolean = true
}
