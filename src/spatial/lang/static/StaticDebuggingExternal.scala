package spatial.lang
package static

import argon._
import forge.tags._
import utils.implicits.collections._

import spatial.internal.{assertIf, printIf, exitIf, breakpointIf}
import spatial.node.TextConcat

trait StaticDebuggingExternal {
  @api def println(v: Any): Void = v match {
    case t: Top[_] => printIf(Set.empty, t.toText ++ "\n")
    case t         => printIf(Set.empty, t.toString + "\n")
  }
  @api def print(v: Any): Void = v match {
    case t: Top[_] => printIf(Set.empty, t.toText)
    case t         => printIf(Set.empty, t.toString)
  }

  @api def println(): Void = println("")

  @api def assert(cond: Bit): Void = assertIf(Set.empty,cond,None)
  @api def assert(cond: Bit, msg: Text): Void = assertIf(Set.empty,cond,Some(msg))

  @api def breakpoint(): Void = breakpointIf(Set.empty)
  @api def exit(): Void = exitIf(Set.empty)

  @api def sleep(cycles: I32): Void = Foreach(cycles by 1){_ =>  }


  implicit class Quoting(sc: StringContext) {
    @api def r(args: Any*): Text = {
      val quotedArgs = args.toArray.map{
        case t: Top[_] => t.toText
        case t         => Text(t.toString)
      }
      val quotedParts = sc.parts.map(Text.apply)

      val str = quotedParts.interleave(quotedArgs)
      stage(TextConcat(str))
    }
  }
}
