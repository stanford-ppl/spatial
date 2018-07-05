package spatial.codegen
package scalagen

import argon._
import argon.codegen.StructCodegen
import argon.node._
import spatial.lang._

trait ScalaGenStructs extends ScalaCodegen with StructCodegen with ScalaGenBits {

  override def invalid(tp: Type[_]): String = tp match {
    case struct: Struct[_] => src"""$struct(${struct.fields.map(_._2).map(invalid).mkString(", ")})"""
    case _ => super.invalid(tp)
  }

  protected def structName(tp: Struct[_], idx: Int): String = s"Struct$idx"

  protected def emitStructDeclaration(name: String, tp: Struct[_]): Unit = {
    open(src"case class $name(")
      tp.fields.foreach{case (field,t) => emit(src"var $field: $t,") }
    close(") {")
    open("")
      emit("override def productPrefix = \"" + tp.typeName + "\"")
    close("}")
  }

  protected def emitDataStructures(): Unit = if (encounteredStructs.nonEmpty) {
    inGen(getOrCreateStream(out, "Structs.scala")) {
      emitHeader()
      for ((tp, name) <- encounteredStructs) {
        emitStructDeclaration(name, tp)
        emit("")
      }
      emitFooter()
    }
  }
  

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case e: StructAlloc[_] => emit(src"val $lhs: ${e.R} = new ${e.R}(${e.elems.map(_._2)})")
    case FieldUpdate(struct, field, value) => emit(src"val $lhs = $struct.$field = $value")
    case FieldApply(struct, field)         => emit(src"val $lhs = $struct.$field")

    case _ => super.gen(lhs, rhs)
  }


}
