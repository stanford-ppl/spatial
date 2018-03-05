package spatial.internal

import core._
import forge.tags._

import spatial.lang._
import spatial.node._

trait Debug {
  @rig def printIf(en: Seq[Bit], x: Text): Void = stage(PrintIf(en,x))
  @rig def assertIf(en: Seq[Bit], cond: Bit, x: Option[Text]): Void = stage(AssertIf(en,cond,x))

}
