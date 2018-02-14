package pcc.lang.static

import forge._
import pcc.core._
import pcc.lang.Debug
import pcc.helpers.IRImplicits._

trait Debugs { this: Statics =>
  @api def println(msg: Text): Void = print(msg>"\n")
  @api def println(v: Top[_]): Void = println(v.toText)
  @api def println[A:Type](v: A): Void = println(v.asTop.toText)
  @api def println(): Void = println("")

  @api def print(msg: Text): Void = Debug.printIf(Nil, msg)

  @api def assert(cond: Bit): Void = Debug.assertIf(Nil,cond,None)
  @api def assert(cond: Bit, msg: Text): Void = Debug.assertIf(Nil,cond,Some(msg))
}
