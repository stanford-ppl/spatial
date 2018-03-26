package spatial.codegen.scalagen

import argon.core._
import spatial.codegen.scalagen.ScalaCodegen
import spatial.aliases._
import spatial.nodes._

trait ScalaGenRange extends ScalaCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]) = rhs match {
    case RangeForeach(start, end, step, func, i) =>
      open(src"val $lhs = for ($i <- $start until $end by $step) {")
      emitBlock(func)
      close("}")
    case _ => super.gen(lhs, rhs)
  }

}
