package pcc
package ir

import forge._

case class Text(eid: Int) extends Sym[Text](eid) {
  override type I = String

  override def fresh(id: Int): Text = Text(id)
  override def isPrimitive: Boolean = true
  override def stagedClass: Class[Text] = classOf[Text]

  @api def >[A](that: Sym[A]): Text = Text.concat(that.toText,this)
  @api def >[A](that: A): Text = Text.concat(Text.c(that.toString),this)
  @api def >(that: Text): Text = Text.concat(that,this)
}
object Text {
  implicit val text: Text = Text(-1)

  @api def c(x: String) = const[Text](x)
  @api def concat(a: Text, b: Text): Text = stage(TextConcat(a,b))
  @api def textify[A:Sym](a: A): Text = stage(ToText(a))
}


case class TextConcat(a: Text, b: Text) extends Op[Text] {
  def mirror(f:Tx) = Text.concat(f(a),f(b))
}

case class ToText[A:Sym](a: A) extends Op[Text] {
  def mirror(f:Tx) = Text.textify(f(a))
}