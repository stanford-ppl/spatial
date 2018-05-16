package spatial.lang

import argon._
import forge.tags._

@ref class Void extends Top[Void] with Ref[Unit,Void] {
  // --- Infix Methods
  @api override def neql(that: Void): Bit = false
  @api override def eql(that: Void): Bit = true

  // --- Typeclass Methods
  override protected def value(c: Any): Option[(Unit, Boolean)] = c match {
    case u: Unit => Some((u,true))
    case _ => super.value(c)
  }

  override protected val __isPrimitive: Boolean = true
}
object Void {
  def c: Void = uconst[Void](())
}
