package spatial.codegen.chiselgen

import argon._
import argon.node._

trait ChiselGenVec extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecAlloc(elems) => emitGlobalWire(src"val $lhs = Vec($elems)")

    case VecSlice(vec, start, stop) =>
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"$lhs.zipWithIndex.foreach{case(w, i) => w := $vec(i+$stop)}")

    case VecConcat(list) => 
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(s"var ${lhs}_i = 0")
      list.zipWithIndex.foreach{case (a, i) => emitt(s"${quote(a)}.zipWithIndex.foreach{case (a,i) => ${quote(lhs)}(${lhs}_i + i) := a}; ${lhs}_i = ${lhs}_i + ${quote(a)}.length") }

    case VecApply(vec, id) =>
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"$lhs.r := $vec($id).r")

    case _ => super.gen(lhs, rhs)
  }

}