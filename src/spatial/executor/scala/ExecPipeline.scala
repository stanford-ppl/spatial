package spatial.executor.scala

import argon.lang.Bit
import argon.node.Enabled
import argon.{Op, Sym, dbgs, emit, indentGen, stm}
import forge.tags.stateful
import spatial.executor.scala.memories.ScalaQueue
import spatial.node.{Dequeuer, Enqueuer, FIFODeq, FIFOEnq, LIFOPop, StreamInRead, StreamOutWrite}

import scala.collection.{mutable => cm}

class ExecPipeline(val stages: Seq[PipelineStage])(implicit state: argon.State) {
  // Can a new workload be pushed in
  def canAcceptNewState: Boolean = stages.head.isEmpty
  def pushState(executionStates: Seq[ExecutionState]): Unit = {
    if (!canAcceptNewState) {
      throw new IllegalStateException(s"Can't push into a pipeline that's not accepting")
    }
    stages.head.setExecution(executionStates)
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
            val currentStates = execution.executionStates
            later.setExecution(currentStates)
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

  @stateful def setExecution(executionStates: Seq[ExecutionState]): Unit = {
    assert(currentExecution.isEmpty, s"clear the execution first before setting a new one")
    currentExecution = Some(makeNewExecution(executionStates))
    // Run first tick of cycle 0
    tick()
  }

  def clearExecution(): Unit = { currentExecution = None }

  def makeNewExecution(executionState: Seq[ExecutionState]): PipelineStageExecution
}

trait PipelineStageExecution {
  def willStall: Boolean
  def tick(): Unit
  def isDone: Boolean
  val executionStates: Seq[ExecutionState]
  implicit def IR: argon.State = executionStates.head.IR
}

class InnerPipelineStage(syms: Seq[Sym[_]]) extends PipelineStage {
  override def makeNewExecution(executionStates: Seq[ExecutionState]): PipelineStageExecution = new InnerPipelineStageExecution(syms, executionStates)

  override def toString: String = {
    s"InnerPipelineStage(${syms.mkString(", ")})[$currentExecution]"
  }
}

class InnerPipelineStageExecution(syms: Seq[Sym[_]], override val executionStates: Seq[ExecutionState]) extends PipelineStageExecution {

  private val enableSyms = (syms.collect {
    case Op(s: FIFOEnq[_]) => s.ens
    case Op(s: FIFODeq[_]) => s.ens
    case Op(s: StreamInRead[_]) => s.ens
    case Op(s: StreamOutWrite[_]) => s.ens
  }).flatten

  def isEnabled(ens: Set[Bit], executionState: ExecutionState): Boolean = {
    ens.forall(executionState.getValue[emul.Bool](_).value)
  }

  object SpeculativeOOB extends Exception with scala.util.control.NoStackTrace

  private def preEvaluateSym(sym: Sym[_], readIndex: cm.Map[Sym[_], Int], writeIndex: cm.Map[Sym[_], Int], execState: ExecutionState): Unit = {
    // Run and register all of its dependencies that weren't run already
    sym.inputs.filterNot(execState.values.contains(_)).foreach(preEvaluateSym(_, readIndex, writeIndex, execState))

    sym match {
      case Op(s: FIFODeq[_]) if isEnabled(s.ens, execState) =>
        // We can't actually dequeue, so we peek it instead
        val index = readIndex.getOrElseUpdate(s.mem, 0)
        readIndex(s.mem) += 1
        val result = execState(s.mem) match {
          case sq: ScalaQueue[SomeEmul] =>
            if (sq.size < index) {throw SpeculativeOOB}
            sq.queue(index)
        }
        execState.register(sym, result)
      case Op(s: StreamInRead[_]) if isEnabled(s.ens, execState) =>
        val index = readIndex.getOrElseUpdate(s.mem, 0)
        readIndex(s.mem) += 1
        val result = execState(s.mem) match {
          case sq: ScalaQueue[SomeEmul] =>
            if (sq.size < index) {throw SpeculativeOOB}
            sq.queue(index)
        }
        execState.register(sym, result)
      case Op(s: LIFOPop[_]) =>
        throw new NotImplementedError(s"Haven't implemented LIFOs yet")
      case _ => execState.runAndRegister(sym)
    }
  }

  override def willStall: Boolean = {
    val readIndex = cm.Map.empty[Sym[_], Int]
    val writeIndex = cm.Map.empty[Sym[_], Int]

    try {
      val queuesAndCounts = executionStates.flatMap {
        executionStateOld =>
          // fork the execution state
          val executionState = executionStateOld.copy()

          // evaluate all of the enable conditions that we need
          enableSyms.foreach(preEvaluateSym(_, readIndex, writeIndex, executionState))

          syms.collect {
            case Op(FIFOEnq(mem, _, ens)) if isEnabled(ens, executionState) =>
              executionState(mem) match {
                case sq: ScalaQueue[_] => (sq, 1, 0)
              }
            case Op(StreamOutWrite(mem, _, ens)) if isEnabled(ens, executionState) =>
              executionState(mem) match {
                case sq: ScalaQueue[_] => (sq, 1, 0)
              }
            case Op(FIFODeq(mem, ens)) if isEnabled(ens, executionState) =>
              executionState(mem) match {
                case sq: ScalaQueue[_] => (sq, 0, 1)
              }
            case Op(StreamInRead(mem, ens)) if isEnabled(ens, executionState) =>
              executionState(mem) match {
                case sq: ScalaQueue[_] => (sq, 0, 1)
              }
          }
      }
      val canExecute = queuesAndCounts.groupBy(_._1).forall {
        case (queue, tmp) =>
          val enqs = tmp.map(_._2).sum
          val deqs = tmp.map(_._3).sum

          ((queue.size + enqs) <= queue.capacity) && (queue.size >= deqs)
      }
      !canExecute
    } catch {
      case SpeculativeOOB => true
    }
  }

  private var hasTicked = false
  override def tick(): Unit = {
    // Simply run and register all of the symbols
    // We don't worry about delay because it's run on a timed graph.
    indentGen {
      syms.foreach {
        sym =>
          executionStates.foreach(_.runAndRegister(sym))
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
  override def makeNewExecution(executionStates: Seq[ExecutionState]): PipelineStageExecution = {
    executionStates.foreach(exec => transients.foreach(exec.runAndRegister(_)))
    new OuterPipelineStageExecution(ctrl, executionStates)
  }

  override def toString: String = {
    s"OuterPipelineStage(${transients.mkString(", ")}, $ctrl)[$currentExecution]"
  }
}

class OuterPipelineStageExecution(ctrl: Sym[_], override val executionStates: Seq[ExecutionState]) extends PipelineStageExecution {
  override def willStall: Boolean = false

  private var overhead = 4
  private val executors = executionStates.map(ControlExecutor(ctrl, _))
  override def tick(): Unit = {
    if (overhead > 0) {
      overhead -= 1
    } else {
      indentGen {
        executors.map(_.tick())
      }
    }
  }

  override def isDone: Boolean = {
    executors.forall {
      executor =>
        executor.status match {
          case _: Finished => true
          case Running => false
        }
    }
  }
}
