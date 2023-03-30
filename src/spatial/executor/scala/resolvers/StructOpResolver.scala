package spatial.executor.scala.resolvers
import argon._
import argon.node._
import spatial.executor.scala.memories.ScalaStruct
import spatial.executor.scala.{EmulResult, EmulVal, ExecutionState}

trait StructOpResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): EmulResult = op match {
    case SimpleStruct(elems) =>
      ScalaStruct(elems.toMap.mapValues(execState(_) match {case ev: EmulVal[_] => ev}), elems.map(_._1))
    case FieldApply(struct, field) =>
      execState(struct) match {
        case structVal: ScalaStruct => structVal.fieldValues(field)
      }
    case _ => super.run(sym, op, execState)
  }
}
