package spatial.node

import argon.node.Primitive
import forge.tags._
import spatial.lang._

@op case class DelayLine[A:Bits](size: Int, data: Bits[A]) extends Primitive[A]
