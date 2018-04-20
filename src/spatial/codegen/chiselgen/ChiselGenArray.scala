package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}
import spatial.data._
import spatial.util._

trait ChiselGenArray extends ChiselGenCommon {

  protected def getField(tp: Type[_],field: String): (Int,Int) = tp match {
    case x: Struct[_] =>
      val idx = x.fields.indexWhere(_._1 == field)
      val width = bitWidth(x.fields(idx)._2)
      val prec = x.fields.take(idx)
      val precBits = prec.map{case (_,bt) => bitWidth(bt)}.sum
      (precBits+width-1, precBits)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecSlice(vec, start, stop) => 
      emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.zipWithIndex.foreach{case(w, i) => w := ${vec}(i+$stop)}")
    case VecConcat(list) => 
      emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})")
      list.zipWithIndex.foreach{case (a, i) => emitt(src"${lhs}($i) := $a") }
    case VecApply(vec, id) => 
      emitGlobalWireMap(src"${lhs}", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := ${vec}($id).r")
    case SimpleStruct(st) => 
      emitGlobalWireMap(src"val $lhs", src"Wire(${lhs.tp})")
      emitt(src"${lhs}.r := Cat(${st.map{f => src"${f._2}"}.mkString(",")})")

    case FieldApply(struct, field) => 
      emitGlobalWireMap(src"""val $lhs""", src"""Wire(${lhs.tp})""")
      val (start, end) = getField(struct.typeArgs.head, field)
      emitt(src"${lhs}.r := ${struct}(${start}, ${end})")

    case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {

    super.emitFooter()
  }

}