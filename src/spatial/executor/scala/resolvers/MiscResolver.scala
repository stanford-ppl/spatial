package spatial.executor.scala.resolvers
import argon.{Exp, Op}
import argon.node.{AssertIf, FixToText, PrintIf, TextConcat}
import emul.FixedPoint
import spatial.executor.scala.{EmulResult, EmulUnit, ExecutionState, SimpleEmulVal}
import utils.Result.Fail

trait MiscResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = sym match {

    case Op(ftt: FixToText[_, _, _]) =>
      val a = execState.getValue[FixedPoint](ftt.a.asSym)
      SimpleEmulVal(a.toString)

    case Op(TextConcat(parts)) =>
      SimpleEmulVal(parts.map(execState.getValue[String](_)).mkString(""))

    case Op(PrintIf(ens, text)) =>
      if (ens.forall(execState.getValue[Boolean](_))) {
        execState.log(execState.getValue[String](text))
      }

      EmulUnit(sym)

    case Op(AssertIf(ens, cond, message)) =>
      if (ens.forall(execState.getValue[Boolean](_))) {
        // Check the condition
        val condBool = execState.getValue[Boolean](cond)
        if (!condBool) {
          execState.error(s"FAILED ASSERT AT ${sym.ctx}")
          message.foreach {
            text => execState.error(execState.getValue[String](text))
          }
        }
      }

      EmulUnit(sym)

    case _ => super.run(sym, execState)
  }
}
