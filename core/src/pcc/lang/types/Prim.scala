package pcc.lang
package types

import pcc.core.Sym

abstract class Prim[A](id: Int)(implicit ev: A<:<Prim[A]) extends Sym[A](id) {
  final override def isPrimitive: Boolean = true
}
