package pcc.lang
package types

import pcc.core.Top

abstract class Prim[A](implicit ev: A<:<Prim[A]) extends Top[A] {
  final override def isPrimitive: Boolean = true
}
