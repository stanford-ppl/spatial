package spatial.lang

import forge.tags._
import core._
import spatial.node._

import nova.implicits.views._

case class Text() extends Ref[Text] {
  override type I = String
  override def fresh: Text = new Text
  override def isPrimitive: Boolean = true

  @api override def !==(that: Text): Bit = stage(TextNeq(this,that))
  @api override def ===(that: Text): Bit = stage(TextEql(this,that))
}
object Text {
  implicit val tp: Text = (new Text).asType
  def c(x: String): Text = const[Text](x)

  @rig def concat(a: Text, b: Text): Text = stage(TextConcat(a,b))
  @rig def textify[A:Type](a: A): Text = stage(ToText(a.viewSym))
}
