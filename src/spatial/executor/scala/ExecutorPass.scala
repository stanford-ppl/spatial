package spatial.executor.scala

import argon.{Block, Op, emit, error, inGen}
import spatial.SpatialConfig
import spatial.node.AccelScope
import spatial.traversal.AccelTraversal


case class ExecutorPass(IR: argon.State, bytesPerTick: Int, responseLatency: Int, activeRequests: Int) extends AccelTraversal {
  private val lineLength = 120
  override protected def process[R](block: Block[R]): Block[R] = {
    inGen(IR.config.genDir, s"SimulatedExecutionLog_${IR.paddedPass}") {
      val executionState = new ExecutionState(Map.empty, new MemTracker, new MemoryController(bytesPerTick, responseLatency, activeRequests), IR)
      var cycles = 0
      block.stms.foreach {
        case accelScope@Op(_: AccelScope) =>
          emit(s"Starting Accel Simulation".padTo(lineLength, "-").mkString)
          val exec = new AccelScopeExecutor(accelScope, executionState)
          while (exec.status != Done) {
            emit(s"Tick $cycles".padTo(lineLength, "-").mkString)
            exec.tick()
            emit(s"Execution Status".padTo(lineLength, "-").mkString)
            exec.print()
            emit(s"MemoryController".padTo(lineLength, "-").mkString)
            executionState.memoryController.tick()
            executionState.memoryController.print(emit(_))
            cycles += 1
          }
        case stmt =>
          // These are top-level host operations
          executionState.runAndRegister(stmt)
      }
      emit(s"Concluding Simulation".padTo(lineLength, "-").mkString)
      emit(s"ELAPSED CYCLES: $cycles")
    }
    block
  }
}
