package spatial.lang

import core._
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
  override def isPrimitive: Boolean = true
  override def nbits: Int = 1

  @rig def zero: Bit = this.from(false)
  @rig def one: Bit = this.from(true)
  @rig def random(max: Option[Bit]): Bit = stage(BitRandom(max))

  @rig override def cnst(c: Any, checked: Boolean = true): Option[Bool] = c match {
    case b: Bool => Some(b)
    case "false" | "0" | 0 | false => Some(Bool(false))
    case "true" | "1" | 1 | true   => Some(Bool(true))
    case _:Int|_:Short|_:Byte|_:Char|_:Long => withCheck(Bool(true),checked){_ => true}
    case _:Float|_:Double => withCheck(Bool(true),checked){_ => true}
    case _ => None
  }
}

object Bit {

}
