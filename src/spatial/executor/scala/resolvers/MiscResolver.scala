package spatial.executor.scala.resolvers
import argon.lang.Struct
import argon._
import argon.node._
import emul.FixedPoint
import spatial.executor.scala.memories.{ScalaStruct, ScalaStructType}
import spatial.executor.scala.{EmulResult, EmulUnit, EmulVal, EmulVector, ExecutionState, SimpleEmulVal, SomeEmul}
import spatial.node.{Mux, RetimeGate}

trait MiscResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): EmulResult = {
    implicit val ir: argon.State = execState.IR
    op match {
      case TextConcat(parts) =>
        SimpleEmulVal(parts.map(execState.getValue[String](_)).mkString(""))

      case pi@PrintIf(ens, text) =>
        if (pi.isEnabled(execState)) {
          val stringToWrite = execState.getValue[String](text)
          emit(execState.getValue[String](text))
          info(stringToWrite)
        }

        EmulUnit(sym)

      case ai@AssertIf(ens, cond, message) =>
        if (ai.isEnabled(execState)) {
          // Check the condition
          val condBool = execState.getValue[emul.Bool](cond).value
          if (!condBool) {
            emit(s"FAILED ASSERT AT ${sym.ctx}")
            message.foreach {
              text => emit(execState.getValue[String](text))
            }
            error(s"FAILED ASSERTION")
          }
        }

        EmulUnit(sym)

      case SimpleStruct(elems) =>
        ScalaStruct(elems.toMap.mapValues {
          v => execState(v) match {case ev: EmulVal[_] => ev}
        })

      case Mux(en, left, right) =>
        val select = execState.getValue[emul.Bool](en).value
        if (select) {
          execState(left)
        } else {
          execState(right)
        }

      case RetimeGate() =>
        EmulUnit(sym)

      case VecApply(vec, i) =>
        execState(vec) match {
          case EmulVector(value, _) =>
            value(i).asInstanceOf[SomeEmul]
        }

      case _ => super.run(sym, op, execState)
    }
  }
}
