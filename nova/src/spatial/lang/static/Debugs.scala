package spatial.lang.static

import forge.tags._
import spatial.lang.Debug

trait Debugs { this: ExternalStatics =>
  @api def println(v: Any): Void = v match {
    case t: Text   => Debug.printIf(Nil, t ++ "\n")
    case t: Ref[_] => Debug.printIf(Nil, t.toText ++ "\n")
    case t         => Debug.printIf(Nil, t.toString + "\n")
  }
  @api def print(v: Any): Void = v match {
    case t: Ref[_]  => Debug.printIf(Nil, t.toText)
    case t          => Debug.printIf(Nil, t.toString)
  }

  @api def println(): Void = println("")

  @api def assert(cond: Bit): Void = Debug.assertIf(Nil,cond,None)
  @api def assert(cond: Bit, msg: Text): Void = Debug.assertIf(Nil,cond,Some(msg))

  @rig def convertToText(x: Any): Text = x match {
    case t: Ref[_] => t.toText
    case t => t.toString
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
