package spatial.codegen.tsthgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.util.spatialConfig
import spatial.codegen.cppgen._

trait TungstenHostGenAccel extends CppGenAccel {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    case AccelScope(func) => 
      emit("REPL Top(&DUT, std::cout);")
      emit("RunAccel();")

    case ExitIf(en) => 
      // Emits will only happen if outside the accel
      emit(src"exit(1);")
      if (inHw) earlyExits = earlyExits :+ lhs

    case AssertIf(en, cond, m) => 
      // Emits will only happen if outside the accel
      val str = src"""${m.getOrElse("\"API assert failed with no message provided\"")}"""
      emit(src"""string $lhs = ("\n=================\n" + ($str + "\n=================\n"));""")
      val enable = if (en.toList.isEmpty) "true" else en.map(quote).mkString("&")
      emit(src"""if ($enable) { ASSERT($cond, ${lhs}.c_str()); }""")
      if (inHw) earlyExits = earlyExits :+ lhs

    case BreakpointIf(en) => 
      // Emits will only happen if outside the accel
      emit(src"exit(1);")
      if (inHw) earlyExits = earlyExits :+ lhs
      
    case _ => super.gen(lhs, rhs)
  }

}
