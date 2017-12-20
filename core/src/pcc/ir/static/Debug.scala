package pcc.ir.static

import forge._
import pcc._

import pcc.ir.{PrintIf, AssertIf}

trait Debug { this: Implicits =>
  @api def println(msg: Text): Void = print(msg + "\n")
  @api def println(): Void = println("")

  @api def print(msg: Text): Void = stage(PrintIf(Nil,msg))

  @api def assert(cond: Bit): Void = stage(AssertIf(Nil,cond,None))
  @api def assert(cond: Bit, msg: Text): Void = stage(AssertIf(Nil,cond,Some(msg)))
}
