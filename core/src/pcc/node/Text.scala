package pcc.node

import forge._
import pcc.core._
import pcc.lang._

@op case class TextConcat(a: Text, b: Text) extends Op[Text]
@op case class ToText[A:Sym](a: A) extends Op[Text]
