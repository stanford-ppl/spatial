package spatial.executor.scala

import argon.{Block, Op, Sym, dbgs, emit, error, inGen, indentGen, info, stm}
import spatial.SpatialConfig
import spatial.node.AccelScope
import spatial.traversal.AccelTraversal
import spatial.metadata.control._
import spatial.metadata.debug._

case class ExecutorPass(IR: argon.State,
                        bytesPerTick: Int,
                        responseLatency: Int,
                        activeRequests: Int)
    extends AccelTraversal {

  private val shouldPrint = IR.config.enLog

  private val lineLength = 120
  override protected def process[R](block: Block[R]): Block[R] = {
    info(s"Starting scala simulation")
    val accelScopes = collection.mutable.Set.empty[Sym[_]]
    var time: Long = System.currentTimeMillis()
    IR.runtimeArgs.zipWithIndex.foreach {
      case (rtArgs, index) =>
        val splitArgs = rtArgs.split("\\W+")
        val executionState = new ExecutionState(
          Map.empty,
          splitArgs,
          new MemTracker,
          new MemoryController(bytesPerTick, responseLatency, activeRequests),
          new CycleTracker(),
          IR)
        var cycles = 0
        inGen(IR.config.genDir,
              s"SimulatedExecutionLog_${IR.paddedPass}_${index}") {
          emit(s"Starting Simulation with args: ${splitArgs.mkString("Array(", ", ", ")")}")
          block.stms.foreach {
            case accelScope @ Op(_: AccelScope) =>
              accelScopes.add(accelScope)
              val newTime = System.currentTimeMillis()
              info(s"Setup time: ${newTime - time} ms")
              time = newTime
              emit(s"Starting Accel Simulation".padTo(lineLength, "-").mkString)
              val exec = new AccelScopeExecutor(accelScope, executionState)
              while (exec.status != Done) {
                if (shouldPrint) {
                  emit(s"Tick $cycles".padTo(lineLength, "-").mkString)
                }
                exec.tick()
                if (shouldPrint) {
                  emit(s"Execution Status".padTo(lineLength, "-").mkString)
                  exec.print()
                }

                executionState.memoryController.tick()
                if (shouldPrint) {
                  emit(s"MemoryController".padTo(lineLength, "-").mkString)
                  executionState.memoryController.print(emit(_))
                }
                cycles += 1
                if (cycles % 10000 == 0) {
                  val newTime = System.currentTimeMillis()
                  info(s"$cycles elapsed [${newTime - time}ms]")
                  time = newTime
                }
              }
              info(s"Finished in $cycles")
            case stmt =>
              // These are top-level host operations
              emit(s"Executing host operation ${stm(stmt)}")
              executionState.runAndRegister(stmt)
          }
          emit(s"Concluding Simulation".padTo(lineLength, "-").mkString)
          // Iterate over all controllers in a DFS fashion
        }

        inGen(IR.config.genDir,
          s"SimulatedExecutionSummary_${IR.paddedPass}_${index}") {
          val printed = collection.mutable.Set.empty[Sym[_]]

          def recursiveCyclePrint(sym: Sym[_]): Unit = {
            emit(
              s"$sym [${sym.ctx}]: ${executionState.cycleTracker.controllers(sym)}")
            val data = executionState.cycleTracker.controllers(sym)
            val cycsPerIter = data.cycles / data.iterations
            sym.treeAnnotations =
              s"""
                 |<font color=\"red\"> $cycsPerIter cycles/iter<br>
                 |<font size="2">(${data.cycles} total cycles, ${data.iterations} total iters)<br></font> </font><br>""".stripMargin

            printed += sym
            indentGen {
              sym.children.foreach {
                case Ctrl.Node(s, _)
                  if !(printed contains s) && (executionState.cycleTracker.controllers contains s) =>
                  recursiveCyclePrint(s)
                case _ =>
              }
            }
          }
          accelScopes.foreach(recursiveCyclePrint)
          emit(s"ELAPSED CYCLES: $cycles")
        }
    }
    block
  }
}
