package argon.node

import argon._
import argon.lang._

import emul.Bool
import forge.tags._


abstract class BitOp2[R:Type] extends Primitive[R]
abstract class BitOp1 extends BitOp2[Bit]
abstract class BitBinary(override val unstaged: (Bool,Bool) => Bool) extends BitOp1 with Binary[emul.Bool,Bit]
abstract class BitUnary(override val unstaged: Bool => Bool) extends BitOp1 with Unary[emul.Bool,Bit]

/** Bit */
@op case class Not(a: Bit) extends BitUnary(a => !a) {
  @rig override def rewrite: Bit = a match {
    case Op(Not(x)) => x
    case _ => super.rewrite
  }
}
@op case class And(a: Bit, b: Bit) extends BitBinary(_&&_) {
  override def absorber: Option[Bit] = Some(Bit(false))
  override def identity: Option[Bit] = Some(Bit(true))
  override def isAssociative: Boolean = true
}
@op case class Or(a: Bit, b: Bit) extends BitBinary(_||_) {
  override def absorber: Option[Bit] = Some(Bit(true))
  override def identity: Option[Bit] = Some(Bit(false))
  override def isAssociative: Boolean = true
}
@op case class Xor(a: Bit, b: Bit) extends BitBinary(_!==_) {
  override def identity: Option[Bit] = Some(Bit(false))
  override def isAssociative: Boolean = true
}
@op case class Xnor(a: Bit, b: Bit) extends BitBinary(_===_) {
  override def identity: Option[Bit] = Some(Bit(true))
  override def isAssociative: Boolean = true
}

@op case class TextToBit(t: Text) extends BitOp1 {
  override val canAccel: Boolean = false
}

@op case class BitToText(a: Bit) extends BitOp2[Text] {
  override val canAccel: Boolean = false
}

@op case class BitRandom(max: Option[Bit]) extends Primitive[Bit] {
  override def effects: Effects = Effects.Simple
}