package spatial.codegen.chiselgen

import argon._
import argon.node._

trait ChiselGenVec extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecAlloc(elems) => emit(src"val $lhs = Vec($elems)")

    case VecSlice(vec, start, stop) =>
      emit(src"val $lhs = Wire(${lhs.tp})")
      emit(src"$lhs.zipWithIndex.foreach{case(w, i) => w := $vec(i+$stop)}")

    case VecConcat(list) => 
      emit(src"val $lhs = Wire(${lhs.tp})")
      emit(s"var ${lhs}_i = 0")
      list.reverse.zipWithIndex.foreach{case (a, i) => emit(s"${quote(a)}.reverse.zipWithIndex.foreach{case (a,i) => ${quote(lhs)}(${lhs}_i + i) := a}; ${lhs}_i = ${lhs}_i + ${quote(a)}.length") }

    case VecApply(vec, id) =>
      emit(src"val $lhs = Wire(${lhs.tp})")
      emit(src"$lhs.r := $vec($id).r")

    case _ => super.gen(lhs, rhs)
  }

}