package spatial.codegen.cppgen

import argon.codegen.cppgen.CppCodegen
import argon.core._
import spatial.aliases._
import spatial.nodes._

trait CppGenRange extends CppCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case RangeForeach(start, end, step, func, i) =>
      open(src"for (int $i = $start; $i < $end; $i = $i + $step) {")
      emitBlock(func)
      close("}")

    case _ =>
    	super.gen(lhs,rhs)
  }
}