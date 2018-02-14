package pcc.node

import forge.op
import pcc.lang._

@op case class Mux[A:Bits](s: Bit, a: A, b: A) extends Primitive[A]
