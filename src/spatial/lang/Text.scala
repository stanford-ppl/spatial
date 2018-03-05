package spatial.lang

import core._
import forge.tags._

import spatial.node._

@ref class Text extends Top[Text] with Ref[String,Text] {
  // --- Infix methods
  @api override def neql(that: Text): Bit = stage(TextNeq(this,that))
  @api override def eql(that: Text): Bit = stage(TextEql(this,that))

  @api override def toText: Text = this

  // --- Typeclass Methods
  def isPrimitive: Boolean = true
}
object Text {
  def c(x: String): Text = uconst[Text](x)

  @rig def concat(a: Text, b: Text): Text = stage(TextConcat(a,b))
}
