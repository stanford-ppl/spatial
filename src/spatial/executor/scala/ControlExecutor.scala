package spatial.executor.scala
import argon.lang.{Bit, Idx}
import argon._
import emul.{Bool, FixedPoint, FixedPointRange}
import forge.tags.stateful
import spatial.executor.scala.memories.ScalaReg
import spatial.executor.scala.resolvers.OpResolver
import spatial.lang.{CounterChain, Reg}
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.retiming._

import scala.collection.{mutable => cm}

case class TransientsAndControl(transients: Seq[Sym[_]], control: Sym[_])

private object ControlExecutorUtils {

  // Computes the iteration offsets
  // Iterations of ctrler
  // -- iter -> iter value
  @stateful def computeIters(cchain: CounterChain, execState: ExecutionState): Seq[Map[Sym[Idx], FixedPoint]] = {
    val ranges = cchain.counters.map {
      ctr =>
        val fStart = execState.getValue[FixedPoint](ctr.start)
        val fEnd = execState.getValue[FixedPoint](ctr.end)
        val fStep = execState.getValue[FixedPoint](ctr.step)
        val par = ctr.ctrParOr1

        FixedPointRange(fStart, fEnd, fStep * par, isInclusive = false)
    }

    val iters = cchain.counters.flatMap(_.iter).map(_.asInstanceOf[Sym[Idx]])
    spatial.util.crossJoin(ranges).map {
      iterVals => (iters zip iterVals).toMap
    }
  }.toSeq

  @stateful def splitIntoTransientsAndControl(stms: Seq[Sym[_]]): Seq[TransientsAndControl] = {
    var curTransients: Seq[Sym[_]] = Seq.empty
    var curSplits: Seq[TransientsAndControl] = Seq.empty
    stms.foreach {
      case ctrl if ctrl.isControl =>
        curSplits :+= TransientsAndControl(curTransients, ctrl)
        curTransients = Seq.empty
      case other => curTransients :+= other
    }
    curSplits
  }

  @stateful def createInnerPipeline(stms: Seq[Sym[_]]): ExecPipeline = {
    val grouped = stms.groupBy(_.fullDelay.toInt)
    val numCycles = grouped.keys.max
    val stages = (0 to numCycles) map {
      stage =>
        val syms = grouped.getOrElse(stage, Seq.empty)
        new InnerPipelineStage(syms)
    }
    new ExecPipeline(stages.toSeq)
  }

  @stateful def createOuterPipeline(stms: Seq[Sym[_]]): ExecPipeline = {
    val transientsAndControl = splitIntoTransientsAndControl(stms)
    val stages = transientsAndControl.map {
      case TransientsAndControl(transients, control) => new OuterPipelineStage(transients, control)
    }
    new ExecPipeline(stages)
  }
}

object ControlExecutor {
  @stateful def apply(ctrl: Sym[_], execState: ExecutionState): OpExecutorBase = {
    val orderedSchedules = Set[CtrlSchedule](Pipelined, Sequenced)

    ctrl match {
      case Op(_:AccelScope) => new AccelScopeExecutor(ctrl, execState)
      case Op(_:OpForeach) if ctrl.isInnerControl => new InnerForeachExecutor(ctrl, execState)
      case Op(_:OpForeach) if ctrl.isOuterControl && orderedSchedules.contains(ctrl.schedule) => new OuterForeachExecutor(ctrl, execState)
      case Op(_:UnitPipe) if orderedSchedules.contains(ctrl.schedule) => new UnitPipeExecutor(ctrl, execState)
      case Op(_:UnitPipe) => new StreamUnitPipeExecutor(ctrl, execState)
      case _ => throw new NotImplementedError(s"Didn't know how to handle ${stm(ctrl)}")
    }
  }
}

class AccelScopeExecutor(ctrl: Sym[_], override val execState: ExecutionState)(implicit state: argon.State) extends OpExecutorBase {
  private val Op(AccelScope(blk)) = ctrl
  private val exec = if (ctrl.isInnerControl) {
    ControlExecutorUtils.createInnerPipeline(blk.stms)
  } else {
    ControlExecutorUtils.createOuterPipeline(blk.stms)
  }
  exec.pushState(Seq(execState))
  override def tick(): Unit = {
    exec.tick()
  }
  override def status: Status = if (exec.isEmpty) Done else Running
}

class UnitPipeExecutor(ctrl: Sym[_], override val execState: ExecutionState)(implicit state: argon.State) extends OpExecutorBase {
  private val Op(UnitPipe(ens, blk, stopWhen)) = ctrl
  private val shouldRun = ens.forall(execState.getValue[Bool](_).value)
  private val exec = if (ctrl.isInnerControl) {
    ControlExecutorUtils.createInnerPipeline(blk.stms)
  } else {
    ControlExecutorUtils.createOuterPipeline(blk.stms)
  }

  if (shouldRun) { exec.pushState(Seq(execState)) }

  override def tick(): Unit = if (shouldRun) exec.tick()

  override def status: Status = {
    if (!shouldRun) return Disabled
    if (exec.isEmpty) return Done
    Running
  }
}

class StreamUnitPipeExecutor(ctrl: Sym[_], override val execState: ExecutionState)(implicit state: argon.State) extends OpExecutorBase {
  private val Op(UnitPipe(ens, blk, stopWhen)) = ctrl
  private val shouldRun = ens.forall(execState.getValue[Bool](_).value)
  private lazy val executors = blk.stms.flatMap {
    case child if child.isControl =>
      Some(ControlExecutor(child, execState))
    case fringeNode@Op(_:FringeNode[_, _]) =>
      Some(FringeNodeExecutor(fringeNode, execState))
    case simple =>
      execState.runAndRegister(simple)
      None
  }

  override def tick(): Unit = if (shouldRun) {
    executors.foreach {
      exec =>
        emit(s"Ticking Exec: $exec")
        indentGen {
          exec.tick()
        }
    }
  }

  override def status: Status = {
    if (!shouldRun) return Disabled
    val statuses = executors.map(_.status)
    // if everything here is indeterminate or done, then we're done
    if (statuses.forall {
      case Done | Disabled | Indeterminate => true
      case Running => false
    }) {
      Done
    } else {
      Running
    }
  }
}


abstract class ForeachExecutorBase(ctrl: Sym[_], override val execState: ExecutionState)(implicit state: argon.State) extends OpExecutorBase {
  protected val Op(OpForeach(ens, cchain, blk, iters, stopWhen)) = ctrl
  protected val shouldRun = ens.forall(execState.getValue[Bool](_).value)
  protected val iterMap = ControlExecutorUtils.computeIters(cchain, execState).toIterator
  protected val steps = cchain.counters.map(_.step).map(execState.getValue[FixedPoint](_).toInt)
  protected val shifts = spatial.util.computeShifts(cchain.counters.map(_.ctrParOr1)).map {
    shift => shift.zip(steps).map {
      case (shift, step) => shift * step
    }
  }
  protected val itersWithShifts = shifts.map {
    shift => iters.map(_.asSym).zip(shift)
  }

  protected val bounds = (iters.map {
    iter =>
      val ctr = iter.counter.ctr
      iter -> (execState.getValue[FixedPoint](ctr.start), execState.getValue[FixedPoint](ctr.end))
  }).toMap

  protected def pipelines: Map[List[Int], ExecPipeline]

  override def status: Status = {
    if (!shouldRun) return Disabled
    if ((pipelines.forall {
      case (_, pipeline) => pipeline.isEmpty
    }) && iterMap.isEmpty) {
      return Done
    }
    Running
  }
}
class InnerForeachExecutor(ctrl: Sym[_], execState: ExecutionState)(implicit state: argon.State) extends ForeachExecutorBase(ctrl, execState) {
  emit(s"Setting up inner foreach pipeline for $ctrl")
  private val IIEnable = (Stream.from(0).map{
    x =>
      if (ctrl.II.toInt == 0) {
        true
      } else {
        (x % ctrl.II.toInt) == 0
      }
  }).iterator
  // There's only one pipeline for an inner foreach
  override lazy val pipelines = Map(List.empty[Int] -> ControlExecutorUtils.createInnerPipeline(blk.stms))
  lazy val pipeline = pipelines.head._2

  override def tick(): Unit = {
    if (!shouldRun) return
    emit(s"Ticking Pipeline for $ctrl")
    indentGen {
      pipeline.tick()
    }

    if (!IIEnable.next()) return

    if (iterMap.isEmpty) return

    val canEnqueue = pipeline.canAcceptNewState

    if (!canEnqueue) return

    val nextIter = iterMap.next()
    val states = itersWithShifts.flatMap {
      iterShifts =>
        val isEnabled = iterShifts.forall {
          case (iter, shift) =>
            val candidate = nextIter(iter) + shift
            val (lb, ub) = bounds(iter.unbox)
            ((lb <= candidate) && (candidate < ub)).value
        }
        if (isEnabled) {
          val newState = execState.copy()
          iterShifts.foreach {
            case (iter, shift) => newState.register(iter, SimpleEmulVal(nextIter(iter) + shift))
          }
          Some(newState)
        } else None
    }
    pipeline.pushState(states)
  }
}

class OuterForeachExecutor(ctrl: Sym[_], execState: ExecutionState)(implicit state: argon.State) extends ForeachExecutorBase(ctrl, execState) {
  override lazy val pipelines = shifts.map {
    shift => shift -> ControlExecutorUtils.createOuterPipeline(blk.stms)
  }.toMap

  emit(s"Setting up OuterForeachExecutor($ctrl):")
  indentGen {
    emit(s"Shifts: $shifts")
    emit(s"Pipelines: $pipelines")
  }

  override def tick(): Unit = {
    emit(s"Ticking Pipelines for $ctrl")
    indentGen {
      pipelines.foreach {
        case (shift, pipeline) =>
          emit(s"Ticking: $shift [$pipeline]")
          indentGen { pipeline.tick() }
      }
    }

    val schedEnabled = ctrl.schedule match {
      case Pipelined => pipelines.forall { case (_, pipeline) => pipeline.canAcceptNewState }
      case Sequenced => pipelines.forall { case (_, pipeline) => pipeline.isEmpty }
    }

    emit(s"SchedEnabled: $schedEnabled")

    if (!schedEnabled) return

    if (iterMap.isEmpty) return

    val nextIter = iterMap.next()
    emit(s"Staging next iter: $nextIter")
    itersWithShifts.foreach {
      iterShifts =>
        val isEnabled = iterShifts.forall {
          case (iter, shift) =>
            val candidate = nextIter(iter) + shift
            val (lb, ub) = bounds(iter.unbox)
            ((lb <= candidate) && (candidate < ub)).value
        }
        emit(s"IsEnabled: $isEnabled")
        if (isEnabled) {
          val newState = execState.copy()
          iterShifts.foreach {
            case (iter, shift) => newState.register(iter, SimpleEmulVal(nextIter(iter) + shift))
          }
          pipelines(iterShifts.map(_._2).toList).pushState(Seq(newState))
        }
    }
  }

  override def toString: String = {
    val pipelineString = pipelines.mapValues(_.toString)
    s"OuterForeachExecutor($ctrl) {$pipelineString}"
  }
}
