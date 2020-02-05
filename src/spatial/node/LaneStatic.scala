package spatial.node

import argon.node._
import forge.tags._
import spatial.lang._

@op case class LaneStatic[A:Bits](iter: A, elems: Seq[scala.Int]) extends Primitive[A] {
  override val isTransient: Boolean = true

  @stateful def simplify[A:Bits](x: A, value: scala.Int): A = {
    x.from(value)
  }

  @rig override def rewrite: A = if (elems.size == 1) simplify(iter, elems.head) else super.rewrite
}
