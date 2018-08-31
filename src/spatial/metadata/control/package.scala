package spatial.metadata

import argon._
import argon.node._
import forge.tags.stateful
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.bounds._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.util.spatialConfig
import spatial.issues.AmbiguousMetaPipes

import scala.util.Try

import utils.Tree
import utils.implicits.collections._

package object control {

  implicit class ControlOpOps(op: Op[_]) {
    /** True if this Op is a loop which has a body which is run multiple times at runtime. */
    def isLoop: Boolean = op match {
      case loop: Loop[_] => !loop.cchains.forall(_._1.willFullyUnroll)
      case _ => false
    }
    def isFullyUnrolledLoop: Boolean = op match {
      case loop: Loop[_] => loop.cchains.forall(_._1.willFullyUnroll)
      case _ => false
    }

    def isControl: Boolean = op.isInstanceOf[Control[_]] || op.isInstanceOf[IfThenElse[_]]
    def isPrimitive: Boolean = op.isInstanceOf[Primitive[_]]
    def isTransient: Boolean = op match {
      case p: Primitive[_] => p.isTransient
      case _ => false
    }

    def isAccel: Boolean = op.isInstanceOf[AccelScope]

    def isSwitch: Boolean = op.isInstanceOf[Switch[_]]
    def isBranch: Boolean = op match {
      case _:Switch[_] | _:SwitchCase[_] | _:IfThenElse[_] => true
      case _ => false
    }

    def isParallel: Boolean = op.isInstanceOf[ParallelPipe]

    def isUnitPipe: Boolean = op.isInstanceOf[UnitPipe]

    def isMemReduce: Boolean = op match {
      case _:OpMemReduce[_,_] => true
      case _ => false
    }

    def isFSM: Boolean = op.isInstanceOf[StateMachine[_]]

    def isStreamLoad: Boolean = op match {
      case _:FringeDenseLoad[_,_] => true
      case _ => false
    }

    def isTileTransfer: Boolean = op match {
      case _:FringeDenseLoad[_,_]   => true
      case _:FringeDenseStore[_,_]  => true
      case _:FringeSparseLoad[_,_]  => true
      case _:FringeSparseStore[_,_] => true
      case _ => false
    }

    def isTileLoad: Boolean = op match {
      case _:FringeDenseLoad[_,_]  => true
      case _:FringeSparseLoad[_,_] => true
      case _ => false
    }

    def isTileStore: Boolean = op match {
      case _:FringeDenseStore[_,_]  => true
      case _:FringeSparseStore[_,_] => true
      case _ => false
    }
  }

  abstract class CtrlHierarchyOps(s: Option[Sym[_]]) {
    private def op: Option[Op[_]] = s.flatMap{sym => sym.op : Option[Op[_]] }

    /** Returns the nearest controller at or above the current symbol or controller.
      * Identity for Ctrl instances.
      */
    def toCtrl: Ctrl

    /** True if this is a controller or the Host control. */
    def isControl: Boolean
    def isPrimitive: Boolean = op.exists(_.isPrimitive)
    def isTransient: Boolean = op.exists(_.isTransient)

    def isAccel: Boolean = op.exists(_.isAccel)
    def isSwitch: Boolean = op.exists(_.isSwitch)
    def isBranch: Boolean = op.exists(_.isBranch)
    def isParallel: Boolean = op.exists(_.isParallel)
    def isUnitPipe: Boolean = op.exists(_.isUnitPipe)
    def isFSM: Boolean = op.exists(_.isFSM)

    def isMemReduce: Boolean = op.exists(_.isMemReduce)

    def isStreamLoad: Boolean = op.exists(_.isStreamLoad)
    def isTileTransfer: Boolean = op.exists(_.isTileTransfer)
    def isTileLoad: Boolean = op.exists(_.isTileLoad)
    def isTileStore: Boolean = op.exists(_.isTileStore)

    def isCounter: Boolean = s.exists(_.isInstanceOf[Counter[_]])
    def isCounterChain: Boolean = s.exists(_.isInstanceOf[CounterChain])

    /** Returns the level of this controller (Outer or Inner). */
    def level: CtrlLevel = toCtrl match {
      case ctrl @ Ctrl.Node(sym,_) if sym.isRawOuter && ctrl.mayBeOuterBlock => Outer
      case Ctrl.Host => Outer
      case _         => Inner
    }

    def innerBlocks: Seq[(Seq[I32],Block[_])] = op match {
      case Some(ctrl:Control[_]) => ctrl.bodies.zipWithIndex.flatMap{case (body, id) =>
        val stage = s.map{sym => Ctrl.Node(sym, id) }.getOrElse(Ctrl.Host)
        if (!stage.mayBeOuterBlock || this.isInnerControl) body.blocks else Nil
      }
      case _ => Nil
    }
    def outerBlocks: Seq[(Seq[I32],Block[_])] = op match {
      case Some(ctrl:Control[_]) => ctrl.bodies.zipWithIndex.flatMap{case (body, id) =>
        val stage = s.map{sym => Ctrl.Node(sym, id) }.getOrElse(Ctrl.Host)
        if (stage.mayBeOuterBlock && this.isOuterControl) body.blocks else Nil
      }
      case _ => Nil
    }

    def innerAndOuterBlocks: (Seq[(Seq[I32],Block[_])], Seq[(Seq[I32],Block[_])]) = {
      (innerBlocks, outerBlocks)
    }

    /** Returns whether this control node is a Looped control or Single iteration control.
      * Nodes which will be fully unrolled are considered Single control.
      */
    def looping: CtrlLooping = if (op.exists(_.isLoop)) Looped else Single

    def schedule: CtrlSchedule = {
      val ctrlLoop  = this.looping
      val ctrlLevel = this.level
      s.map(_.rawSchedule) match {
        case None => Sequenced
        case Some(actualSchedule) => (ctrlLoop, ctrlLevel) match {
          case (Looped, Inner) => actualSchedule
          case (Looped, Outer) => actualSchedule
          case (Single, Inner) => actualSchedule match {
            case Sequenced => Sequenced
            case Pipelined => Sequenced
            case Streaming => Streaming
            case ForkJoin  =>  ForkJoin
            case Fork => Fork
          }
          case (Single, Outer) => actualSchedule match {
            case Sequenced => Sequenced
            case Pipelined => Sequenced
            case Streaming => Streaming
            case ForkJoin  => ForkJoin
            case Fork => Fork
          }
        }
      }
    }

    def isCtrl(
      loop:  CtrlLooping = null,
      level: CtrlLevel = null,
      sched: CtrlSchedule = null
    ): Boolean = {
      val isCtrl  = this.isControl
      if (!isCtrl) false
      else {
        val _loop  = Option(loop)
        val _level = Option(level)
        val _sched = Option(sched)
        val ctrlLoop  = this.looping
        val ctrlLevel = this.level
        val ctrlSched = this.schedule

        val hasLoop = _loop.isEmpty || _loop.contains(ctrlLoop)
        val hasLevel = _level.isEmpty || _level.contains(ctrlLevel)
        val hasSched = _sched.isEmpty || _sched.contains(ctrlSched)

        isCtrl && hasLoop && hasLevel && hasSched
      }
    }

    /** True if this node will become a fully unrolled loop at unrolling time. */
    def isFullyUnrolledLoop: Boolean = op.exists(_.isFullyUnrolledLoop)

    /** True if this is a control block which is not run iteratively. */
    def isSingleControl: Boolean = isCtrl(loop = Single)

    /** True if this is a loop whose body will be executed for multiple iterations at runtime.
      * False for fully unrolled loops and UnitPipe.
      */
    def isLoopControl: Boolean = isCtrl(loop = Looped)

    /** True if this is an inner scope Ctrl stage or symbol. */
    def isInnerControl: Boolean = isCtrl(level = Inner)
    /** True if this is an outer scope Ctrl stage or symbol. */
    def isOuterControl: Boolean = isCtrl(level = Outer)

    /** True if this is a sequential controller. */
    def isSeqControl: Boolean = isCtrl(sched = Sequenced)
    /** True if this is a pipelined controller. */
    def isPipeControl: Boolean = isCtrl(sched = Pipelined)
    /** True if this is a streaming scheduled controller. */
    def isStreamControl: Boolean = isCtrl(sched = Streaming)

    /** True if this is an inner, sequential controller. */
    def isInnerSeqControl: Boolean = isCtrl(level = Inner, sched = Sequenced)

    /** True if this is an outer streaming controller.
      * (Note that all streaming controllers should be outer.)
      */
    def isOuterStreamControl: Boolean = isCtrl(level = Outer, sched = Streaming)

    /** True if this is an outer pipelined controller.
      * (Note that all outer pipelined controllers are loops.)
      */
    def isOuterPipeControl: Boolean = isCtrl(level = Outer, sched = Pipelined)

    /** True if this is an inner controller for a loop. */
    def isInnerLoop: Boolean = isCtrl(loop = Looped, level = Inner)
    /** True if this is an outer controller for a loop. */
    def isOuterLoop: Boolean = isCtrl(loop = Looped, level = Outer)

    /** True if this is a sequential controller for a loop. */
    def isSeqLoop: Boolean = isCtrl(loop = Looped, sched = Sequenced)

    /** True if this is a pipelined controller for a loop. */
    def isPipeLoop: Boolean = isCtrl(loop = Looped, sched = Pipelined)

    /** True if this is an inner pipelined controller for a loop. */
    def isInnerPipeLoop: Boolean = isCtrl(loop = Looped, level = Inner, sched = Pipelined)

    /** True if this is an outer pipelined controller for a loop. */
    def isOuterPipeLoop: Boolean = isCtrl(loop = Looped, level = Outer, sched = Pipelined)

    /** True if this is an outer streaming controller for a loop. */
    def isOuterStreamLoop: Boolean = isCtrl(loop = Looped, level = Outer, sched = Streaming)


    // --- Control Looping / Conditional Execution Functions --- //

    /** True if this controller, counterchain, or counter is statically known to run for an
      * infinite number of iterations. */
    def isForever: Boolean = op match {
      case Some(op: Control[_]) => op.cchains.exists(_._1.isForever)
      case Some(op: CounterChainNew) => op.counters.exists(_.isForever)
      case Some(_: ForeverNew) => true
      case _ => false
    }

    /** True if this controller, counterchain, or counter is statically known to run forever.
      * Also true if any of this controller's descendants will run forever.
      */
    @stateful def willRunForever: Boolean = isForever || children.exists(_.willRunForever)

    /** True if this control or symbol is a loop or occurs within a loop. */
    def willRunMultiple: Boolean = s.exists(_.isLoopControl) || hasLoopAncestor

    /** True if this symbol or Ctrl block takes enables as inputs. */
    def takesEnables: Boolean = op.exists{
      case _:EnPrimitive[_] | _:EnControl[_] => true
      case _ => false
    }


    // --- Control hierarchy --- //

    /** Returns the controller or Host which is the direct controller parent of this controller. */
    def parent: Ctrl

    /** Returns a sequence of all controllers and subcontrollers which are direct children in the
      * control hierarchy of this symbol or controller.
      */
    @stateful def children: Seq[Ctrl.Node]

    /** Returns all ancestors of the controller or symbol.
      * Ancestors are ordered outermost to innermost
      */
    def ancestors: Seq[Ctrl] = Tree.ancestors(this.toCtrl){_.parent}

    /** Returns all ancestors of the controller or symbol, stopping when stop is true (exclusive).
      * Ancestors are ordered outermost to innermost
      */
    def ancestors(stop: Ctrl => Boolean): Seq[Ctrl] = Tree.ancestors(toCtrl, stop){_.parent}

    /** Returns all ancestors of the controller or symbol, stopping at `stop` (exclusive).
      * Ancestors are ordered outermost to innermost
      */
    def ancestors(stop: Ctrl): Seq[Ctrl] = Tree.ancestors[Ctrl](toCtrl, {c => c == stop}){_.parent}

    /** True if this control or symbol has the given ancestor controller. */
    def hasAncestor(ctrl: Ctrl): Boolean = ancestors.contains(ctrl)


    /** True if this control or symbol occurs within a loop. */
    def hasLoopAncestor: Boolean = ancestors.exists(_.isLoopControl)

    /** Returns the child of this controller that contains the given symbol x. None if x does not
      * occur in any of the children.
      */
    @stateful def getChildContaining(x: Sym[_]): Option[Ctrl.Node] = {
      val path = x.ancestors
      children.find{c => path.contains(c) }
    }

    /** Returns all children which occur in dataflow order before the given child. */
    @stateful def childrenPriorTo(child: Ctrl.Node): Seq[Ctrl.Node] = {
      // TODO: Arbitrary dataflow?
      children.takeWhile{c => c != child }
    }

    /** Returns true if all copies of the loop body across iterator iter will have the
      * same execution time.
      * If reference is defined, only accounts for the stages up to and including the reference.
      * This is currently trivially true for inner controllers.
      */
    @stateful def isLockstepAcross(iters: Seq[Idx], reference: Option[Sym[_]]): Boolean = {
      val child = reference.flatMap{ref => this.getChildContaining(ref) }
      val ctrls = if (op.isDefined && op.get.isLoop) children else child.map{c => childrenPriorTo(c) }.getOrElse(children)
      ctrls.forall{c => c.runtimeIsInvariantAcross(iters, reference, allowSwitch = false) } &&
      child.forall{c => c.runtimeIsInvariantAcross(iters, reference, allowSwitch = true) }
    }

    @stateful def runtimeIsInvariantAcross(iters: Seq[Idx], reference: Option[Sym[_]], allowSwitch: Boolean): Boolean = {
      if (isFSM) false
      else if (isSwitch && isOuterControl) {
        allowSwitch && reference.exists{r => r.ancestors.contains(toCtrl) } &&
        isLockstepAcross(iters, reference)
      }
      else {
        // TODO: More restrictive than it needs to be. Change to ctr bounds being invariant w.r.t iters
        isLockstepAcross(iters, reference) &&
        cchains.forall{cchain => cchain.counters.forall{ctr => ctr.nIters match {
          case Some(Expect(_)) => true
          case _ => false
        }}}
      }
    }

    def writtenMems: Set[Sym[_]] = {
      s.flatMap{sym => metadata[WrittenMems](sym).map(_.mems) }.getOrElse(Set.empty)
    }
    def writtenMems_=(mems: Set[Sym[_]]): Unit = {
      s.foreach{sym => metadata.add(sym, WrittenMems(mems)) }
    }

    def readMems: Set[Sym[_]] = {
      s.flatMap{sym => metadata[ReadMems](sym).map(_.mems) }.getOrElse(Set.empty)
    }
    def readMems_=(mems: Set[Sym[_]]): Unit = {
      s.foreach{sym => metadata.add(sym, ReadMems(mems)) }
    }



    // --- Streaming Controllers --- //
    def listensTo: List[StreamInfo] = {
      s.flatMap{sym => metadata[ListenStreams](sym).map(_.listen) }.getOrElse(Nil)
    }
    def listensTo_=(listen: List[StreamInfo]): Unit = {
      s.foreach{sym => metadata.add(sym, ListenStreams(listen)) }
    }

    def pushesTo: List[StreamInfo] = {
      s.flatMap{sym => metadata[PushStreams](sym).map(_.push) }.getOrElse(Nil)
    }
    def pushesTo_=(push: List[StreamInfo]): Unit = {
      s.foreach{sym => metadata.add(sym, PushStreams(push)) }
    }

    /** True if this controller or symbol has a streaming controller ancestor. */
    def hasStreamAncestor: Boolean = ancestors.exists(_.isStreamControl)
    /** True if this controller or symbol has a streaming controller parent. */
    def hasStreamParent: Boolean = toCtrl.parent.isStreamControl

    /** True if this controller or symbol has an ancestor which runs forever. */
    def hasForeverAncestor: Boolean = ancestors.exists(_.isForever)

    /** True if this is an inner controller which directly contains
      * stream enablers/holder accesses.
      */
    def hasStreamAccess: Boolean = isInnerControl && (op match {
      case Some(o) => o.blocks.flatMap(_.nestedStms).exists{ sym =>
        sym.isStreamStageEnabler || sym.isStreamStageHolder
      }
      case None => false
    })

    // --- Tile Transfers --- //
    def isAligned: Boolean = {
      s.flatMap{sym => metadata[AlignedTransfer](sym) }.exists(_.is)
    }
    def isAligned_=(flag: Boolean): Unit = {
      s.foreach{sym => metadata.add(sym, AlignedTransfer(flag)) }
    }

    def loadCtrl: List[Sym[_]] = {
      s.flatMap{sym => metadata[LoadMemCtrl](sym).map(_.ctrl) }.getOrElse(Nil)
    }
    def loadCtrl_=(ls: List[Sym[_]]): Unit = {
      s.foreach{sym => metadata.add(sym, LoadMemCtrl(ls)) }
    }

    def getFringe: Option[Sym[_]] = {
      s.flatMap{sym => metadata[Fringe](sym) }.map(_.fringe)
    }
    def setFringe(fringe: Sym[_]): Unit = {
      s.foreach{sym => metadata.add(sym, Fringe(fringe)) }
    }

    def argMapping: PortMap = {
      s.flatMap{sym => metadata[ArgMap](sym).map(_.map) }.getOrElse(PortMap(-1,-1,-1))
    }
    def argMapping_=(id: PortMap): Unit = {
      s.foreach{sym => metadata.add(sym, ArgMap(id)) }
    }


    /** Returns a list of all counterhains in this controller. */
    def cchains: Seq[CounterChain] = op match {
      case Some(op: Control[_]) => op.cchains.map(_._1).distinct
      case _ => Nil
    }
  }

  abstract class ScopeHierarchyOps(s: Option[Sym[_]]) extends CtrlHierarchyOps(s) {
    // --- Scope hierarchy --- //
    def toScope: Scope

    /** Returns the control scope in which this symbol or controller is defined. */
    def scope: Scope

    /** Returns all scope ancestors of the controller or symbol.
      * Ancestors are ordered outermost to innermost
      */
    def scopes: Seq[Scope] = Tree.ancestors[Scope](this.toScope){_.parent.scope}

    /** Returns all scope ancestors of the controller or symbol, stopping when stop is true (exclusive).
      * Ancestors are ordered outermost to innermost
      */
    def scopes(stop: Scope => Boolean): Seq[Scope] = Tree.ancestors[Scope](toScope, stop){_.parent.scope}

    /** Returns all scope ancestors of the controller or symbol, stopping at `stop` (exclusive).
      * Scopes are ordered outermost to innermost.
      */
    def scopes(stop: Scope): Seq[Scope] = Tree.ancestors[Scope](toScope, {c => c == stop})(_.parent.scope)
  }


  implicit class SymControlOps(s: Sym[_]) extends ScopeHierarchyOps(Some(s)) {
    def toCtrl: Ctrl = if (s.isControl) Ctrl.Node(s,-1) else s.parent
    def toScope: Scope = if (s.isControl) Scope.Node(s,-1,-1) else s.scope
    def isControl: Boolean = s.op.exists(_.isControl)

    @stateful def children: Seq[Ctrl.Node] = {
      if (s.isControl) toCtrl.children
      else throw new Exception(s"Cannot get children of non-controller ${stm(s)}")
    }

    def parent: Ctrl = s.rawParent
    def scope: Scope = s.rawScope

    /** Returns an Option of the control level (Inner or Outer) metadata. None if undefined. */
    def getRawLevel: Option[CtrlLevel] = metadata[ControlLevel](s).map(_.level)
    /** Returns the control level (Inner or Outer) metadata. Use .level for most purposes. */
    def rawLevel: CtrlLevel = getRawLevel.getOrElse{throw new Exception(s"No control level defined for $s") }
    def rawLevel_=(level: CtrlLevel): Unit = metadata.add(s, ControlLevel(level))

    def isRawOuter: Boolean = rawLevel == Outer
    def isRawInner: Boolean = rawLevel == Inner

    def getRawSchedule: Option[CtrlSchedule] = metadata[ControlSchedule](s).map(_.sched)
    def rawSchedule: CtrlSchedule = getRawSchedule.getOrElse{ throw new Exception(s"Undefined schedule for $s") }
    def rawSchedule_=(sched: CtrlSchedule): Unit = metadata.add(s, ControlSchedule(sched))

    def isRawSeq: Boolean = rawSchedule == Sequenced
    def isRawPipe: Boolean = rawSchedule == Pipelined
    def isRawStream: Boolean = rawSchedule == Streaming
    def isForkJoin: Boolean = rawSchedule == ForkJoin
    def isFork: Boolean = rawSchedule == Fork

    def getUserSchedule: Option[CtrlSchedule] = metadata[UserScheduleDirective](s).map(_.sched)
    def userSchedule: CtrlSchedule = getUserSchedule.getOrElse{throw new Exception(s"Undefined user schedule for $s") }
    def userSchedule_=(sched: CtrlSchedule): Unit = metadata.add(s, UserScheduleDirective(sched))


    // --- Control Hierarchy --- //

    def getOwner: Option[Sym[_]] = metadata[CounterOwner](s).map(_.owner)
    def owner: Sym[_] = getOwner.getOrElse{throw new Exception(s"Undefined counter owner for $s") }
    def owner_=(own: Sym[_]): Unit = metadata.add(s, CounterOwner(own))

    def rawParent: Ctrl = metadata[ParentCtrl](s).map(_.parent).getOrElse(Ctrl.Host)
    def rawParent_=(p: Ctrl): Unit = metadata.add(s, ParentCtrl(p))

    def rawChildren: Seq[Ctrl.Node] = {
      if (!s.isControl) throw new Exception(s"Cannot get children of non-controller.")
      metadata[Children](s).map(_.children).getOrElse(Nil)
    }
    def rawChildren_=(cs: Seq[Ctrl.Node]): Unit = metadata.add(s, Children(cs))

    // --- Scope Hierarchy --- //

    def rawScope: Scope = metadata[ScopeCtrl](s).map(_.scope).getOrElse(Scope.Host)
    def rawScope_=(scope: Scope): Unit = metadata.add(s, ScopeCtrl(scope))

    // --- IR Hierarchy --- //

    def blk: Blk = metadata[DefiningBlk](s).map(_.blk).getOrElse(Blk.Host)
    def blk_=(b: Blk): Unit = metadata.add(s, DefiningBlk(b))

    def bodyLatency: Seq[Double] = metadata[BodyLatency](s).map(_.latency).getOrElse(Nil)
    def bodyLatency_=(latencies: Seq[Double]): Unit = metadata.add(s, BodyLatency(latencies))
    def bodyLatency_=(latency: Double): Unit = metadata.add(s, BodyLatency(Seq(latency)))

    @stateful def latencySum: Double = if (spatialConfig.enableRetiming) s.bodyLatency.sum else 0.0

    def II: Double = metadata[InitiationInterval](s).map(_.interval).getOrElse(1.0)
    def II_=(interval: Double): Unit = metadata.add(s, InitiationInterval(interval))

    def userII: Option[Double] = metadata[UserII](s).map(_.interval)
    def userII_=(interval: Option[Double]): Unit = interval.foreach{ii => metadata.add(s, UserII(ii)) }

  }


  implicit class ScopeOperations(scp: Scope) extends ScopeHierarchyOps(scp.s) {
    def toCtrl: Ctrl = scp match {
      case Scope.Node(sym,id,_) => Ctrl.Node(sym,id)
      case Scope.Host           => Ctrl.Host
    }
    def toScope: Scope = scp
    def isControl: Boolean = true

    @stateful def children: Seq[Ctrl.Node] = toCtrl.children
    def parent: Ctrl = toCtrl.parent
    def scope: Scope = scp

    def iters: Seq[I32] = Try(scp match {
      case Scope.Host => Nil
      case Scope.Node(Op(loop: Loop[_]), -1, -1)         => loop.iters
      case Scope.Node(Op(loop: Loop[_]), stage, block)   => loop.bodies(stage).blocks.apply(block)._1
      case Scope.Node(Op(loop: UnrolledLoop[_]), -1, -1) => loop.iters
      case Scope.Node(Op(loop: UnrolledLoop[_]), i ,_)   => loop.bodiess.apply(i)._1.flatten
      case _ => Nil
    }).getOrElse(throw new Exception(s"$scope had no iterators defined"))

    def valids: Seq[Bit] = Try(scp match {
      case Scope.Host => Nil
      case Scope.Node(Op(loop: UnrolledLoop[_]), _, _) => loop.valids
      case _ => Nil
    }).getOrElse(throw new Exception(s"$scope had no valids defined"))

  }


  implicit class CtrlControl(ctrl: Ctrl) extends CtrlHierarchyOps(ctrl.s) {
    def toCtrl: Ctrl = ctrl
    def isControl: Boolean = true

    @stateful def children: Seq[Ctrl.Node] = ctrl match {
      // "Master" controller case
      case Ctrl.Node(sym, -1) => sym match {
        case Op(ctrl: Control[_]) => ctrl.bodies.zipWithIndex.flatMap{case (body,id) =>
          val stage = Ctrl.Node(sym,id)
          // If this is a pseudostage, transparently return all controllers under this pseudostage
          if (body.isPseudoStage) sym.rawChildren.filter(_.scope.toCtrl == stage)
          // Otherwise return the subcontroller for this "future" stage
          else Seq(Ctrl.Node(sym, id))
        }

        case Op(ctrl: IfThenElse[_]) => sym.rawChildren // Fixme?

        case _ => throw new Exception(s"Cannot get children of non-controller.")
      }
      // Subcontroller case - return all children which have this subcontroller as an owner
      case Ctrl.Node(sym,_) => sym.rawChildren.filter(_.scope.toCtrl == ctrl)

      // The children of the host controller is all Accel scopes in the program
      case Ctrl.Host => AccelScopes.all
    }

    def parent: Ctrl = ctrl match {
      case Ctrl.Node(sym,-1) => sym.parent
      case Ctrl.Node(sym, _) => Ctrl.Node(sym, -1)
      case Ctrl.Host => Ctrl.Host
    }

    def scope: Scope = ctrl match {
      case Ctrl.Node(sym,-1) => sym.scope
      case Ctrl.Node(sym, _) => Scope.Node(sym, -1, -1)
      case Ctrl.Host         => Scope.Host
    }
  }

  implicit class BlkOps(blk: Blk) {
    def toScope: Scope = blk match {
      case Blk.Host      => Scope.Host
      case Blk.Node(s,i) => s match {
        case Op(op:Control[_]) =>
          val block = op.blocks(i)
          val stage = op.bodies.indexWhere(_.blocks.exists(_._2 == block))
          val blk   = op.bodies(stage).blocks.indexWhere(_._2 == block)
          Scope.Node(s, stage, blk)

        case _ => Scope.Node(s, -1, -1)
      }
    }
  }


  implicit class CounterChainHelperOps(x: CounterChain) {
    def node: CounterChainNew = x match {
      case Op(c: CounterChainNew) => c
      case _ => throw new Exception(s"Could not find counterchain definition for $x")
    }

    def counters: Seq[Counter[_]] = x.node.counters
    def pars: Seq[I32] = counters.map(_.ctrPar)
    def constPars: Seq[Int] = pars.map(_.toInt)
    def willFullyUnroll: Boolean = counters.forall(_.willFullyUnroll)
    def isUnit: Boolean = counters.forall(_.isUnit)
    def isStatic: Boolean = counters.forall(_.isStatic)
  }


  implicit class CounterHelperOps[F](x: Counter[F]) {
    def node: CounterNew[F] = x match {
      case Op(c: CounterNew[_]) => c.asInstanceOf[CounterNew[F]]
      case _ => throw new Exception(s"Could not find counter definition for $x")
    }

    def start: Sym[F] = x.node.start
    def step: Sym[F] = x.node.step
    def end: Sym[F] = x.node.end
    def ctrPar: I32 = x.node.par
    def isStatic: Boolean = (start,step,end) match {
      case (Final(_), Final(_), Final(_)) => true
      case _ => false
    }
    def nIters: Option[Bound] = (start,step,end) match {
      case (Final(min), Final(stride), Final(max)) =>
        Some(Final(Math.ceil((max - min).toDouble / stride).toInt))

      case (Expect(min), Expect(stride), Expect(max)) =>
        Some(Expect(Math.ceil((max - min).toDouble / stride).toInt))

      case _ => None
    }
    def willFullyUnroll: Boolean = (nIters,ctrPar) match {
      case (Some(Expect(nIter)), Expect(par)) => par >= nIter
      case _ => false
    }
    def isUnit: Boolean = nIters match {
      case (Some(Final(1))) => true
      case _ => false
    }
  }

  implicit class IndexHelperOps[W](i: Ind[W]) {
    def ctrStart: Ind[W] = i.counter.start.unbox
    def ctrStep: Ind[W] = i.counter.step.unbox
    def ctrEnd: Ind[W] = i.counter.end.unbox
    def ctrPar: I32 = i.counter.ctrPar
    def ctrParOr1: Int = i.getCounter.map(_.ctrPar.toInt).getOrElse(1)
  }

  implicit class IndexCounterOps[A](i: Num[A]) {
    def getCounter: Option[Counter[A]] = metadata[IndexCounter](i).map(_.ctr.asInstanceOf[Counter[A]])
    def counter: Counter[A] = getCounter.getOrElse{throw new Exception(s"No counter associated with $i") }
    def counter_=(ctr: Counter[_]): Unit = metadata.add(i, IndexCounter(ctr))
  }


  /** True if the given symbol is allowed to be defined on the Host and used in Accel
    * This is true for the following types:
    *   - Global (shared) memories (e.g. DRAM, ArgIn, ArgOut, HostIO, StreamIn, StreamOut)
    *   - Bit-based types (if ArgIn inference is allowed)
    *   - Text (staged strings) (TODO: only allowed in debug mode?)
    */
  def allowSharingBetweenHostAndAccel(s: Sym[_], allowArgInference: Boolean): Boolean = {
    s.isRemoteMem ||
      (s.isBits && allowArgInference) ||
      s.isInstanceOf[Text]
  }

  def collectCChains(block: Block[_]): Seq[CounterChain] = collectCChains(block.stms)
  def collectCChains(stms: Seq[Sym[_]]): Seq[CounterChain] = stms.collect{case s: CounterChain => s}



  /** Returns the least common ancestor (LCA) of the two controllers.
    * If the controllers have no ancestors in common, returns None.
    */
  def LCA(a: Sym[_], b: Sym[_]): Ctrl = LCA(a.toCtrl,b.toCtrl)
  def LCA(a: Ctrl, b: Ctrl): Ctrl= Tree.LCA(a, b){_.parent}

  /** Returns the least common ancestor (LCA) of a list of controllers.
    * Also returns the pipeline distance between the first and last accesses,
    * and the pipeline distance between the first access and the first stage.
    * If the controllers have no ancestors in common, returns None.
    */
  @stateful def LCA(n: => Set[Sym[_]]): Ctrl = { LCA(n.map(_.toCtrl)) }
  @stateful def LCA(n: Set[Ctrl]): Ctrl = {
    if (n.isEmpty) throw new Exception(s"No LCA for empty set")
    else n.reduce{(a,b) => LCA(a,b) }
  }
  @stateful def LCAPortMatchup(n: List[Sym[_]], lca: Ctrl): (Int,Int) = {
    val lcaChildren = lca.children.toList.map(_.master)
    val portMatchup = n.map{a =>
      val idx = lcaChildren.indexWhere{s => a.ancestors.map(_.master).contains(s) }
      if (idx < 0) {
        ctrlTree(n.toSet).foreach{line => dbgs(line) }
        throw new Exception(s"Access $a doesn't seem to occur under LCA $lca? (accesses: $n)")
      }
      idx
    }
    val basePort = portMatchup.min
    val numPorts = portMatchup.max - portMatchup.min
    (basePort, numPorts)
  }

  def LCAWithPaths(a: Ctrl, b: Ctrl): (Ctrl, Seq[Ctrl], Seq[Ctrl]) = {
    Tree.LCAWithPaths(a,b){_.parent}
  }

  /** Returns the stage distance between a and b with respect to the common controller ctrl.
    *
    * The stage distance between a and b is defined as:
    *   If a and b are equal: 0
    *   If ctrl is an inner controller: 0
    *   If ctrl is not a common ancestor of both a and b: <undefined>
    *   If ctrl == a XOR ctrl == b: <undefined> (ctrl == a == b: 0)
    *   Otherwise: dataflow distance between the immediate child(ren) of ctrl containing a and b.
    *
    * The distance is positive if a comes before b (dataflow order), negative otherwise.
    */
  @stateful def getStageDistance(ctrl: Ctrl, a: Ctrl, b: Ctrl): Option[Int] = {
    if (a == b) Some(0)
    else if (ctrl.isInnerControl) Some(0)
    else {
      val pathA = a.ancestors
      val pathB = b.ancestors
      val ctrlIdxA = pathA.indexOf(ctrl)
      val ctrlIdxB = pathB.indexOf(ctrl)
      logs(s"  PathA: " + pathA.mkString(", "))
      logs(s"  PathB: " + pathB.mkString(", "))
      logs(s"  Ctrl: $ctrl")
      logs(s"  ctrlIdxA: $ctrlIdxA")
      logs(s"  ctrlIdxB: $ctrlIdxB")

      if (ctrlIdxA < 0 || ctrlIdxB < 0) None        // ctrl is not common to a and b
      else if (ctrlIdxA >= pathA.length - 1) None   // implies ctrl == a
      else if (ctrlIdxB >= pathB.length - 1) None   // implies ctrl == b
      else {
        // TODO[2]: Revise to account for arbitrary dataflow graphs for children controllers
        val topA = pathA(ctrlIdxA + 1)
        val topB = pathB(ctrlIdxB + 1)
        val idxA = ctrl.children.indexOf(topA)
        val idxB = ctrl.children.indexOf(topB)
        logs(s"  A: $a, B: $b")
        logs(s"  ${ctrl.children.mkString(" ")}")
        logs(s"  CtrlA: $topA ($idxA), CtrlB: $topB ($idxB)")
        logs(s"  Dist = ${idxB - idxA}")
        if (idxA < 0 || idxB < 0) None
        Some(idxB - idxA)
      }
    }
  }

  @stateful def getStageDistance(ctrl: Ctrl, a: Sym[_], b: Sym[_]): Option[Int] = {
    getStageDistance(ctrl, a.toCtrl, b.toCtrl)
  }

  /** Returns the LCA between a and b along with their dataflow distance w.r.t. the LCA. */
  @stateful def LCAWithDataflowDistance(a: Ctrl, b: Ctrl): (Ctrl,Int) = {
    val lca = LCA(a, b)
    // TODO[2]: This should return a non-zero distance for inner controllers
    val dist = getStageDistance(lca, a, b).getOrElse(0)
    (lca, dist)
  }

  @stateful def LCAWithDataflowDistance(a: Sym[_], b: Sym[_]): (Ctrl,Int) = {
    LCAWithDataflowDistance(a.toCtrl,b.toCtrl)
  }

  /** Returns the coarse distance between two symbols a and b from the given controller ctrl.
    *
    * The coarse distance is defined as:
    *   If ctrl is an outer pipeline or streaming controller: the stage distance between a and b
    *   Otherwise: <undefined>
    */
  @stateful def getCoarseDistance(ctrl: Ctrl, a: Ctrl, b: Ctrl): Option[Int] = {
    val dist = getStageDistance(ctrl, a, b)
    if (ctrl.isOuterPipeLoop || ctrl.isOuterStreamLoop) dist else None
  }

  @stateful def getCoarseDistance(ctrl: Ctrl, a: Sym[_], b: Sym[_]): Option[Int] = {
    getCoarseDistance(ctrl,a.toCtrl,b.toCtrl)
  }

  /** Returns the LCA between a and b along with their stage distance w.r.t. the LCA. */
  @stateful def getLCAWithStageDistance(a: Ctrl, b: Ctrl): Option[(Ctrl,Int)] = {
    val lca = LCA(a, b)
    getStageDistance(lca, a, b).map{dist => (lca,dist) }
  }

  /** Returns the LCA between a and b along with their stage distance w.r.t. the LCA. */
  @stateful def LCAWithStageDistance(a: Ctrl, b: Ctrl): (Ctrl,Int) = {
    val lca = LCA(a, b)
    val dist = getStageDistance(lca, a, b)
    if (dist.isEmpty) throw new Exception(s"Stage distance between $a and $b is undefined.")
    (lca, dist.get)
  }

  @stateful def LCAWithStageDistance(a: Sym[_], b: Sym[_]): (Ctrl,Int) = {
    LCAWithStageDistance(a.toCtrl,b.toCtrl)
  }

  /** Returns the LCA between a and b along with their coarse distance w.r.t. the LCA. */
  @stateful def getLCAWithCoarseDistance(a: Ctrl, b: Ctrl): Option[(Ctrl,Int)] = {
    val lca = LCA(a,b)
    getCoarseDistance(lca, a, b).map{dist => (lca,dist) }
  }

  @stateful def getLCAWithCoarseDistance(a: Sym[_], b: Sym[_]): Option[(Ctrl,Int)] = {
    getLCAWithCoarseDistance(a.toCtrl, b.toCtrl)
  }


  /** Returns all metapipeline parents between all pairs of (w in writers, a in readers U writers). */
  @stateful def findAllMetaPipes(readers: Set[Sym[_]], writers: Set[Sym[_]]): Map[Ctrl,Set[(Sym[_],Sym[_])]] = {
    if (writers.isEmpty && readers.isEmpty) Map.empty
    else {
      val ctrlGrps: Set[(Ctrl,(Sym[_],Sym[_]))] = writers.flatMap{w =>
        (readers ++ writers).flatMap{a =>
          getLCAWithCoarseDistance(w, a) match {
            case Some((lca, dist)) =>
              dbgs(s"  $a <-> $w: LCA: $lca, coarse-dist: $dist")
              if (dist != 0) Some((lca.master, (w,a))) else None
            case _ =>
              dbgs(s"  $a <-> $w: LCA: ${LCA(a,w)}, coarse-dist: <None>")
              None
          }
        }
      }
      ctrlGrps.groupBy(_._1).mapValues(_.map(_._2))
    }
  }

  /** Computes the required buffer depth for the given memory and accesses.
    * Memories are buffered on producers/consumers across stages in coarse-grained pipelines.
    *
    * Returns a buffer pipeline controller, if applicable.
    * Returns a mapping from access symbol to buffer port.
    * Returns an optional issue if hiearchical pipelining is required to support the reads.
    *
    * A buffer port is either Some(index), where index is the port location on an N-buffer.
    * The maximum port for an N-buffer is N-1.
    * Reads and writes which occur in a time-multiplexed fashion outside the pipeline are notated
    * with a buffer port of None.
    */
  @stateful def computeMemoryBufferPorts(mem: Sym[_], readers: Set[Sym[_]], writers: Set[Sym[_]]): (Option[Ctrl], Map[Sym[_],Option[Int]], Option[Issue]) = {
    val accesses = readers ++ writers

    if (mem.isNonBuffer) {
      val ports = accesses.map{a => a -> Some(0) }.toMap
      (None, ports, None)
    }
    else {
      val metapipeLCAs = findAllMetaPipes(readers, writers)
      val hierarchicalBuffer = metapipeLCAs.keys.size > 1
      val issue = if (hierarchicalBuffer) Some(AmbiguousMetaPipes(mem, metapipeLCAs)) else None

      metapipeLCAs.keys.headOption match {
        case Some(metapipe) =>
          val group: Set[(Sym[_],Sym[_])] = metapipeLCAs(metapipe)
          val anchor: Sym[_] = group.head._1
          val dists = accesses.map{a =>
            val dist = getCoarseDistance(metapipe, anchor, a)
            dbgs(s"$a <-> $anchor # LCA: $metapipe, Dist: $dist")

            if (group.exists{x => a == x._1 || a == x._2 }) a -> dist else a -> None
          }
          val buffers = dists.filter{_._2.isDefined}.map(_._2.get)
          val minDist = buffers.minOrElse(0)
          val ports = dists.map{case (a,dist) => a -> dist.map{d => d - minDist} }.toMap
          (Some(metapipe), ports, issue)

        case None =>
          (None, accesses.map{a => a -> Some(0)}.toMap, issue)
      }
    }
  }

  /** Creates a String representation of the controller tree over all of the given accesses. */
  @stateful def ctrlTree(accesses: Set[Sym[_]], top: Option[Ctrl] = None, tab: Int = 0): Iterator[String] = {
    if (accesses.nonEmpty) {
      val lca_init = LCA(accesses)
      val lca = top match {
        case Some(ctrl) if lca_init.ancestors.contains(ctrl) => ctrl
        case None => lca_init
      }

      val head: Iterator[String] = Seq(lca match {
        case Ctrl.Node(s,id) => "  "*tab + s"${shortStm(s)} ($id) [Level: ${lca.level}, Loop: ${lca.looping}, Schedule: ${lca.schedule}]"
        case Ctrl.Host       => "  "*tab + "Host"
      }).iterator
      if (lca.isInnerControl) {
        val inner: Iterator[String] = head ++ accesses.iterator.map{a => "  "*tab + s"  ${shortStm(a)}" }
        inner
      }
      else {
        val children = lca.children.iterator
        val childStrs: Iterator[String] = children.flatMap{ child =>
          val accs = accesses.filter(_.ancestors.contains(child))
          ctrlTree(accs, Some(child), tab + 1)
        }
        head ++ childStrs
      }
    }
    else Nil.iterator
  }

  @stateful def getReadStreams(ctrl: Ctrl): Set[Sym[_]] = {
    // ctrl.children.flatMap(getReadStreams).toSet ++
    LocalMemories.all.filter{mem => mem.readers.exists{_.parent.s == ctrl.s }}
      .filter{mem => mem.isStreamIn || mem.isFIFO }
    // .filter{case Op(StreamInNew(bus)) => !bus.isInstanceOf[DRAMBus[_]]; case _ => true}
  }

  @stateful def getWriteStreams(ctrl: Ctrl): Set[Sym[_]] = {
    // ctrl.children.flatMap(getWriteStreams).toSet ++
    LocalMemories.all.filter{mem => mem.writers.exists{c => c.parent.s == ctrl.s }}
      .filter{mem => mem.isStreamOut || mem.isFIFO }
    // .filter{case Op(StreamInNew(bus)) => !bus.isInstanceOf[DRAMBus[_]]; case _ => true}
  }


}
