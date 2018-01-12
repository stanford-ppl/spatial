package pcc.node

import forge._
import pcc.core._
import pcc.data._
import pcc.lang._

@op case class PrintIf(en: Seq[Bit], x: Text) extends Op[Void] {
  override def effects = Effects.Simple
}

@op case class AssertIf(en: Seq[Bit], cond: Bit, x: Option[Text]) extends Op[Void] {
  override def effects = Effects.Global
}