package spatial.lang
package types

abstract class Prim[A](implicit ev: A<:<Prim[A]) extends Ref[A] {
  private implicit lazy val tA: Prim[A] = this.tp.view(this)

  final override def isPrimitive: Boolean = true
}
