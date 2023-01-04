package spatial.executor.scala.resolvers
import argon.{Exp, Op}
import spatial.executor.scala.{EmulResult, EmulUnit, ExecutionState}
import spatial.node.{CounterChainNew, CounterNew}

trait ControlResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = sym match {
    case Op(_: CounterNew[_]) | Op(_: CounterChainNew) =>
      EmulUnit(sym)
    case _ =>
      super.run(sym, execState)
  }
}
