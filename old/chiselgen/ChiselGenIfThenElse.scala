package spatial.codegen.chiselgen

import argon.core._
import argon.nodes.IfThenElse

trait ChiselGenIfThenElse extends ChiselCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case IfThenElse(cond, thenp, elsep) =>
      open(src"val $lhs = {")
      open(src"if ($cond) { ")
      emitBlock(thenp)
      close("}")
      open("else {")
      emitBlock(elsep)
      close("}")
      close("}")

    case _ => super.gen(lhs, rhs)
  }

}
