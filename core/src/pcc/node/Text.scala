package pcc.node

import forge._
import pcc.core._
import pcc.lang._

@op case class TextConcat(a: Text, b: Text) extends Primitive[Text] {
  override val debugOnly: Boolean = true
}
@op case class ToText[A](a: Sym[A]) extends Primitive[Text] {
  override val debugOnly: Boolean = true
}
@op case class TextEql(a: Text, b: Text) extends Primitive[Bit] {
  override val debugOnly: Boolean = true
}
@op case class TextNeq(a: Text, b: Text) extends Primitive[Bit] {
  override val debugOnly: Boolean = true
}
