package spatial.node

import forge.tags._
import core._
import spatial.lang._

@op case class TextConcat(a: Text, b: Text) extends Primitive[Text] {
  override val debugOnly: Boolean = true
}
@op case class TextEql(a: Text, b: Text) extends Primitive[Bit] {
  override val debugOnly: Boolean = true
}
@op case class TextNeq(a: Text, b: Text) extends Primitive[Bit] {
  override val debugOnly: Boolean = true
}

@op case class GenericToText[A:Type](a: A) extends Primitive[Text] {
  override val debugOnly: Boolean = true
}