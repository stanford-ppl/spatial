package spatial.lang
package static

import core._
import forge.tags._
import utils.implicits.collections._

import spatial.internal.{assertIf, printIf}
import spatial.node.TextConcat

trait StaticDebugsExternal { this: ExternalStatics =>
  @api def println(v: Any): Void = v match {
    case t: Top[_] => printIf(Nil, t.toText ++ "\n")
    case t         => printIf(Nil, t.toString + "\n")
  }
  @api def print(v: Any): Void = v match {
    case t: Top[_] => printIf(Nil, t.toText)
    case t         => printIf(Nil, t.toString)
  }

  @api def println(): Void = println("")

  @api def assert(cond: Bit): Void = assertIf(Nil,cond,None)
  @api def assert(cond: Bit, msg: Text): Void = assertIf(Nil,cond,Some(msg))

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
