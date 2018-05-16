package spatial.codegen.scalagen

import argon._
import spatial.node.SeriesForeach

trait ScalaGenSeries extends ScalaCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case SeriesForeach(start,end,step,func) =>
      open(src"val $lhs = for (${func.input} <- $start until $end by $step) {")
        gen(func)
      close("}")

    case _ => super.gen(lhs,rhs)
  }

}
