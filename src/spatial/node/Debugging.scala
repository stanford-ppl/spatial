package spatial.node

import forge.tags._
import argon._
import spatial.data._
import spatial.lang._

@op case class PrintIf(ens: Set[Bit], x: Text) extends EnPrimitive[Void] {
  override def effects = Effects.Simple
  override val debugOnly: Boolean = true
}

@op case class AssertIf(ens: Set[Bit], cond: Bit, x: Option[Text]) extends EnPrimitive[Void] {
  override def effects = Effects.Global
  override val debugOnly: Boolean = true
}

@op case class BreakpointIf(ens: Set[Bit]) extends EnPrimitive[Void] {
  override def effects = Effects.Global
}

@op case class ExitIf(ens: Set[Bit]) extends EnPrimitive[Void] {
  override def effects = Effects.Global
}