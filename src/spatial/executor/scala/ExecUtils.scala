package spatial.executor.scala

import argon.ExpType
import argon.lang._
import emul.{FixedPoint, FloatPoint}
import spatial.executor.scala.memories.ScalaStruct

object ExecUtils {
  def parse(string: Iterator[String], tp: ExpType[_, _]): EmulVal[_] = {
    tp match {
      case st: Struct[_] =>
        val fields = st.fields.map {
          case (s, t) => s -> parse(string, t)
        }
        ScalaStruct(fields.toMap, st.fields.map(_._1))
      case ft: Fix[_, _, _] =>
        SimpleEmulVal(FixedPoint(string.next(), ft.fmt.toEmul))
      case ft: FltPt[_, _] =>
        SimpleEmulVal(FloatPoint(string.next(), ft.fmt.toEmul))
    }
  }

  def serialize(tp: EmulVal[_], builder: StringBuilder = StringBuilder.newBuilder): StringBuilder = {
    tp match {
      case SimpleEmulVal(v: FixedPoint, _) =>
        builder.append(v.toString)
      case SimpleEmulVal(v: FloatPoint, _) =>
        builder.append(v)
      case ScalaStruct(fieldValues, fieldOrder) =>
        fieldOrder.foreach {
          field =>
            serialize(fieldValues(field), builder)
        }
    }
    builder
  }
}
