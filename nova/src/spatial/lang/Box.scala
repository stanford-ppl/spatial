package spatial.lang

abstract class Box[A](implicit ev: A <:< Box[A]) extends Ref[A] {
  override def isPrimitive = false
}
