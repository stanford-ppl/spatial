package pcc.lang
package types

import forge._

abstract class Bits[A](id: Int)(implicit ev: A<:<Bits[A]) extends Prim[A](id) {
  def bits: Int
  @api def zero: A
  @api def one: A
}