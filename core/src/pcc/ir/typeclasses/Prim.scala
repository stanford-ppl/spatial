package pcc
package ir
package typeclasses

abstract class Prim[A](id: Int)(implicit ev: A<:<Prim[A]) extends Sym[A](id) {
  final override def isPrimitive: Boolean = true
}
