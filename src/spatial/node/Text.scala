package spatial.node

import forge.tags._
import argon._
import spatial.lang._

/** Concatenation of Text parts. */
@op case class TextConcat(parts: Seq[Text]) extends Primitive[Text] {
  override val debugOnly: Boolean = true
}

/** Equality comparison of Text a and b. */
@op case class TextEql(a: Text, b: Text) extends Primitive[Bit] {
  override val debugOnly: Boolean = true
}

/** Inequality comparison of Text a and b. */
@op case class TextNeq(a: Text, b: Text) extends Primitive[Bit] {
  override val debugOnly: Boolean = true
}

@op case class TextLength(a: Text) extends Primitive[I32] {
  override val debugOnly: Boolean = true
}

@op case class TextApply(a: Text, i: I32) extends Primitive[U8] {
  override val debugOnly: Boolean = true
}

@op case class TextSlice(a: Text, start: I32, end: I32) extends Primitive[Text] {
  override val debugOnly: Boolean = true
}

/** Generic conversion from any symbol to Text. */
@op case class GenericToText[A:Type](a: Sym[A]) extends Primitive[Text] {
  val A: Type[A] = Type[A]
  override val debugOnly: Boolean = true
}
