package spatial.codegen
package pirgen

import argon._
import argon.codegen.StructCodegen
import argon.node._
import spatial.lang._
import spatial.node.CoalesceStoreParams

trait PIRGenStructs extends PIRCodegen {

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case e: StructAlloc[_] => 
      val elems = e.elems.toMap
      stateStruct(lhs, lhs.tp) { field => Lhs(elems(field.map{_._1}.get)) }

    // case e: CoalesceStoreParams => 
      // val elems = Seq(("base", e.base), ("len", e.len)).toMap
      // stateStruct(lhs, lhs.tp) { field => Lhs(elems(field.map{_._1}.get)) }
    case CoalesceStoreParams(base, len) =>
        stateStruct(lhs, lhs.tp) { field => if (field.get._1 == "_1") Lhs(base) else Lhs(len) }

    //case FieldUpdate(struct, field, value) => 
      //state(lhs, tp=Some("Unit"))(src"""$struct("$field") = $value""")
      
    case FieldApply(struct, field)         => 
      alias(lhs)(Lhs(struct, Some(field)))

    case _ => super.genAccel(lhs, rhs)
  }


}
