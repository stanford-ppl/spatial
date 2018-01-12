package pcc.lang

import pcc.core.Sym

abstract class Box[A](eid: Int)(implicit ev: A <:< Box[A]) extends Sym[A](eid) {
  override def isPrimitive = false
}
