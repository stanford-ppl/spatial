package spatial.codegen.chiselgen

import argon.core._
import argon.nodes._
import argon.util.escapeString

trait ChiselGenString extends ChiselCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case StringType => "String"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(c: Const[_]): String = c match {
    case Const(c: String) => escapeString(c)
    case _ => super.quoteConst(c)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ToString(x) => emit(src"val $lhs = $x.toString")
    case StringConcat(x,y) => emit(src"val $lhs = $x + $y")
    case StringEquals(x,y) => emit(src"val $lhs = $x == $y")
    case StringDiffer(x,y) => emit(src"val $lhs = $x != $y")
    case StringSlice(a,b,c) => emit(src"""val $lhs = "don't care" """)
    case Char2Int(x) =>
      emit(src"val $lhs = ${x}(0).toInt.FP(true, 8, 0) // Assume we only convert constants")
    case _ => super.gen(lhs, rhs)
  }

}
