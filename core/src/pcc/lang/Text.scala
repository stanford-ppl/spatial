package pcc.lang

import forge._
import pcc.core._
import pcc.node._

case class Text() extends Top[Text] {
  override type I = String
  override def fresh: Text = new Text
  override def isPrimitive: Boolean = true

  @api def >[A](that: Top[A]): Text = Text.concat(this,that.toText)
  @api def >[A](that: A): Text = that match {
    case t: Top[_] => Text.concat(this, t.toText)
    case t => Text.concat(this, Text.c(t.toString))
  }
  @api def >(that: Text): Text = Text.concat(this,that)
}
object Text {
  implicit val tp: Text = (new Text).asType
  def c(x: String): Text = const[Text](x)

  @rig def concat(a: Text, b: Text): Text = stage(TextConcat(a,b))
  @rig def textify[A:Sym](a: A): Text = stage(ToText(a))
}
