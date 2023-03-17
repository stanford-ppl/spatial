package spatial.executor.scala.resolvers
import argon._
import argon.node.Enabled
import spatial.executor.scala.{EmulPoison, EmulResult, ExecutionState}

trait DisabledResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): EmulResult = op match {
    case enabled: Enabled[_] if !enabled.isEnabled(execState) =>
      EmulPoison(sym)
    case _ => super.run(sym, op, execState)
  }

}
