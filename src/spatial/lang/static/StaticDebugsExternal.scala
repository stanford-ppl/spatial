package spatial.lang
package static

import forge.tags._
import spatial.internal.{printIf,assertIf}

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

  @rig def convertToText(x: Any): Text = x match {
    case t: Top[_] => t.toText
    case t         => t.toString
  }

  implicit class Quoting(sc: StringContext) {
    @api def r(args: Any*): Text = {
      val quoted = args.map(convertToText)
      sc.parts.foldLeft((Text.c(""),0)){
        case ((str,i),"") => (str ++ quoted(i), i+1)
        case ((str,i), p) => (str ++ p, i)
      }._1
    }
  }
}
