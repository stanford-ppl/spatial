package pcc
package ir

/**
  * A (optionally stateful) black box
  */
abstract class Box[A](eid: Int)(implicit ev: A <:< Box[A]) extends Sym[A](eid) {
  final override def isPrimitive: Boolean = false
}

abstract class BoxAlloc[A](implicit tA: Sym[A]) extends Op[A]
