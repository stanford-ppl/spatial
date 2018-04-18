package spatial.codegen.scalagen

import argon._
import spatial.node._

trait PIRGenIfThenElse extends PIRCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case IfThenElse(cond, thenp, elsep) =>
      open(src"val $lhs = {")
        open(src"if ($cond) { ")
          ret(thenp)
        close("}")
        open("else {")
          ret(elsep)
        close("}")
      close("}")

    case _ => super.gen(lhs, rhs)
  }

}
