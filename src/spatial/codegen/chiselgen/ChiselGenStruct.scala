package spatial.codegen.chiselgen

import argon._
import argon.node._
import spatial.lang._

trait ChiselGenStruct extends ChiselGenCommon {

  override protected def remap(tp: Type[_]): String = tp match {
    case _: Struct[_] => s"UInt(${bitWidth(tp)}.W)"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case SimpleStruct(st) =>
      emit(src"val $lhs = Wire(${lhs.tp})")
      emit(src"$lhs.r := Cat(${st.reverse.map{f => if (bitWidth(f._2.tp) > 1) src"${f._2}.r" else src"${f._2}"}.mkString(",")})")

    case FieldApply(struct, field) =>
      emit(src"val $lhs = Wire(${lhs.tp})")
      val (start, end) = getField(struct.tp, field)
      emit(src"$lhs.r := $struct($start, $end)")

    case _ => super.gen(lhs,rhs)
  }

}