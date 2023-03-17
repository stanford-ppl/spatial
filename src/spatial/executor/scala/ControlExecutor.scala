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

object ControlExecutor {
  @stateful def apply(ctrl: Sym[_],
                      execState: ExecutionState): ControlExecutor = {
    val orderedSchedules = Set[CtrlSchedule](Pipelined, Sequenced)
    emit(s"Setting up pipelines for ${ctrl}: ${ctrl.ctx}")
    lazy val schedule = ctrl.schedule
    ctrl match {
      case Op(_: AccelScope) => new AccelScopeExecutor(ctrl, execState)
      case Op(_: OpForeach) if ctrl.isInnerControl =>
        new InnerForeachExecutor(ctrl, execState)
      case Op(_: OpForeach)
        if ctrl.isOuterControl && orderedSchedules.contains(schedule) =>
        new OuterForeachExecutor(ctrl, execState)
      case Op(_: UnitPipe) if orderedSchedules.contains(schedule) =>
        new UnitPipeExecutor(ctrl, execState)
      case Op(_: UnitPipe) => new StreamUnitPipeExecutor(ctrl, execState)
      case Op(_: ParallelPipe) => new StreamUnitPipeExecutor(ctrl, execState)
      case Op(_: OpReduce[_]) if ctrl.isInnerControl =>
        new InnerReduceExecutor(ctrl, execState)
      case Op(_: OpReduce[_]) if ctrl.isOuterControl =>
        new OuterReduceExecutor(ctrl, execState)
      case Op(_: OpMemReduce[_, _]) if ctrl.isInnerControl =>
        new InnerMemReduceExecutor(ctrl, execState)
      case Op(_: OpMemReduce[_, _]) if ctrl.isOuterControl =>
        new OuterMemReduceExecutor(ctrl, execState)
      case Op(_: Switch[_]) => new SwitchExecutor(ctrl, execState)
      case Op(_: FringeNode[_, _]) => FringeNodeExecutor(ctrl, execState)
      case _ =>
        throw new NotImplementedError(s"Didn't know how to handle ${stm(ctrl)}")
    }
  }
}

case class TransientsAndControl(transients: Vector[Sym[_]], control: Sym[_])

abstract class ControlExecutor extends OpExecutorBase {
  implicit def state: argon.State = execState.IR
  val ctrl: Sym[_]
  lazy val schedule = ctrl.schedule
  var lastIter: Option[Seq[FixedPoint]] = None
  override def print(): Unit = {
    emit(s"${ctrl} ${schedule}($status, ${lastIter.map(_.mkString(", ")).getOrElse("None")}) @ [${ctrl.ctx}]")
    indentGen {
      printInternals()
    }
  }
  def printInternals(): Unit = {}
  def shouldRun: Boolean = true

  var hasBeenRegistered = false

  def getCycleEntry: CycleTrackerEntry = {
    execState.cycleTracker.getEntry(ctrl)
  }

  override def tick(): Unit = {
    if (shouldRun) {
      if (!hasBeenRegistered) {
        getCycleEntry.iterations += 1
        hasBeenRegistered = true
      }
      getCycleEntry.cycles += 1
    }
  }

  def isDeadlocked: Boolean
}

private object ControlExecutorUtils {

  // Computes the iteration offsets
  // Iterations of ctrler
  // -- iter -> iter value
  @stateful def computeIters(
      cchain: CounterChain,
      execState: ExecutionState): Vector[Map[Sym[Idx], FixedPoint]] = {
    val ranges = cchain.counters.map { ctr =>
      val fStart = execState.getValue[FixedPoint](ctr.start)
      val fEnd = execState.getValue[FixedPoint](ctr.end)
      val fStep = execState.getValue[FixedPoint](ctr.step)
      val par = ctr.ctrParOr1

      FixedPointRange(fStart, fEnd, fStep * par, isInclusive = false)
    }

    val iters = cchain.counters.flatMap(_.iter).map(_.asInstanceOf[Sym[Idx]])
    spatial.util.crossJoin(ranges).map { iterVals =>
      (iters zip iterVals).toMap
    }.toVector
  }

  @stateful def splitIntoTransientsAndControl(
      stms: Seq[Sym[_]]): (Vector[TransientsAndControl], Vector[Sym[_]]) = {
    var curTransients: Vector[Sym[_]] = Vector.empty
    var curSplits: Vector[TransientsAndControl] = Vector.empty
    stms.foreach {
      case ctrl if ctrl.isControl =>
        curSplits :+= TransientsAndControl(curTransients, ctrl)
        curTransients = Vector.empty
      case fringeOp@Op(_:FringeNode[_, _]) =>
        curSplits :+= TransientsAndControl(curTransients, fringeOp)
        curTransients = Vector.empty
      case other => curTransients :+= other
    }
    (curSplits, curTransients)
  }

  @stateful def createInnerPipeline(stms: Seq[Sym[_]]): ExecPipeline = {
    val grouped = stms.groupBy(_.fullDelay.toInt)
    val numCycles = grouped.keys.max
    val stages = (0 to numCycles).toVector map { stage =>
      val syms = grouped.getOrElse(stage, Vector.empty)
      new InnerPipelineStage(syms.toVector)
    }
    new ExecPipeline(stages)
  }

  @stateful def createOuterPipeline(stms: Seq[Sym[_]]): ExecPipeline = {
    val (transientsAndControl, leftovers) = splitIntoTransientsAndControl(stms)
    var stages: Vector[PipelineStage] = transientsAndControl.map {
      case TransientsAndControl(transients, control) =>
        new OuterPipelineStage(transients, control)
    }
    if (leftovers.nonEmpty) {
      stages :+= new InnerPipelineStage(leftovers)
    }
    new ExecPipeline(stages)
  }
}

class AccelScopeExecutor(val ctrl: Sym[_],
                         override val execState: ExecutionState)(
    implicit override val state: argon.State) extends ControlExecutor {
  private val Op(AccelScope(blk)) = ctrl
  private val exec = if (ctrl.isInnerControl) {
    ControlExecutorUtils.createInnerPipeline(blk.stms)
  } else {
    ControlExecutorUtils.createOuterPipeline(blk.stms)
  }
  exec.pushState(Vector(execState))
  override def tick(): Unit = {
    super.tick()
    exec.tick()
  }
  override def status: Status = if (exec.isEmpty) Done else Running

  override def printInternals(): Unit = {
    exec.print()
  }

  override def isDeadlocked: Boolean = {
    exec.canProgress
  }
}

class UnitPipeExecutor(val ctrl: Sym[_], override val execState: ExecutionState)(
    implicit override val state: argon.State)
    extends  ControlExecutor {
  private val Op(UnitPipe(ens, blk, stopWhen)) = ctrl
  override val shouldRun = ens.forall(execState.getValue[Bool](_).value)
  private val exec = if (ctrl.isInnerControl) {
    ControlExecutorUtils.createInnerPipeline(blk.stms)
  } else {
    ControlExecutorUtils.createOuterPipeline(blk.stms)
  }

  if (shouldRun) {
    exec.pushState(Vector(execState))
  }

  override def tick(): Unit = if (shouldRun) {
    super.tick()
    exec.tick()
  }

  override def status: Status = {
    if (!shouldRun) return Disabled
    if (exec.isEmpty) return Done
    Running
  }

  override def printInternals(): Unit = {
    exec.print()
  }

  override def isDeadlocked: Boolean = {
    !exec.canProgress
  }
}

class StreamUnitPipeExecutor(val ctrl: Sym[_],
                             override val execState: ExecutionState)(
    implicit override val state: argon.State)
    extends  ControlExecutor {

  val (ens, blk, stopWhen) = ctrl match {
    case Op(UnitPipe(ens, blk, stopWhen)) => (ens, blk, stopWhen)
    case Op(ParallelPipe(ens, blk)) => (ens, blk, None)
  }

  override val shouldRun = ens.forall(execState.getValue[Bool](_).value)
  private lazy val executors = blk.stms.flatMap {
    case child if child.isControl =>
      Some(ControlExecutor(child, execState))
    case fringeNode @ Op(_: FringeNode[_, _]) =>
      Some(FringeNodeExecutor(fringeNode, execState))
    case simple =>
      execState.runAndRegister(simple)
      None
  }

  override def tick(): Unit = if (shouldRun) {
    super.tick()
    executors.foreach { exec =>
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
          case Running                         => false
        }) {
      Done
    } else {
      Running
    }
  }

  override def printInternals(): Unit = {
    executors.foreach(_.print())
  }

  override def isDeadlocked: Boolean = {
    (executors.collect {
      case ctrl: ControlExecutor => ctrl.isDeadlocked
    }).forall(x => x)
  }
}

abstract class ForeachExecutorBase(ctrl: Sym[_],
                                   override val execState: ExecutionState)(
    implicit override val state: argon.State)
    extends  ControlExecutor {
  protected val Op(OpForeach(ens, cchain, blk, iters, stopWhen)) = ctrl
  override val shouldRun = ens.forall(execState.getValue[Bool](_).value)
  protected val iterMap =
    ControlExecutorUtils.computeIters(cchain, execState).toIterator
  protected val steps =
    cchain.counters.map(_.step).map(execState.getValue[FixedPoint](_).toInt)
  protected val shifts =
    spatial.util.computeShifts(cchain.counters.map(_.ctrParOr1)).map { shift =>
      shift.zip(steps).map {
        case (shift, step) => shift * step
      }
    }
  protected val itersWithShifts = shifts.map { shift =>
    iters.map(_.asSym).zip(shift)
  }

  protected val bounds = (iters.map { iter =>
    val ctr = iter.counter.ctr
    iter -> (execState.getValue[FixedPoint](ctr.start), execState
      .getValue[FixedPoint](ctr.end))
  }).toMap

  protected def pipelines: Map[Seq[Int], ExecPipeline]

  override def status: Status = {
    if (!shouldRun) return Disabled
    if ((pipelines.forall {
          case (_, pipeline) => pipeline.isEmpty
        }) && iterMap.isEmpty) {
      return Done
    }
    Running
  }

  override def isDeadlocked: Boolean = {
    !pipelines.values.exists(_.canProgress)
  }
}
class InnerForeachExecutor(val ctrl: Sym[_], execState: ExecutionState)(
    implicit override val state: argon.State)
    extends ForeachExecutorBase(ctrl, execState) {
  private val IIEnable = (Stream
    .from(0)
    .map { x =>
      if (ctrl.II.toInt == 0) {
        true
      } else {
        (x % ctrl.II.toInt) == 0
      }
    })
    .iterator
  // There's only one pipeline for an inner foreach
  override lazy val pipelines = Map(
    List.empty[Int] -> ControlExecutorUtils.createInnerPipeline(blk.stms))
  lazy val pipeline = pipelines.head._2

  override def tick(): Unit = {
    if (!shouldRun) return

    super.tick()
    indentGen {
      pipeline.tick()
    }

    if (!IIEnable.next()) return

    if (iterMap.isEmpty) return

    val canEnqueue = pipeline.canAcceptNewState

    if (!canEnqueue) return

    val nextIter = iterMap.next()
    lastIter = Some(nextIter.values.toVector)
    val states = itersWithShifts.flatMap { iterShifts =>
      val isEnabled = iterShifts.forall {
        case (iter, shift) =>
          val candidate = nextIter(iter) + shift
          val (lb, ub) = bounds(iter.unbox)
          ((lb <= candidate) && (candidate < ub)).value
      }
      if (isEnabled) {
        val newState = execState.copy()
        iterShifts.foreach {
          case (iter, shift) =>
            newState.register(iter, SimpleEmulVal(nextIter(iter) + shift))
        }
        Some(newState)
      } else None
    }
    pipeline.pushState(states)
  }

  override def printInternals(): Unit = {
    pipeline.print()
  }
}

class OuterForeachExecutor(val ctrl: Sym[_], execState: ExecutionState)(
    implicit override val state: argon.State)
    extends ForeachExecutorBase(ctrl, execState) {
  override lazy val pipelines = shifts.map { shift =>
    shift -> ControlExecutorUtils.createOuterPipeline(blk.stms)
  }.toMap

  override def tick(): Unit = {
    if (!shouldRun) {return}
    super.tick()
    indentGen {
      pipelines.foreach {
        case (shift, pipeline) =>
          pipeline.tick()
      }
    }

    val schedEnabled = schedule match {
      case Pipelined =>
        pipelines.forall { case (_, pipeline) => pipeline.canAcceptNewState }
      case Sequenced =>
        pipelines.forall { case (_, pipeline) => pipeline.isEmpty }
    }

    if (!schedEnabled) return

    if (iterMap.isEmpty) return

    val nextIter = iterMap.next()
    lastIter = Some(nextIter.values.toVector)
    itersWithShifts.foreach { iterShifts =>
      val isEnabled = iterShifts.forall {
        case (iter, shift) =>
          val candidate = nextIter(iter) + shift
          val (lb, ub) = bounds(iter.unbox)
          ((lb <= candidate) && (candidate < ub)).value
      }
      if (isEnabled) {
        val newState = execState.copy()
        iterShifts.foreach {
          case (iter, shift) =>
            newState.register(iter, SimpleEmulVal(nextIter(iter) + shift))
        }
        pipelines(iterShifts.map(_._2)).pushState(Vector(newState))
      }
    }
  }

  override def printInternals(): Unit = {
    pipelines.foreach {
      case (shift, pipeline) =>
        emit(s"Shift: ${shift.mkString(", ")}")
        indentGen {
          pipeline.print()
        }
    }
  }
}

//@op case class OpReduce[A](
//  ens:    Set[Bit],
//  cchain: CounterChain,
//  accum:  Reg[A],
//  map:    Block[A],
//  load:   Lambda1[Reg[A],A],
//  reduce: Lambda2[A,A,A],
//  store:  Lambda2[Reg[A],A,Void],
//  ident:  Option[A],
//  fold:   Option[A],
//  iters:  List[I32],
//  stopWhen:  Option[Reg[Bit]]

abstract class ReduceExecutorBase(ctrl: Sym[_],
                                  override val execState: ExecutionState)(
    implicit override val state: argon.State)
    extends  ControlExecutor {
  protected val Op(
    OpReduce(ens,
             cchain,
             accum,
             mapF,
             loadF,
             reduceF,
             storeF,
             identOpt,
             foldOpt,
             iters,
             stopWhen)) = ctrl
  override val shouldRun = ens.forall(execState.getValue[Bool](_).value)
  protected val iterMap =
    ControlExecutorUtils.computeIters(cchain, execState).toIterator
  protected val steps =
    cchain.counters.map(_.step).map(execState.getValue[FixedPoint](_).toInt)
  protected val shifts =
    spatial.util.computeShifts(cchain.counters.map(_.ctrParOr1)).map { shift =>
      shift.zip(steps).map {
        case (shift, step) => shift * step
      }
    }
  protected val itersWithShifts = shifts.map { shift =>
    iters.map(_.asSym).zip(shift)
  }

  protected val bounds = (iters.map { iter =>
    val ctr = iter.counter.ctr
    iter -> (execState.getValue[FixedPoint](ctr.start), execState
      .getValue[FixedPoint](ctr.end))
  }).toMap

  protected val accumReg = execState(accum) match {
    case sr: ScalaReg[SomeEmul] => sr
  }

  protected def pipelines: Map[Seq[Int], ExecPipeline]

  protected def updateAccum(): Unit = {
    val lastStates = pipelines.values.flatMap(_.lastStates)
    val results = lastStates.map { eState =>
      eState(mapF.result)
    }
    if (results.nonEmpty) {
      val output = results.reduce { (a, b) =>
        OpResolver.runBlock(reduceF,
                            Map(reduceF.inputA -> a, reduceF.inputB -> b),
                            execState)
      }
      val updated = OpResolver.runBlock(
        reduceF,
        Map(reduceF.inputA -> output, reduceF.inputB -> accumReg.curVal),
        execState)
      accumReg.write(updated, true)
    }
  }

  override def status: Status = {
    if (!shouldRun) return Disabled
    if ((pipelines.forall {
          case (_, pipeline) => pipeline.isEmpty
        }) && iterMap.isEmpty) {
      return Done
    }
    Running
  }

  override def isDeadlocked: Boolean = {
    !pipelines.values.exists(_.canProgress)
  }
}

class InnerReduceExecutor(val ctrl: Sym[_], execState: ExecutionState)(
    implicit override val state: argon.State)
    extends ReduceExecutorBase(ctrl, execState) {
  private val IIEnable = (Stream
    .from(0)
    .map { x =>
      if (ctrl.II.toInt == 0) {
        true
      } else {
        (x % ctrl.II.toInt) == 0
      }
    })
    .iterator
  // There's only one pipeline for an inner foreach
  override lazy val pipelines = Map(
    Seq.empty[Int] -> ControlExecutorUtils.createInnerPipeline(mapF.stms))
  lazy val pipeline = pipelines.head._2

  override def tick(): Unit = {
    if (!shouldRun) return
    super.tick()

    indentGen {
      pipeline.tick()
    }

    updateAccum()

    if (!IIEnable.next()) return

    if (iterMap.isEmpty) return

    val canEnqueue = pipeline.canAcceptNewState

    if (!canEnqueue) return

    val nextIter = iterMap.next()
    val states = itersWithShifts.flatMap { iterShifts =>
      val isEnabled = iterShifts.forall {
        case (iter, shift) =>
          val candidate = nextIter(iter) + shift
          val (lb, ub) = bounds(iter.unbox)
          ((lb <= candidate) && (candidate < ub)).value
      }
      if (isEnabled) {
        val newState = execState.copy()
        iterShifts.foreach {
          case (iter, shift) =>
            newState.register(iter, SimpleEmulVal(nextIter(iter) + shift))
        }
        Some(newState)
      } else None
    }
    pipeline.pushState(states)
  }


  override def printInternals(): Unit = {
    pipeline.print()
  }
}

class OuterReduceExecutor(val ctrl: Sym[_], execState: ExecutionState)(
    implicit override val state: argon.State)
    extends ReduceExecutorBase(ctrl, execState) {
  override lazy val pipelines = shifts.map { shift =>
    shift -> ControlExecutorUtils.createOuterPipeline(mapF.stms)
  }.toMap

  override def tick(): Unit = {
    if (!shouldRun) return
    super.tick()
    indentGen {
      pipelines.foreach {
        case (shift, pipeline) =>
          pipeline.tick()
      }
    }

    updateAccum()

    val schedEnabled = schedule match {
      case Pipelined =>
        pipelines.forall { case (_, pipeline) => pipeline.canAcceptNewState }
      case Sequenced =>
        pipelines.forall { case (_, pipeline) => pipeline.isEmpty }
    }

    if (!schedEnabled) return

    if (iterMap.isEmpty) return

    val nextIter = iterMap.next()
    itersWithShifts.foreach { iterShifts =>
      val isEnabled = iterShifts.forall {
        case (iter, shift) =>
          val candidate = nextIter(iter) + shift
          val (lb, ub) = bounds(iter.unbox)
          ((lb <= candidate) && (candidate < ub)).value
      }
      if (isEnabled) {
        val newState = execState.copy()
        iterShifts.foreach {
          case (iter, shift) =>
            newState.register(iter, SimpleEmulVal(nextIter(iter) + shift))
        }
        pipelines(iterShifts.map(_._2).toList).pushState(Vector(newState))
      }
    }
  }

  override def printInternals(): Unit = {
    pipelines.foreach {
      case (shift, pipeline) =>
        emit(s"Shift: ${shift.mkString(", ")}")
        indentGen {
          pipeline.print()
        }
    }
  }
}

//ens:       Set[Bit],
//  cchainMap: CounterChain,
//  cchainRed: CounterChain,
//  accum:     C[A],
//  map:       Block[C[A]],
//  loadRes:   Lambda1[C[A],A],
//  loadAcc:   Lambda1[C[A],A],
//  reduce:    Lambda2[A,A,A],
//  storeAcc:  Lambda2[C[A],A,Void],
//  ident:     Option[A],
//  fold:      Boolean,
//  itersMap:  Seq[I32],
//  itersRed:  Seq[I32],
//  stopWhen:  Option[Reg[Bit]]

abstract class MemReduceExecutorBase(ctrl: Sym[_],
                                     override val execState: ExecutionState)(
    implicit override val state: argon.State)
    extends  ControlExecutor {
  protected val Op(
    OpMemReduce(ens,
                cchain,
                cchainRed,
                accum,
                mapF,
                loadRes,
                loadAcc,
                reduceF,
                storeAcc,
                identOpt,
                foldOpt,
                iters,
                itersRed,
                stopWhen)) = ctrl
  override val shouldRun = ens.forall(execState.getValue[Bool](_).value)
  protected val iterMap =
    ControlExecutorUtils.computeIters(cchain, execState).toIterator
  protected val steps =
    cchain.counters.map(_.step).map(execState.getValue[FixedPoint](_).toInt)
  protected val shifts =
    spatial.util.computeShifts(cchain.counters.map(_.ctrParOr1)).map { shift =>
      shift.zip(steps).map {
        case (shift, step) => shift * step
      }
    }
  protected val itersWithShifts = shifts.map { shift =>
    iters.map(_.asSym).zip(shift)
  }

  protected val bounds = (iters.map { iter =>
    val ctr = iter.counter.ctr
    iter -> (execState.getValue[FixedPoint](ctr.start), execState
      .getValue[FixedPoint](ctr.end))
  }).toMap

  protected val accumMem =
    execState.getTensor[SomeEmul](accum.asInstanceOf[Sym[_]])

  protected def pipelines: Map[Seq[Int], ExecPipeline]

  protected def updateAccum(): Unit = {
    val lastStates = pipelines.values.flatMap(_.lastStates)
    val results = lastStates.map { eState =>
      eState.getTensor[SomeEmul](mapF.result)
    }
    // Results will be a list of tensors in this case.
    results.foreach { resultTensor =>
      val ranges = cchainRed.counters.map {
        case Op(CounterNew(start, end, step, _)) =>
          FixedPointRange(
            execState.getValue[FixedPoint](start),
            execState.getValue[FixedPoint](end),
            execState.getValue[FixedPoint](step),
            isInclusive = false
          ).toList
      }
      val iterations = spatial.util.crossJoin(ranges.toList)
      iterations.foreach { iterVals =>
        val addr = iterVals.map(_.toInt).toSeq
        val aVal = resultTensor.read(addr, true).get
        val result = accumMem.read(addr, true) match {
          case Some(bVal) =>
            OpResolver.runBlock(reduceF,
                                Map(reduceF.inputA -> aVal,
                                    reduceF.inputB -> bVal),
                                execState)
          case None => aVal
        }
        accumMem.write(result, addr, true)
      }
    }
  }

  override def status: Status = {
    if (!shouldRun) return Disabled
    if ((pipelines.forall {
          case (_, pipeline) => pipeline.isEmpty
        }) && iterMap.isEmpty) {
      return Done
    }
    Running
  }

  override def isDeadlocked: Boolean = {
    !pipelines.values.exists(_.canProgress)
  }
}

class InnerMemReduceExecutor(val ctrl: Sym[_], execState: ExecutionState)(
    implicit override val state: argon.State)
    extends MemReduceExecutorBase(ctrl, execState) {
  private val IIEnable = (Stream
    .from(0)
    .map { x =>
      if (ctrl.II.toInt == 0) {
        true
      } else {
        (x % ctrl.II.toInt) == 0
      }
    })
    .iterator
  // There's only one pipeline for an inner foreach
  override lazy val pipelines = Map(
    List.empty[Int] -> ControlExecutorUtils.createInnerPipeline(mapF.stms))
  lazy val pipeline = pipelines.head._2

  override def tick(): Unit = {
    if (!shouldRun) return
    super.tick()
    indentGen {
      pipeline.tick()
    }

    updateAccum()

    if (!IIEnable.next()) return

    if (iterMap.isEmpty) return

    val canEnqueue = pipeline.canAcceptNewState

    if (!canEnqueue) return

    val nextIter = iterMap.next()
    val states = itersWithShifts.flatMap { iterShifts =>
      val isEnabled = iterShifts.forall {
        case (iter, shift) =>
          val candidate = nextIter(iter) + shift
          val (lb, ub) = bounds(iter.unbox)
          ((lb <= candidate) && (candidate < ub)).value
      }
      if (isEnabled) {
        val newState = execState.copy()
        iterShifts.foreach {
          case (iter, shift) =>
            newState.register(iter, SimpleEmulVal(nextIter(iter) + shift))
        }
        Some(newState)
      } else None
    }
    pipeline.pushState(states)
  }

  override def printInternals(): Unit = {
    pipelines.foreach {
      case (shift, pipeline) =>
        emit(s"Shift: ${shift.mkString(", ")}")
        indentGen {
          pipeline.print()
        }
    }
  }
}

class OuterMemReduceExecutor(val ctrl: Sym[_], execState: ExecutionState)(
    implicit override val state: argon.State)
    extends MemReduceExecutorBase(ctrl, execState) {
  override lazy val pipelines = shifts.map { shift =>
    shift -> ControlExecutorUtils.createOuterPipeline(mapF.stms)
  }.toMap

  override def tick(): Unit = {
    if (!shouldRun) return
    super.tick()
    indentGen {
      pipelines.foreach {
        case (shift, pipeline) =>
          pipeline.tick()
      }
    }

    updateAccum()

    val schedEnabled = schedule match {
      case Pipelined =>
        pipelines.forall { case (_, pipeline) => pipeline.canAcceptNewState }
      case Sequenced =>
        pipelines.forall { case (_, pipeline) => pipeline.isEmpty }
    }

    if (!schedEnabled) return

    if (iterMap.isEmpty) return

    val nextIter = iterMap.next()
    itersWithShifts.foreach { iterShifts =>
      val isEnabled = iterShifts.forall {
        case (iter, shift) =>
          val candidate = nextIter(iter) + shift
          val (lb, ub) = bounds(iter.unbox)
          ((lb <= candidate) && (candidate < ub)).value
      }
      if (isEnabled) {
        val newState = execState.copy()
        iterShifts.foreach {
          case (iter, shift) =>
            newState.register(iter, SimpleEmulVal(nextIter(iter) + shift))
        }
        pipelines(iterShifts.map(_._2)).pushState(Vector(newState))
      }
    }
  }

  override def printInternals(): Unit = {
    pipelines.foreach {
      case (shift, pipeline) =>
        emit(s"Shift: ${shift.mkString(", ")}")
        indentGen {
          pipeline.print()
        }
    }
  }
}

class SwitchCaseExecutor(casee: SwitchCase[_], execState: ExecutionState)(
    implicit state: argon.State) {
  private val SwitchCase(blk) = casee

  val isOuter = blk.stms.exists(_.isControl)

  private val exec = if (blk.stms.nonEmpty) {
    Some(if (!isOuter) {
      ControlExecutorUtils.createInnerPipeline(blk.stms)
    } else {
      ControlExecutorUtils.createOuterPipeline(blk.stms)
    })
  } else None

  exec.foreach(_.pushState(Vector(execState)))

  def tick(): Unit = exec.foreach(_.tick())

  def status: Status = {
    if (exec.isEmpty) return Done
    if (exec.get.isEmpty) return Done
    Running
  }

  def printInternals(): Unit = {
    exec.foreach(_.print())
  }

  def canProgress: Boolean = exec.exists(_.canProgress)
}

class SwitchExecutor(val ctrl: Sym[_], override val execState: ExecutionState)(
    implicit override val state: argon.State)
    extends  ControlExecutor {
  private val Op(switches @ Switch(selects, _)) = ctrl

  val enabledCases = (selects zip switches.cases) find {
    case (sel, _) => execState.getValue[emul.Bool](sel).value
  }

  val exec = enabledCases.map {
    case (_, casee) => new SwitchCaseExecutor(casee, execState)
  }

  override def tick(): Unit = {
    super.tick()
    exec.foreach(_.tick())
  }

  override def status: Status = {
    if (exec.isEmpty) return Disabled
    exec.get.status
  }

  override def printInternals(): Unit = {
    enabledCases match {
      case None =>
        emit("Disabled")
      case Some((_, _)) =>
        exec.foreach(_.printInternals())
    }
  }

  override def isDeadlocked: Boolean = {
    !exec.exists(_.canProgress)
  }
}
