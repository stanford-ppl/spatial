package argon.codegen.cppgen

import argon.core._
import argon.nodes._

trait CppGenPrint extends CppCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]) = rhs match {
    case Print(x)   => emit(src"""std::cout << $x;""")
    case Println(x) => emit(src"""std::cout << $x << std::endl;""")
    case _ => super.gen(lhs, rhs)
  }
}
