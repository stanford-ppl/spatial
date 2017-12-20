package pcc
package ir

import data._

case class PrintIf(en: Seq[Bit], x: Text) extends Op[Void] {
  override def effects = Effects.Simple
}

case class AssertIf(en: Seq[Bit], cond: Bit, x: Option[Text]) extends Op[Void] {
  override def effects = Effects.Global
}