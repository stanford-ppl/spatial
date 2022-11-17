package spatial.node

import argon._
import argon.node._
import forge.tags._
import spatial.lang._

@op case class CircNew[_A:Bits,_B:Bits](func: Lambda1[_A,_B]) extends Alloc[Circ[_A,_B]] {
  type A = _A
  type B = _B
  val evA: Bits[A] = implicitly[Bits[A]]
  val evB: Bits[B] = implicitly[Bits[B]]
}

@op case class CircApply[_A:Bits,_B:Bits](circ: Circ[_A,_B], id: Int, arg: _A) extends Primitive[_B] {
  type A = _A
  type B = _B
  val evA: Bits[A] = implicitly[Bits[A]]
  val evB: Bits[B] = implicitly[Bits[B]]
}
