package argon.codegen.cppgen

import argon.core._
import argon.nodes._

trait CppGenIfThenElse extends CppGenArray {

  protected def isVoidType(tp: Type[_]): Boolean = tp match {
    case UnitType => true
    case _ => false
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case IfThenElse(cond, thenp, elsep) =>
      if (isVoidType(lhs.tp)) {
        emit(src"// This ifthenelse returns void, ignore the return")
      } else if (isArrayType(lhs.tp)) { // Initialization of lhs is funky if we have array type
        emit(src"// TODO: IfThenElse to set array type is tricky, only works for 1D now")
        emit(src"${lhs.tp}* $lhs;")
      } else { 
        emit(src"${lhs.tp} $lhs;")
      }
      open(src"if ($cond) { ")
      emitBlock(thenp)
      if (!isVoidType(lhs.tp)) emit(src"$lhs = ${thenp.result};")
      closeopen("} else { ")
      emitBlock(elsep)
      if (!isVoidType(lhs.tp)) emit(src"$lhs = ${elsep.result};")
      close("}")


    case _ => super.emitNode(lhs, rhs)
  }

}
