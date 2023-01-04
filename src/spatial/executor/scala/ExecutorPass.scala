package spatial.executor.scala

import argon.{Block, Op, emit, inGen}
import spatial.node.AccelScope
import spatial.traversal.AccelTraversal

case class ExecutorPass(IR: argon.State) extends AccelTraversal {
  override protected def process[R](block: Block[R]): Block[R] = {
    inGen(IR.config.genDir, s"SimulatedExecutionLog_${IR.paddedPass(4)}") {
      val executionState = new ExecutionState(Map.empty, emit(_))
      var cycles = 0
      block.stms.foreach {
        case accelScope@Op(_: AccelScope) =>
          val exec = new AccelScopeExecutor(accelScope, executionState)
          while (!exec.isDone) {
            exec.tick()
            cycles += 1
            emit(s"-" * 100)
          }
        case stmt =>
          // These are top-level host operations
          executionState.runAndRegister(stmt)
      }
      emit(s"ELAPSED CYCLES: $cycles")
    }
    block
  }
}
