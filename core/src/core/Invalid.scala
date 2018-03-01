package core

import forge.tags._

@ref class Invalid(msg: String) extends Ref[Nothing,Invalid] {
  override def isPrimitive: Boolean = false
}

object Invalid extends Invalid("")
