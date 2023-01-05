package spatial.executor.scala

import argon.{Op, Sym, emit, indentGen, stm}
import forge.tags.stateful
import spatial.node.{Dequeuer, Enqueuer, FIFODeq, FIFOEnq}

class ExecPipeline(val stages: Seq[PipelineStage])(implicit state: argon.State) {
  // Can a new workload be pushed in
  def canAcceptNewState: Boolean = stages.head.isEmpty
  def pushState(executionState: ExecutionState): Unit = {
    if (!canAcceptNewState) {
      throw new IllegalStateException(s"Can't push into a pipeline that's not accepting")
    }
    assert(canAcceptNewState, s"Can't push into a pipeline that's not accepting")
    stages.head.setExecution(executionState)
  }

  def tick(): Unit = {
    // Advance all stages by one tick
    emit(s"Advancing stages: $stages")
    indentGen {
      stages.foreach(_.tick())
    }

    // If the last stage is done, then we pop it off of the pipeline
    if (stages.last.isDone) {
      stages.last.currentExecution = None
    }

    indentGen {
      stages.foreach {
        stage =>
          emit(s"Stage: $stage")
          indentGen {
            emit(s"CurrentExec: ${stage.currentExecution}")
            emit(s"IsDone: ${stage.isDone}")
            stage.currentExecution match {
              case Some(exec) =>
                emit(s"WillStall: ${exec.willStall}")
              case None =>
            }
          }
      }
    }

    // Taking pairs from the end of the stages, advance if the next stage is empty and the current stage is done.
    stages.reverse.sliding(2).foreach {
      case Seq(later, earlier) =>
        emit(s"Attempting to shift: $earlier -> $later")
        (later.currentExecution, earlier.currentExecution) match {
          case (None, Some(execution)) if execution.isDone =>
            val currentState = execution.executionState
            later.setExecution(currentState)
            earlier.clearExecution()
            emit(s"Shifting: $earlier -> $later")
          case _ => // Pass, not ready to move on yet
        }
      case skip if skip.size < 2 => // Single or no items
    }
  }

  def isEmpty: Boolean = stages.forall(_.isEmpty)

  override def toString: String = {
    s"ExecPipeline(${stages.map(_.toString).mkString(", ")})"
  }
}

trait PipelineStage {
  var currentExecution: Option[PipelineStageExecution] = None
  def isEmpty: Boolean = currentExecution.isEmpty
  def tick(): Unit = currentExecution.foreach {
    execution => if (!execution.willStall && !execution.isDone) execution.tick()
  }

  // Marked as done only if we have an execution and it's done. otherwise check empty
  def isDone: Boolean = currentExecution.exists(_.isDone)

  @stateful def setExecution(executionState: ExecutionState): Unit = {
    assert(currentExecution.isEmpty, s"clear the execution first before setting a new one")
    currentExecution = Some(makeNewExecution(executionState))
    // Run first tick of cycle 0
    tick()
  }

  def clearExecution(): Unit = { currentExecution = None }

  @stateful def makeNewExecution(executionState: ExecutionState): PipelineStageExecution
}

trait PipelineStageExecution {
  def willStall: Boolean
  def tick(): Unit
  def isDone: Boolean
  val executionState: ExecutionState
}

class InnerPipelineStage(syms: Seq[Sym[_]]) extends PipelineStage {
  @stateful override def makeNewExecution(executionState: ExecutionState): PipelineStageExecution = new InnerPipelineStageExecution(syms, executionState)

  override def toString: String = {
    s"InnerPipelineStage(${syms.mkString(", ")})[$currentExecution]"
  }
}

class InnerPipelineStageExecution(syms: Seq[Sym[_]], override val executionState: ExecutionState)(implicit state: argon.State) extends PipelineStageExecution {

  private val containsStreamAccesses = {
    syms.exists {
      case Op(_: FIFOEnq[_]) => true
      case Op(_: FIFODeq[_]) => true
      case _ => false
    }
  }

  override def willStall: Boolean = {
    if (!containsStreamAccesses) { return false }

    throw new NotImplementedError(s"Haven't implemented stalling yet, found stream usage in $syms")
    syms.exists {
      case Op(enqueuer: Enqueuer[_]) =>
        false
      case Op(dequeuer: Dequeuer[_, _]) =>
        false
      case _ => false
    }
  }

  private var hasTicked = false
  override def tick(): Unit = {
    // Simply run and register all of the symbols
    // We don't worry about delay because it's run on a timed graph.
    indentGen {
      syms.foreach {
        sym =>
          executionState.log(s"Running: ${stm(sym)} [$executionState]")
          executionState.runAndRegister(sym)
      }
    }
    hasTicked = true
  }

  override def isDone: Boolean = hasTicked

  override def toString: String = {
    s"InnerPipelineStageExecution($syms) [done = $isDone]"
  }
}

class OuterPipelineStage(transients: Seq[Sym[_]], ctrl: Sym[_]) extends PipelineStage {
  @stateful override def makeNewExecution(executionState: ExecutionState): PipelineStageExecution = {
    transients.foreach(executionState.runAndRegister(_))
    new OuterPipelineStageExecution(ctrl, executionState)
  }

  override def toString: String = {
    s"OuterPipelineStage(${transients.mkString(", ")}, $ctrl)[$currentExecution]"
  }
}

class OuterPipelineStageExecution(ctrl: Sym[_], override val executionState: ExecutionState)(implicit state: argon.State) extends PipelineStageExecution {
  override def willStall: Boolean = false

  private var overhead = 4
  private val executor = ControlExecutor(ctrl, executionState)
  override def tick(): Unit = {
    if (overhead > 0) {
      overhead -= 1
    } else {
      indentGen {
        executor.tick()
      }
    }
  }

  override def isDone: Boolean = executor.isDone
}