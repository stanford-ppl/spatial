package spatial.executor.scala.resolvers
import argon._
import argon.node._
import spatial.executor.scala._
import spatial.node.Mux

trait MiscResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): EmulResult = {
    implicit val ir: argon.State = execState.IR
    op match {
      case TextConcat(parts) =>
        SimpleEmulVal(parts.map(execState.getValue[String](_)).mkString(""))

      case pi@PrintIf(ens, text) =>
        if (pi.isEnabled(execState)) {
          val stringToWrite = execState.getValue[String](text)
          emitNB(execState.getValue[String](text))
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

      case Mux(en, left, right) =>
        val select = execState.getValue[emul.Bool](en).value
        if (select) {
          execState(left)
        } else {
          execState(right)
        }

      case _ => super.run(sym, op, execState)
    }
  }
}
