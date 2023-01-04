//package spatial.executor.scala
//
//import argon._
//import spatial.metadata.retiming._
//import spatial.metadata.control._
//import argon.node._
//import forge.tags.stateful
//import spatial.executor.scala.memories.ScalaReg
//import spatial.executor.scala.resolvers.OpResolver
//import spatial.node._
//
//abstract class BlockExecution[A] {
//  @stateful def tick(): Unit
//  def results: Seq[EmulResult]
//}
//
//// Captures the current execution state of a block
//// Multiple execStates represent unroll factors
//class OuterBlockExecution[A](blk: Block[A], execStates: Seq[ExecutionState]) extends BlockExecution[A] {
//  var stage = 0
//  private val stmIterator = blk.stms.toIterator
//
//  var currentExecutors: Seq[OpExecutorBase] = Seq.empty
//
//  private def dropDone(): Unit = {
//    currentExecutors = currentExecutors.filterNot(_.isDone)
//  }
//
//  @stateful override def tick(): Unit = {
//    emit(s"Ticking: $blk (currentExecutors = $currentExecutors)")
//    // If we don't have any current executors, execute until we get to a controller
//    if (currentExecutors.isEmpty) {
//      while (stmIterator.hasNext && currentExecutors.isEmpty) {
//        val next = stmIterator.next()
//        if (next.isControl) {
//          emit(s"Found Control: $next")
//          currentExecutors = execStates.map {
//            execState => ControlExecutor(next, execState)
//          }
//          dropDone()
//        } else {
//          emit(s"Non-Control: $next")
//          execStates.foreach {
//            execState =>
//              execState.register(OpResolver.run(next, execState))
//          }
//        }
//      }
//      cState = BlockState(completed = currentExecutors.isEmpty && stmIterator.isEmpty)
//      return
//    }
//
//    currentExecutors.foreach(_.tick())
//    dropDone()
//
//    // Update the controller state
//    cState = BlockState(completed = currentExecutors.isEmpty && stmIterator.isEmpty)
//  }
//
//  override def isStalled: Boolean = false
//
//  override def results: Seq[EmulResult] = execStates.map { execState => execState(blk.result) }
//}
//
//class InnerBlockExecution[A](blk: Block[A], val execStates: Seq[ExecutionState]) extends BlockExecution[A] {
//  private var curStage: Int = 0
//
//  // split the block down into timings
//  private val timingMap = blk.stms.groupBy(_.fullDelay.toInt)
//  val maxStages: Int = timingMap.keys.max
//
//  override def isStalled: Boolean = {
//    val fwdPressure = blk.stms.exists {
//      case _ => false
//    }
//    val backPressure = blk.stms.exists {
//      case _ => false
//    }
//
//    fwdPressure || backPressure
//  }
//
//  @stateful def tick(): Unit = {
//    emit(s"Ticking: $blk (stage = $curStage)")
//    assert(cState.running)
//    // If we're not stalled and not early terminated, then we can proceed
//
//    // Run the current steps
//    val currentSteps = timingMap.getOrElse(curStage, Seq.empty)
//    currentSteps.foreach {
//      sym =>
//        execStates.foreach {
//          execState =>
//            val castedSym: Sym[sym.R] = sym.asInstanceOf[Sym[sym.R]]
//            execState.register(OpResolver.run(castedSym, execState))
//        }
//    }
//
//    curStage += 1
//    if (curStage > maxStages) {
//      cState = BlockState(completed = true)
//    }
//  }
//
//  override def results: Seq[EmulResult] = execStates.map { execState => execState(blk.result) }
//}
