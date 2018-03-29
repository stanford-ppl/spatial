package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._


trait CppGenDebug extends CppGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixToText(x) => emit(src"${lhs.tp} $lhs = std::to_string($x);")
    case TextConcat(strings) => emit(src"${lhs.tp} $lhs = string_plus(${strings.mkString(",")});")
    case PrintIf(cond,x)   => 
    	if (cond.isEmpty) emit(src"""std::cout << $x;""")
    	else emit(src"""if ( ${cond.toList.mkString(" & ")} ) std::cout << $x;""")
    case _ => super.gen(lhs, rhs)
  }


}
