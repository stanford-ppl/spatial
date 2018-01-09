package pcc
package ir
package typeclasses

abstract class Bits[A](id: Int)(implicit ev: A<:<Bits[A]) extends Prim[A](id) {
  def bits: Int
}