package spatial.node

import argon.Effects
import argon.node.Primitive
import forge.tags._
import spatial.lang._

@op case class SplitterStart(addr: Seq[I32]) extends Primitive[Void] {
  override def effects = Effects.Simple
}

@op case class SplitterEnd(addr: Seq[I32]) extends Primitive[Void] {
  override def effects = Effects.Simple
}