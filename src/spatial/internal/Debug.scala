package spatial.internal

import argon._
import forge.tags._

import spatial.lang._
import spatial.node._

trait Debug {
  @rig def printIf(en: Set[Bit], x: Text): Void = stage(PrintIf(en,x))
  @rig def assertIf(en: Set[Bit], cond: Bit, x: Option[Text]): Void = stage(AssertIf(en,cond,x))
  @rig def assertIf(cond: Bit, msg: Text): Void = assertIf(Set.empty, cond, Some(msg))

  @rig def exitIf(en: Set[Bit]): Void = stage(ExitIf(en))
  @rig def breakpointIf(en: Set[Bit]): Void = stage(BreakpointIf(en))

}
