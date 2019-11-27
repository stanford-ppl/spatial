package spatial.node

import argon.Effects
import argon.node.Primitive
import forge.tags._
import spatial.lang._

@op case class DelayLine[A:Bits](size: Int, data: Bits[A]) extends Primitive[A] {
  @rig override def rewrite: A = if (size == 0) data.unbox else super.rewrite
}

@op case class RetimeGate() extends Primitive[Void] {
  override def effects = Effects.Simple
}
