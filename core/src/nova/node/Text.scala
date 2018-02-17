package nova.node

import forge.tags._
import nova.core._
import nova.lang._

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
