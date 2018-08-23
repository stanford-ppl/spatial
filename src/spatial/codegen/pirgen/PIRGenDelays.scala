package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenDelays extends PIRCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case DelayLine(size,data@Const(_)) => // Don't emit anything here (NamedCodegen takes care of this)

    case DelayLine(size,data) => emit(src"val $lhs = $data")
    case _ => super.gen(lhs, rhs)
  }

}
