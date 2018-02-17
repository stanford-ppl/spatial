package pcc.lang

import pcc.core.Top

abstract class Box[A](implicit ev: A <:< Box[A]) extends Top[A] {
  override def isPrimitive = false
}
