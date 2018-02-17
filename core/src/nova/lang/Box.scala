package nova.lang

import nova.core.Top

abstract class Box[A](implicit ev: A <:< Box[A]) extends Top[A] {
  override def isPrimitive = false
}
