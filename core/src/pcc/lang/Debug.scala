package pcc.lang

import forge._
import pcc.core._
import pcc.node._

object Debug {
  @api def printIf(en: Seq[Bit], x: Text): Void = stage(PrintIf(en,x))
  @api def assertIf(en: Seq[Bit], cond: Bit, x: Option[Text]): Void = stage(AssertIf(en,cond,x))
}

