package argon

import forge.tags._

@ref class Invalid(msg: String) extends Ref[Nothing,Invalid] {
  override protected val __isPrimitive: Boolean = false
}

object Invalid extends Invalid("") {
  this.rhs = Def.TypeRef
}
