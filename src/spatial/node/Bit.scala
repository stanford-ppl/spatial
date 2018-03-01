package spatial.node

import forge.tags._
import core._
import spatial.lang._

abstract class BitOp2[R:Type] extends Primitive[R]
abstract class BitOp1 extends BitOp2[Bit]

/** Bit **/
@op case class Not(a: Bit) extends BitOp1
@op case class And(a: Bit, b: Bit) extends BitOp1
@op case class Or(a: Bit, b: Bit) extends BitOp1
@op case class Xor(a: Bit, b: Bit) extends BitOp1
@op case class Xnor(a: Bit, b: Bit) extends BitOp1

@op case class TextToBit(t: Text) extends BitOp1 {
  override val debugOnly: Boolean = true
}
@op case class BitToText(a: Bit) extends BitOp2[Text] {
  override val debugOnly: Boolean = true
}

@op case class BitRandom(max: Option[Bit]) extends Primitive[Bit] {
  override def effects: Effects = Effects.Simple
}