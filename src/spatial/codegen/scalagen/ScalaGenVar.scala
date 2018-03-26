package spatial.codegen.scalagen

import argon._
import spatial.node._

trait ScalaGenVar extends ScalaCodegen {
  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VarNew(init)    => emit(src"var $lhs = $init")
    case VarRead(v)      => emit(src"val $lhs = $v")
    case VarAssign(v, x) => emit(src"val $lhs = { $v = $x }")
    case _ => super.gen(lhs, rhs)
  }
}
