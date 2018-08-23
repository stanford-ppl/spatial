package argon.node

import argon._
import argon.lang._
import forge.tags._

@op case class VarNew[A:Type](init: Option[A]) extends Alloc[Var[A]] {
  val A: Type[A] = Type[A]
  override def aliases = Nul
  override def contains= syms(init)
  override def extracts= Nul
  override def effects  = Effects.Mutable
  override val canAccel = false
}
@op case class VarRead[A:Type](v: Var[A]) extends Primitive[A] {
  val A: Type[A] = Type[A]
  override def aliases = Nul
  override def contains = Nul
  override def extracts = syms(v)
  override val isTransient: Boolean = true
  override val canAccel: Boolean = false

  @rig override def rewrite: A = {
    // Find the most recent write to this var in THIS current scope
    // Note that this assumes that all statements in a block are always executed.
    dbgs(s"Attempting to rewrite VarRead with current impure:")
    state.impure.foreach{case Impure(sym,_) =>
      dbgs(s"  ${stm(sym)}")
    }

    val write = state.impure.reverseIterator.collectFirst{case Impure(Def(VarAssign(v2,x)),_) if v == v2 => x }
    write match {
      case Some(x) => x.asInstanceOf[A]
      case None => super.rewrite
    }
  }
}
@op case class VarAssign[A:Type](v: Var[A], x: A) extends Primitive[Void] {
  val A: Type[A] = Type[A]
  override def effects: Effects = Effects.Writes(v)
  override def aliases = Nul
  override def contains = Nul
  override def extracts = Nul
  override val canAccel: Boolean = false
}
