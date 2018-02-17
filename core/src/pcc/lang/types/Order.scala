package pcc.lang
package types

import forge._


abstract class Order[A](implicit ev: A <:< Order[A]) extends Bits[A] {
  private implicit lazy val tA: Order[A] = this.tp.view(this)

  @api def <(that: A): Bit
  @api def <=(that: A): Bit
  @api def >(that: A): Bit = that < me
  @api def >=(that: A): Bit = that <= me

  @rig def min(a: A, b: A): A
  @rig def max(a: A, b: A): A
}
