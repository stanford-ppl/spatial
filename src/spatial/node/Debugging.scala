package spatial.node

import argon.node.EnPrimitive
import argon._
import argon.lang._
import forge.tags._
import spatial.lang._

@op case class BreakIf(ens: Set[Bit]) extends EnPrimitive[Void] {
  override def effects = Effects.Global
}

@op case class BreakCtrlIf(ctrl: Sym[_], ens: Set[Bit]) extends EnPrimitive[Void] {
  override def effects = Effects.Global
}
