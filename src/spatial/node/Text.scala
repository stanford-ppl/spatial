package spatial.node

import forge.tags._
import core._
import spatial.lang._

/** Concatenation of [[Text]] parts. **/
@op case class TextConcat(parts: Seq[Text]) extends Primitive[Text] {
  override val debugOnly: Boolean = true
}

/** Equality comparison of [[Text]] a and b. **/
@op case class TextEql(a: Text, b: Text) extends Primitive[Bit] {
  override val debugOnly: Boolean = true
}

/** Inequality comparison of [[Text]] a and b. **/
@op case class TextNeq(a: Text, b: Text) extends Primitive[Bit] {
  override val debugOnly: Boolean = true
}

/** Generic conversion from any symbol to Text. **/
@op case class GenericToText[A:Type](a: Sym[A]) extends Primitive[Text] {
  override val debugOnly: Boolean = true
}
