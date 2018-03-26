package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenDelays extends ScalaCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case DelayLine(size,data) => emit(src"val $lhs = $data")
    case _ => super.gen(lhs, rhs)
  }

}
