package spatial.lang

import core._

abstract class Box[A](implicit ev: A <:< Box[A]) extends Top[A] with Ref[Any,A] {
  override def isPrimitive = false
}
