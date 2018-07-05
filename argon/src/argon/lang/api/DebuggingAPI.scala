package argon.lang.api

import argon._
import argon.node._
import forge.tags._

trait DebuggingAPI_Internal {
  @rig def printIf(en: Set[Bit], x: Text): Void = stage(PrintIf(en,x))
  @rig def assertIf(en: Set[Bit], cond: Bit, x: Option[Text]): Void = stage(AssertIf(en,cond,x))
  @rig def assertIf(cond: Bit, msg: Text): Void = assertIf(Set.empty, cond, Some(msg))

  @rig def exitIf(en: Set[Bit]): Void = stage(ExitIf(en))
  @rig def breakpointIf(en: Set[Bit]): Void = stage(BreakpointIf(en))

}

trait DebuggingAPI_Shadowing extends DebuggingAPI_Internal {

  @api def println(v: Any): Void = v match {
    case t: Top[_] => printIf(Set.empty, t.toText ++ "\n")
    case t         => printIf(Set.empty, Text(t.toString + "\n"))
  }
  @api def print(v: Any): Void = v match {
    case t: Top[_] => printIf(Set.empty, t.toText)
    case t         => printIf(Set.empty, Text(t.toString))
  }

  @api def println(): Void = println("")

  @api def assert(cond: Bit): Void = assertIf(Set.empty,cond,None)
  @api def assert(cond: Bit, msg: Text): Void = assertIf(Set.empty,cond,Some(msg))

  @api def breakpoint(): Void = breakpointIf(Set.empty)
  @api def exit(): Void = exitIf(Set.empty)


}
