package pcc.node

import forge._
import pcc.data.Effects
import pcc.lang._

/** Bit **/
@op case class Not(a: Bit) extends Primitive[Bit]
@op case class And(a: Bit, b: Bit) extends Primitive[Bit]
@op case class Or(a: Bit, b: Bit) extends Primitive[Bit]
@op case class Xor(a: Bit, b: Bit) extends Primitive[Bit]
@op case class Xnor(a: Bit, b: Bit) extends Primitive[Bit]

@op case class BitRandom(max: Option[Bit]) extends Primitive[Bit] {
  override def effects: Effects = Effects.Simple
}