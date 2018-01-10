package pcc
package ir

import forge._

case class Text(eid: Int) extends Sym[Text](eid) {
  override type I = String

  override def fresh(id: Int): Text = Text(id)
  override def isPrimitive: Boolean = true
  override def stagedClass: Class[Text] = classOf[Text]

  @api def +(that: Text): Text = Text.concat(this,that)
}
object Text {
  implicit val text: Text = Text(-1)

  @api def concat(a: Text, b: Text): Text = stage(TextConcat(a,b))
}


case class TextConcat(a: Text, b: Text) extends Op[Text] {
  def mirror(f:Tx) = Text.concat(f(a),f(b))
}
