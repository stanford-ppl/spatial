package nova.lang
package types

import nova.core.Top

abstract class Prim[A](implicit ev: A<:<Prim[A]) extends Top[A] {
  private implicit lazy val tA: Prim[A] = this.tp.view(this)

  final override def isPrimitive: Boolean = true
}
