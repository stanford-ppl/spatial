package argon.node

import argon._
import argon.lang._
import forge.tags._

/** Concatenation of Text parts. */
@op case class TextConcat(parts: Seq[Text]) extends Primitive[Text] {
  override val canAccel: Boolean = false
}

/** Equality comparison of Text a and b. */
@op case class TextEql(a: Text, b: Text) extends Primitive[Bit] {
  override val canAccel: Boolean = false
}

/** Inequality comparison of Text a and b. */
@op case class TextNeq(a: Text, b: Text) extends Primitive[Bit] {
  override val canAccel: Boolean = false
}

@op case class TextLength(a: Text) extends Primitive[I32] {
  override val canAccel: Boolean = false
}

@op case class TextApply(a: Text, i: I32) extends Primitive[U8] {
  override val canAccel: Boolean = false
}

@op case class TextSlice(a: Text, start: I32, end: I32) extends Primitive[Text] {
  override val canAccel: Boolean = false
}

/** Generic conversion from any symbol to Text. */
@op case class GenericToText[A:Type](a: Sym[A]) extends Primitive[Text] {
  val A: Type[A] = Type[A]
  override val canAccel: Boolean = false
}
