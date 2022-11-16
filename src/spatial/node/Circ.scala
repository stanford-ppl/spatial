package spatial.node

import argon.node._
import forge.tags._
import spatial.lang._

@op case class CircNew[A:Bits,B:Bits](func: A => B) extends Alloc[Circ[A,B]]

@op case class CircApply[A:Bits,B:Bits](circ: Circ[A,B], id: Int, arg: A) extends Primitive[B]
