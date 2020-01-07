package spatial.codegen.chiselgen

import argon._
import argon.node._
import spatial.lang._
import spatial.metadata.retiming._
import spatial.node.{FIFODeqInterface, FieldDeq, SimpleStreamStruct}

trait ChiselGenStruct extends ChiselGenCommon {

  override protected def remap(tp: Type[_]): String = tp match {
    case _: Struct[_] => s"UInt(${bitWidth(tp)}.W)"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case SimpleStruct(st) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := Cat(${st.reverse.map{f => if (bitWidth(f._2.tp) > 1) src"${f._2}.r" else src"${f._2}"}.mkString(",")})")

    case SimpleStreamStruct(st) =>
      val inportString = st.map { case (name, typ) => src""" ("$name" -> ${bitWidth(typ.tp)}) """ }.mkString("Map(", ",", ")")
      emit(src"val $lhs = Wire(Flipped(new StreamStructInterface($inportString)))")
      st.foreach { case (field, s) =>
        emit(src"""$lhs.get("$field").bits := $s.r""")
        emit(src"""$lhs.get("$field").valid := ${s}.valid""")
        emit(src"""$lhs.getActive("$field").out := ${s}.activeOut""")
        emit(src"""$s.ready := $lhs.get("$field").ready""")
        emit(src"""$s.activeIn := $lhs.getActive("$field").in""")
      }

    case FieldDeq(struct, field, ens) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"""$lhs.r := $struct.get("$field").bits""")
      emit(src"""$struct.get("$field").ready := ${and(ens)} & ~$break && ${DL(src"$datapathEn & $iiIssue", lhs.fullDelay, true)}""")
      emit(src"""$struct.getActive("$field").in := ${and(ens)}""")
      emit(src"""Ledger.connectStructPort($struct.hashCode, "$field")""")

    case FieldApply(struct, field) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      val (start, end) = getField(struct.tp, field)
      emit(src"$lhs.r := $struct($start, $end)")

    case _ => super.gen(lhs,rhs)
  }

}