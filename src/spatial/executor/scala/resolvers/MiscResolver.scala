package spatial.executor.scala.resolvers
import argon.lang.Struct
import argon.{Exp, Op, emit, error}
import argon.node.{AssertIf, FixToText, PrintIf, SimpleStruct, TextConcat}
import emul.FixedPoint
import spatial.executor.scala.memories.{ScalaStruct, ScalaStructType}
import spatial.executor.scala.{EmulResult, EmulUnit, EmulVal, ExecutionState, SimpleEmulVal}

trait MiscResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = {
    implicit val ir: argon.State = execState.IR
    sym match {
      case Op(ftt: FixToText[_, _, _]) =>
        val a = execState.getValue[FixedPoint](ftt.a.asSym)
        SimpleEmulVal(a.toString)

      case Op(TextConcat(parts)) =>
        SimpleEmulVal(parts.map(execState.getValue[String](_)).mkString(""))

      case Op(pi@PrintIf(ens, text)) =>
        if (pi.isEnabled(execState)) {
          emit(execState.getValue[String](text))
        }

        EmulUnit(sym)

      case Op(ai@AssertIf(ens, cond, message)) =>
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

      case Op(SimpleStruct(elems)) =>
        ScalaStruct(elems.toMap.mapValues {
          v => execState(v) match {case ev: EmulVal[_] => ev}
        })


      case _ => super.run(sym, execState)
    }
  }
}
