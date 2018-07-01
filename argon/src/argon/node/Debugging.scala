package argon.node

import argon._
import argon.lang._
import forge.tags._

@op case class PrintIf(ens: Set[Bit], x: Text) extends EnPrimitive[Void] {
  override def effects = Effects.Simple
  override val canAccel: Boolean = false
}

@op case class AssertIf(ens: Set[Bit], cond: Bit, x: Option[Text]) extends EnPrimitive[Void] {
  override def effects = Effects.Global
  override val canAccel: Boolean = false
}

@op case class BreakpointIf(ens: Set[Bit]) extends EnPrimitive[Void] {
  override def effects = Effects.Global
}

@op case class ExitIf(ens: Set[Bit]) extends EnPrimitive[Void] {
  override def effects = Effects.Global
}