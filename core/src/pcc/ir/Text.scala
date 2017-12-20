package pcc
package ir

import forge._

case class Text(eid: Int) extends Sym[Text](eid) {
  override type I = String

  override def fresh(id: Int): Text = Text(id)
  override def isPrimitive: Boolean = true
  override def stagedClass: Class[Text] = classOf[Text]

  @api def +(that: Text): Text = stage(TextConcat(this, that))
}
object Text {
  implicit val text: Text = Text(-1)
}


case class TextConcat(a: Text, b: Text) extends Op[Text]
