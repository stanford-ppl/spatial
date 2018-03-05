package spatial.lang

import core._
import forge.tags._

@ref class Void extends Top[Void] with Ref[Unit,Void] {
  // --- Infix Methods
  @api override def neql(that: Void): Bit = false
  @api override def eql(that: Void): Bit = true

  // --- Typeclass Methods
  override def isPrimitive: Boolean = true
}
object Void {
  def c: Void = uconst[Void](())
}
