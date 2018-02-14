package pcc.lang
package types

import forge._
import pcc.core._

abstract class Bits[A](implicit ev: A<:<Bits[A]) extends Prim[A] {
  def bits: Int
  @api def zero: A
  @api def one: A
}

object Bits {
  def unapply[A](x: Sym[A]): Option[Bits[A]] = x match {
    case b: Bits[_] => Some(b.asInstanceOf[Bits[A]])
    case _ => None
  }
}
