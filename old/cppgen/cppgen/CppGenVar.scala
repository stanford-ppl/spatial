package argon.codegen.cppgen

import argon.core._
import argon.nodes._

trait CppGenVar extends CppCodegen {

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case NewVar(init)    => emit(src"${lhs.tp.typeArguments.head} $lhs = $init;")
    case ReadVar(v)      => emit(src"${lhs.tp} $lhs = $v;")
    case AssignVar(v, x) => emit(src"$v = $x;")
    case _ => super.emitNode(lhs, rhs)
  }
}
