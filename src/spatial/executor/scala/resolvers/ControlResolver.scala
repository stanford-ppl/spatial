package spatial.executor.scala.resolvers
import argon.{Exp, Op}
import spatial.executor.scala.{ControlExecutor, EmulResult, EmulUnit, ExecutionState, SimulationException}
import spatial.node.{CounterChainNew, CounterNew}
import spatial.metadata.control._

trait ControlResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = sym match {
    case Op(_: CounterNew[_]) | Op(_: CounterChainNew) =>
      EmulUnit(sym)

    case ctrl if ctrl.isControl =>
      val inAccel = ctrl.ancestors.exists {
        case Ctrl.Node(ctrl, _) => ctrl.isAccel
        case _ => false
      }

      if (inAccel) {
        throw SimulationException(s"Should not be calling this from an Accel scope, this is meant for host (untimed) simulation")
      }

      implicit val IR: argon.State = execState.IR
      val executor = ControlExecutor(ctrl, execState)
      while (!executor.status.isFinished) {
        executor.tick()
      }

      EmulUnit(ctrl)

    case _ =>
      super.run(sym, execState)
  }
}
