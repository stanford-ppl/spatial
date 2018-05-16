package spatial.lang

import argon._
import emul.Bool
import forge.tags._

import spatial.node._

@ref class Bit extends Top[Bit] with Bits[Bit] with Ref[Bool,Bit] {
  // --- Infix Methods
  @api def unary_!(): Bit = stage(Not(this))
  @api def &(that: Bit): Bit = stage(And(this,that))
  @api def &&(that: Bit): Bit = this & that
  @api def |(that: Bit): Bit = stage(Or(this,that))
  @api def ||(that: Bit): Bit = this | that
  @api def ^(that: Bit): Bit = stage(Xor(this,that))

  @api override def neql(that: Bit): Bit = stage(Xor(this,that))
  @api override def eql(that: Bit): Bit = stage(Xnor(this,that))

  // --- Typeclass Methods
  override val box: Bit <:< Bits[Bit] = implicitly[Bit <:< Bits[Bit]]
  override val __neverMutable: Boolean = true

  @rig def nbits: Int = 1
  @rig def zero: Bit = this.from(false)
  @rig def one: Bit = this.from(true)
  @rig def random(max: Option[Bit]): Bit = stage(BitRandom(max))

  override protected def value(c: Any) = c match {
    case b: Bool => Some(b,true)
    case "false" | "0" | 0 | false => Some(Bool(false),true)
    case "true" | "1" | 1 | true   => Some(Bool(true),true)
    case _:Int|_:Short|_:Byte|_:Char|_:Long => Some(Bool(true), false)
    case _:Float|_:Double => Some(Bool(true), false)
    case _ => super.value(c)
  }
  override protected def extract: Option[Any] = this.c.map(_.value)

  @api override def toText: Text = stage(BitToText(this))
}

object Bit {
  def apply(x: Boolean): Bit = uconst[Bit](Bool(x))
  def apply(x: Bool): Bit = uconst[Bit](x)
}

object BitType {
  def unapply(x: ExpType[_,_]): Boolean = x.isInstanceOf[Bit]
}
