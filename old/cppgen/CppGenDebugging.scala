package spatial.codegen.cppgen

import argon.codegen.cppgen.CppCodegen
import argon.core._
import spatial.aliases._
import spatial.nodes._

trait CppGenDebugging extends CppCodegen {

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case PrintIf(en,x)         => emit(src"""if ($en) { std::cout << $x; }""")
    case PrintlnIf(en,x)       => emit(src"""if ($en) { std::cout << $x << std::endl; }""")
    case _ => super.emitNode(lhs, rhs)
  }

}
