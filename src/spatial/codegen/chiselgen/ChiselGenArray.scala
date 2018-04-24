package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}
import spatial.data._
import spatial.util._

trait ChiselGenArray extends ChiselGenCommon {


  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecSlice(vec, start, stop) => 
      emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.zipWithIndex.foreach{case(w, i) => w := ${vec}(i+$stop)}")
    case VecConcat(list) => 
      emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})")
      emitt(src"var ${lhs}_i = 0")
      list.zipWithIndex.foreach{case (a, i) => emitt(src"${a}.zipWithIndex.foreach{case (a,i) => ${lhs}(${lhs}_i + i) := a}; ${lhs}_i = ${lhs}_i + ${a}.length") }
    case VecApply(vec, id) => 
      emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := ${vec}($id).r")
    case SimpleStruct(st) => 
      emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := Cat(${st.reverse.map{f => if (bitWidth(f._2.tp) > 1) src"${f._2}.r" else src"${f._2}"}.mkString(",")})")

    case FieldApply(struct, field) => 
      emitGlobalWireMap(src"""$lhs""", src"""Wire(${lhs.tp})""")
      val (start, end) = getField(struct.tp, field)
      emitt(src"${lhs}.r := ${struct}(${start}, ${end})")

    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {

    super.emitFooter()
  }

}