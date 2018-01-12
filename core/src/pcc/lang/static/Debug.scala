package pcc.lang.static

import forge._
import pcc.lang.Dbg

trait Debug { this: Statics =>
  @api def println(msg: Text): Void = print(msg>"\n")
  @api def println(): Void = println("")

  @api def print(msg: Text): Void = Dbg.printIf(Nil, msg)

  @api def assert(cond: Bit): Void = Dbg.assertIf(Nil,cond,None)
  @api def assert(cond: Bit, msg: Text): Void = Dbg.assertIf(Nil,cond,Some(msg))
}
