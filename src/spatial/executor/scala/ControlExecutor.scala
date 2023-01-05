package spatial.executor.scala
import argon.lang.{Bit, Idx}
import argon._
import emul.{FixedPoint, FixedPointRange}
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
  exec.pushState(execState)
  override def tick(): Unit = {
    assert(!isDone, s"Shouldn't be running, already finished!")
    exec.tick()
  }
  override def isDone: Boolean = exec.isEmpty
}

class UnitPipeExecutor(ctrl: Sym[_], override val execState: ExecutionState)(implicit state: argon.State) extends OpExecutorBase {
  private val Op(UnitPipe(ens, blk, stopWhen)) = ctrl
  private val shouldRun = ens.forall(execState.getValue[Boolean](_))
  private val exec = if (ctrl.isInnerControl) {
    ControlExecutorUtils.createInnerPipeline(blk.stms)
  } else {
    ControlExecutorUtils.createOuterPipeline(blk.stms)
  }

  if (shouldRun) { exec.pushState(execState) }

  override def tick(): Unit = if (shouldRun) exec.tick()

  override def isDone: Boolean = exec.isEmpty
}

abstract class ForeachExecutorBase(ctrl: Sym[_], override val execState: ExecutionState)(implicit state: argon.State) extends OpExecutorBase {
  protected val Op(OpForeach(ens, cchain, blk, iters, stopWhen)) = ctrl
  protected val shouldRun: Boolean = ens.forall(execState.getValue[Boolean](_))
  protected val iterMap = ControlExecutorUtils.computeIters(cchain, execState).toIterator
  protected val shifts = spatial.util.computeShifts(cchain.counters.map(_.ctrParOr1))
  protected val itersWithShifts = shifts.map {
    shift => iters.map(_.asSym).zip(shift)
  }

  protected val bounds = (iters.map {
    iter =>
      val ctr = iter.counter.ctr
      dbgs(s"Getting iter: $iter (${stm(ctr)}")
      iter -> (execState.getValue[FixedPoint](ctr.start), execState.getValue[FixedPoint](ctr.end))
  }).toMap
}
class InnerForeachExecutor(ctrl: Sym[_], execState: ExecutionState)(implicit state: argon.State) extends ForeachExecutorBase(ctrl, execState) {
  private val IIEnable = (Stream.from(0).map{ x => (x % ctrl.II.toInt) == 0 }).iterator

  private val pipelines = shifts.map {
    shift => shift -> ControlExecutorUtils.createInnerPipeline(blk.stms)
  }.toMap

  override def isDone: Boolean = pipelines.forall({
    case (_, pipeline) => pipeline.isEmpty
  }) && iterMap.isEmpty

  override def tick(): Unit = {
    emit(s"Ticking Pipelines for $ctrl")
    indentGen {
      pipelines.foreach {
        case (shift, pipeline) =>
          emit(s"Ticking: $shift [$pipeline]")
          indentGen {
            pipeline.tick()
          }
      }
    }

    if (!IIEnable.next()) return

    if (iterMap.isEmpty) return

    val nextIter = iterMap.next()
    itersWithShifts.foreach {
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
          val pipelineKey = iterShifts.map(_._2).toList
          emit(s"Pushing into pipeline: $pipelineKey (${pipelines(pipelineKey)})")
          pipelines(pipelineKey).pushState(newState)
        }
    }
  }
}

class OuterForeachExecutor(ctrl: Sym[_], execState: ExecutionState)(implicit state: argon.State) extends ForeachExecutorBase(ctrl, execState) {
  private val pipelines = shifts.map {
    shift => shift -> ControlExecutorUtils.createOuterPipeline(blk.stms)
  }.toMap

  override def isDone: Boolean = pipelines.forall({
    case (_, pipeline) => pipeline.isEmpty
  }) && iterMap.isEmpty

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
          pipelines(iterShifts.map(_._2).toList).pushState(newState)
        }
    }
  }

  override def toString: String = {
    val pipelineString = pipelines.mapValues(_.toString)
    s"OuterForeachExecutor($ctrl) {$pipelineString}"
  }
}
