package nova.lang

import forge.tags._
import nova.core._
import nova.node._

object Debug {
  @api def printIf(en: Seq[Bit], x: Text): Void = stage(PrintIf(en,x))
  @api def assertIf(en: Seq[Bit], cond: Bit, x: Option[Text]): Void = stage(AssertIf(en,cond,x))
}

