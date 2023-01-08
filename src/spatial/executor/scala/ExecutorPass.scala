package spatial.executor.scala

import argon.{Block, Op, emit, error, inGen}
import spatial.node.AccelScope
import spatial.traversal.AccelTraversal


case class ExecutorPass(IR: argon.State) extends AccelTraversal {
  override protected def process[R](block: Block[R]): Block[R] = {
    inGen(IR.config.genDir, s"SimulatedExecutionLog_${IR.paddedPass}") {
      val executionState = new ExecutionState(Map.empty, new MemTracker, IR)
      var cycles = 0
      block.stms.foreach {
        case accelScope@Op(_: AccelScope) =>
          val exec = new AccelScopeExecutor(accelScope, executionState)
          while (exec.status != Done) {
            exec.tick()
            cycles += 1
            emit(s"-" * 100)
            if (cycles >= 1000) {
              throw new Exception(s"Infinite loop detected")
            }
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
