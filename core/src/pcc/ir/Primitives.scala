package pcc
package ir

import forge._

abstract class Prim[A](id: Int)(implicit ev: A<:<Prim[A]) extends Sym[A](id) {
  final override def isPrimitive: Boolean = true
}

abstract class Bits[A](id: Int)(implicit ev: A<:<Bits[A]) extends Prim[A](id) {
  def bits: Int
}

abstract class Num[A](id: Int)(implicit ev: A<:<Num[A]) extends Bits[A](id)









