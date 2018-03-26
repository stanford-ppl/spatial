package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenHostTransfer extends ScalaGenMemories {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case SetArgIn(reg, v) => emit(src"val $lhs = $reg.update(0, $v)")
    case GetArgOut(reg)   => emit(src"val $lhs = $reg.apply(0)")


    case _ => super.gen(lhs, rhs)
  }

}
