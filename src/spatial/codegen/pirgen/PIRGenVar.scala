package spatial.codegen.pirgen

import argon._
import spatial.node._
import spatial.lang._

trait PIRGenVar extends PIRCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp:Var[_] => src"Ptr[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@VarNew(init) => emitDummy(lhs, rhs)
    case VarRead(v)      => emitDummy(lhs, rhs)
    case VarAssign(v, x) => emitDummy(lhs, rhs)
    case _ => super.gen(lhs, rhs)
  }

}
