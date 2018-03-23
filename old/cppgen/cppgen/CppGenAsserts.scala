package argon.codegen.cppgen

import argon.core._
import argon.nodes._

trait CppGenAsserts extends CppCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case _ => super.remap(tp)
  }

  override protected def quoteConst(c: Const[_]): String = c match {
    case _ => super.quoteConst(c)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Assert(cond, m)       =>  
      val str = src"""${m.getOrElse("API assert failed with no message provided")}"""
      emit(src"""std::string $lhs = string_plus("\n=================\n", string_plus($str, "\n=================\n"));""")
      emit(src"""ASSERT($cond, ${lhs}.c_str());""")

    case _ => super.emitNode(lhs, rhs)
  }
}
