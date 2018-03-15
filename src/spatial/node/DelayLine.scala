package spatial.node

import core._
import forge.tags._
import spatial.lang._

@op case class DelayLine[A:Bits](size: Int, data: Bits[A]) extends Primitive[A]
