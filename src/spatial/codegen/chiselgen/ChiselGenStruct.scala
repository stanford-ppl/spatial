package spatial.codegen.chiselgen

import argon._
import argon.node._
import spatial.lang._
import spatial.metadata.retiming._
import spatial.metadata.blackbox._
import spatial.metadata.control._
import spatial.node.{FieldDeq, SimpleStreamStruct}

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
//      emit(src"val $lhs = HVec(${st.reverse.map{f => src"${f._2}_decoupled.asDecoupledDriver" }.mkString("Seq(",",",")")})")

    case FieldDeq(struct, field, ens) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
//      val idx = struct.tp.asInstanceOf[StreamStruct[_]].fields.indexWhere{case (s,_) => s == field}
//      val (start, end) = getField(struct.tp, field)
      emit(src"""$lhs.r := $struct.get("$field").bits""")
      emit(src"""$struct.get("$field").ready := ${and(ens)} & ~$break && ${DL(src"$datapathEn & $iiIssue", lhs.fullDelay, true)}""")
      emit(src"""Ledger.connectStructPort($struct.hashCode, "$field")""")

//    case FieldApply(struct, field) if lhs.parent.s.exists(_.isBlackboxImpl) && struct.isBound =>
//      emit(createWire(quote(lhs),remap(lhs.tp)))
//      emit(src"$lhs.r := io.$field")

    case FieldApply(struct, field) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      val (start, end) = getField(struct.tp, field)
      emit(src"$lhs.r := $struct($start, $end)")

    case _ => super.gen(lhs,rhs)
  }

}