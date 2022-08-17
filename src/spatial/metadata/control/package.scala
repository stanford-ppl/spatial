package spatial.metadata

import argon._
import argon.lang.Ind
import argon.node._
import forge.tags.{stateful,rig}
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.bounds._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.metadata.blackbox._
import spatial.util.spatialConfig
import spatial.issues.{AmbiguousMetaPipes, PotentialBufferHazard}

import scala.util.Try

import utils.Tree
import utils.implicits.collections._
import spatial.metadata.blackbox._

package object control {

  implicit class ControlOpOps(op: Op[_]) {
    /** True if this Op is a loop which has a body which is run multiple times at runtime. */
    @stateful def isLoop: Boolean = op match {
      case loop: Loop[_] => !loop.cchains.forall(_._1.willFullyUnroll)
      case _ => false
    }
    @stateful def isFullyUnrolledLoop: Boolean = op match {
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
    def isSwitchCase: Boolean = op match {
      case _:SwitchCase[_] => true
      case _ => false
    }

    def isParallel: Boolean = op.isInstanceOf[ParallelPipe]

    def isUnitPipe: Boolean = op.isInstanceOf[UnitPipe]

    def isMemReduce: Boolean = op match {
      case _:OpMemReduce[_,_] => true
      case _ => false
    }

    def memReduceItersRed: Seq[I32] = op match {
      case _@OpMemReduce(_,_,_,_,_,_,_,_,_,_,_,_,itersRed,_) => itersRed
      case _ => Seq()
    }

    def isAnyReduce: Boolean = op match {
      case _:OpMemReduce[_,_] => true
      case _:OpReduce[_] => true
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

    def isLoad: Boolean = op match {
      case _:FringeDenseLoad[_,_] => true
      case _ => false
    }

    def isGather: Boolean = op match {
      case _:FringeSparseLoad[_,_] => true
      case _ => false
    }

    def isStore: Boolean = op match {
      case _:FringeDenseStore[_,_] => true
      case _ => false
    }

    def isScatter: Boolean = op match {
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
    def isSwitchCase: Boolean = op.exists(_.isSwitchCase)
    def isBranch: Boolean = op.exists(_.isBranch)
    def isParallel: Boolean = op.exists(_.isParallel)
    def isUnitPipe: Boolean = op.exists(_.isUnitPipe)
    def isFSM: Boolean = op.exists(_.isFSM)

    def isMemReduce: Boolean = op.exists(_.isMemReduce)
    def memReduceItersRed: Seq[I32] = op.flatMap{x => Some(x.memReduceItersRed)}.get
    def isAnyReduce: Boolean = op.exists(_.isAnyReduce)

    def isStreamLoad: Boolean = op.exists(_.isStreamLoad)
    def isTileTransfer: Boolean = op.exists(_.isTileTransfer)
    def isTileLoad: Boolean = op.exists(_.isTileLoad)
    def isTileStore: Boolean = op.exists(_.isTileStore)

    def isLoad: Boolean = op.exists(_.isLoad)
    def isGather: Boolean = op.exists(_.isGather)
    def isStore: Boolean = op.exists(_.isStore)
    def isScatter: Boolean = op.exists(_.isScatter)

    def isCounter: Boolean = s.exists(_.isInstanceOf[Counter[_]])
    def isCounterChain: Boolean = s.exists(_.isInstanceOf[CounterChain])

    /** Returns the level of this controller (Outer or Inner). */
    def level: CtrlLevel = toCtrl match {
      case ctrl @ Ctrl.Node(sym,_) if sym.isRawOuter && ctrl.mayBeOuterBlock => Outer
      case Ctrl.Host => Outer
      case Ctrl.SpatialBlackbox(sym) => Inner
      case _         => Inner
    }

    @stateful def innerBlocks: Seq[(Seq[I32],Block[_])] = op match {
      case Some(ctrl:Control[_]) => ctrl.bodies.zipWithIndex.flatMap{case (body, id) =>
        val stage = s.map{sym => Ctrl.Node(sym, id) }.getOrElse(Ctrl.Host)
        if (!stage.mayBeOuterBlock || this.isInnerControl) body.blocks else Nil
      }
      case _ => Nil
    }
    @stateful def outerBlocks: Seq[(Seq[I32],Block[_])] = op match {
      case Some(ctrl:Control[_]) => ctrl.bodies.zipWithIndex.flatMap{case (body, id) =>
        val stage = s.map{sym => Ctrl.Node(sym, id) }.getOrElse(Ctrl.Host)
        if (stage.mayBeOuterBlock && this.isOuterControl) body.blocks else Nil
      }
      case _ => Nil
    }

    @stateful def innerAndOuterBlocks: (Seq[(Seq[I32],Block[_])], Seq[(Seq[I32],Block[_])]) = {
      (innerBlocks, outerBlocks)
    }

    /** Returns whether this control node is a Looped control or Single iteration control.
      * Nodes which will be fully unrolled are considered Single control.
      */
    @stateful def looping: CtrlLooping = if (op.exists(_.isLoop)) Looped else Single

    @stateful def schedule: CtrlSchedule = {
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
            case PrimitiveBox => PrimitiveBox
          }
          case (Single, Outer) => actualSchedule match {
            case Sequenced => Sequenced
            case Pipelined => Sequenced
            case Streaming => Streaming
            case ForkJoin  => ForkJoin
            case Fork => Fork
            case PrimitiveBox => PrimitiveBox
          }
        }
      }
    }

    @stateful def isCtrl(
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

        val hasLoop = _loop.isEmpty || _loop.contains(this.looping)
        val hasLevel = _level.isEmpty || _level.contains(this.level)
        val hasSched = _sched.isEmpty || _sched.contains(this.schedule)

        isCtrl && hasLoop && hasLevel && hasSched
      }
    }

    /** True if this node will become a fully unrolled loop at unrolling time. */
    @stateful def isFullyUnrolledLoop: Boolean = op.exists(_.isFullyUnrolledLoop)

    /** True if this is a control block which is not run iteratively. */
    @stateful def isSingleControl: Boolean = isCtrl(loop = Single)

    /** True if this is a loop whose body will be executed for multiple iterations at runtime.
      * False for fully unrolled loops and UnitPipe.
      */
    @stateful def isLoopControl: Boolean = isCtrl(loop = Looped)

    /** True if this is an inner scope Ctrl stage or symbol. */
    @stateful def isInnerControl: Boolean = isCtrl(level = Inner)
    /** True if this is an outer scope Ctrl stage or symbol. */
    @stateful def isOuterControl: Boolean = isCtrl(level = Outer)

    /** True if this is a sequential controller. */
    @stateful def isSeqControl: Boolean = isCtrl(sched = Sequenced)
    /** True if this is a pipelined controller. */
    @stateful def isPipeControl: Boolean = isCtrl(sched = Pipelined)
    /** True if this is a streaming scheduled controller. */
    @stateful def isStreamControl: Boolean = isCtrl(sched = Streaming)

    /** True if this is an inner, sequential controller. */
    @stateful def isInnerSeqControl: Boolean = isCtrl(level = Inner, sched = Sequenced)

    /** True if this is an outer streaming controller.
      * (Note that all streaming controllers should be outer.)
      */
    @stateful def isOuterStreamControl: Boolean = isCtrl(level = Outer, sched = Streaming)

    /** True if this is an outer pipelined controller.
      * (Note that all outer pipelined controllers are loops.)
      */
    @stateful def isOuterPipeControl: Boolean = isCtrl(level = Outer, sched = Pipelined)

    /** True if this is an inner controller for a loop. */
    @stateful def isInnerLoop: Boolean = isCtrl(loop = Looped, level = Inner)
    /** True if this is an outer controller for a loop. */
    @stateful def isOuterLoop: Boolean = isCtrl(loop = Looped, level = Outer)

    /** True if this is a sequential controller for a loop. */
    @stateful def isSeqLoop: Boolean = isCtrl(loop = Looped, sched = Sequenced)

    /** True if this is a pipelined controller for a loop. */
    @stateful def isPipeLoop: Boolean = isCtrl(loop = Looped, sched = Pipelined)

    /** True if this is an inner pipelined controller for a loop. */
    @stateful def isInnerPipeLoop: Boolean = isCtrl(loop = Looped, level = Inner, sched = Pipelined)

    /** True if this is an outer pipelined controller for a loop. */
    @stateful def isOuterPipeLoop: Boolean = isCtrl(loop = Looped, level = Outer, sched = Pipelined)

    /** True if this is an outer streaming controller for a loop. */
    @stateful def isOuterStreamLoop: Boolean = isCtrl(loop = Looped, level = Outer, sched = Streaming)


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
    @stateful def willRunMultiple: Boolean = s.exists(_.isLoopControl) || hasLoopAncestor

    /** True if this symbol or Ctrl block takes enables as inputs. */
    def takesEnables: Boolean = op.exists{
      case _:EnPrimitive[_] | _:EnControl[_] => true
      case _ => false
    }

    def nDim: Int = op match {
      case Some(op: CounterChainNew) => op.counters.length
    }

    // --- Control hierarchy --- //

    /** Returns the controller or Host which is the direct controller parent of this controller. */
    def parent: Ctrl

    /** Returns a sequence of all controllers and subcontrollers which are direct children in the
      * control hierarchy of this symbol or controller.
      */
    @stateful def children: Seq[Ctrl.Node]

    /** Returns a sequence of all controllers and subcontrollers which are siblings (children of parent) in the
      * control hierarchy of this symbol or controller.
      */
    @stateful def siblings: Seq[Ctrl.Node]

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
    @stateful def hasLoopAncestor: Boolean = ancestors.exists(_.isLoopControl)

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

    /** Use a flat Seq[Idx] to group a corresponding Seq[_] based on whether they appear in the same multilevel counter chain */
    @stateful def bundleLayers[T](leaf: Sym[_], iters: Seq[Idx], elements: Seq[T]): Seq[Seq[T]] = {
      import scala.collection.mutable.ArrayBuffer
      var layer: scala.Int = 0
      var ctrl = iters.head.parent.s.get
      val groups = ArrayBuffer(ArrayBuffer[T]())
      dbgs(s"Leaf: $leaf, Iters: $iters, target: $ctrl, elements: $elements")

      iters.map(_.parent.s.get).zipWithIndex.foreach{case (cur, i) =>
        // Part of this layer
        if (cur == ctrl) groups(layer) += elements(i)
        // Next layer
        else if (cur.parent.s.get == ctrl) {
          layer = layer + 1
          ctrl = cur
          groups += ArrayBuffer(elements(i))
        }
        // Skipped layer(s)
        else {
          var current = cur
          while (current != ctrl) {
            layer = layer + 1
            val next = current.parent.s.get
            if (next == ctrl) groups += ArrayBuffer(elements(i))
            else groups += ArrayBuffer()
            current = next
          }
          ctrl = cur
        }
      }

      // NOTE: Make sure we extend to leaf
      var current = leaf
      while (current != ctrl) {
        dbgs(s"Current: $current")
        layer = layer + 1
        groups += ArrayBuffer()
        current = current.parent.s.get
      }

      groups
    }

    /** Return breakWhen node for controller if it has one */
    def breaker: Option[Sym[_]] = s match {
      case Some(Op(_@OpForeach(_,_,_,_,breakWhen))) => breakWhen
      case Some(Op(_@UnrolledForeach(_,_,_,_,_,breakWhen))) => breakWhen
      case Some(Op(_@UnitPipe(_,_,breakWhen))) => breakWhen
      case Some(Op(_@OpReduce(_,_,_,_,_,_,_,_,_,_,breakWhen))) => breakWhen
      case Some(Op(_@OpMemReduce(_,_,_,_,_,_,_,_,_,_,_,_,_,breakWhen))) => breakWhen
      case Some(Op(_@UnrolledReduce(_,_,_,_,_,breakWhen))) => breakWhen
      case _ => None
    }

    /** Find controllers between two iterators */
    def ctrlsBetween(top: Idx, bottom: Sym[_], chain: Seq[Sym[_]] = Seq()): Seq[Sym[_]] = {
      if (bottom == top.parent.s.get) Seq(bottom) ++ chain
      else ctrlsBetween(top,bottom.parent.s.get,Seq(bottom) ++ chain)
    }

    /** Returns how iterators derived from UID are related to iterators derived from baseline UID.
      * We determine a value for each iterator based on lockstep-ness of the relevent sub-tree:
      *   If iterator i from UID can be treated as the same iterator i from baseline, then i -> 0.
      *   If iterator i from UID is random relative to iterator i from baseline, then i will not be in the map
      *   If iterator i from UID is systematically offset from iterator i from baseline, then i -> offset.
      * Lockstep-ness of an anchor point is true if every child of that anchor point runs for the same number of cycles in UID as baseline
      * When determining lockstep-ness, we treat PoM and MoP as follows:
      *   If a forkPoint unrolls as MoP, the anchor point for the lockstep check is just above the relevent child of that forkPoint
      *   If a forkPoint unrolls as PoM, the anchor point for the lockstep check is just above the forkPoint itself
      */
    @stateful def iterSynchronizationInfo(leaf: Sym[_], iters: Seq[Idx], baseUID: Seq[Int], uid: Seq[Int]): Map[Idx, Int] = {
      import scala.collection.mutable.ArrayBuffer
      import spatial.util.modeling._
      val map = scala.collection.mutable.HashMap[Idx,Option[Int]]()
      // 1) Bundle iters/uids based on the depth of the counter chain they are part of
      val bundledIters = bundleLayers(leaf, iters, iters)
      val bundledUID = bundleLayers(leaf, iters, uid)
      val bundledBase = bundleLayers(leaf, iters, baseUID)
      val controlChain = ctrlsBetween(iters.head, leaf)
      var forked = false
      var firstFork = false
      var foundRandomLayer = false
      var layer = 0
      // TODO: Mark fork if own uid(iter) doesn't match or if ANY uid(iter) in the layer doesn't match or compute forkedIters only after visiting layer?
      // val forkedIters = (bundledIters, bundledUID, bundledBase).zipped.collect{case (iters,uid0,uid1) if (uid.zip(uid1).exists{case (a,b) => a != b}) => iters}.flatten.toSeq
      val forkedIters = (iters, baseUID, uid).zipped.collect{case (iter,uid0,uid1) if uid0 != uid1 => iter}.toSeq
      // 2) For each layer, determine iterOfs based on the uids of this layer and forking of parent layers
      while (layer < bundledIters.size && !foundRandomLayer) {
        val ctrl = controlChain(layer)
        val liters = bundledIters(layer)
        val luid = bundledUID(layer)
        val lbase = bundledBase(layer)
        // Mark forked flags when we reach first fork, meaning every descendent from now on comes from different ancestry
        if (luid != lbase && !forked) {forked = true; firstFork = true}
        // val forkedIters = (bundledIters.take(layer), bundledUID.take(layer), bundledBase.take(layer)).zipped.collect{case (iters,uid0,uid1) if (uid.zip(uid1).exists{case (a,b) => a != b}) => iters}.flatten.toSeq

        // 2.1) If at first fork, mark iterators for layer appropriately and decide if entire subtrees are synchronized or not
        if (firstFork) {
          // 3.1) If first fork occurs at inner controller (including itersRed of OpMemReduce), then iterators are always synchronized
          if (ctrl.isInnerControl || liters.forall(ctrl.memReduceItersRed.contains)) {
            liters.foreach{iter => map += (iter -> Some(0))}
          }
          // 3.2) If forked POM, check synchronization for ALL children (up to next layer's ctrl if not a looping controller).  I.e. Set forkpoint at this ctrl
          else if (ctrl.isOuterControl && ctrl.willUnrollAsPOM) {
            val stopAtChild = if (layer + 1 < controlChain.size && (op.isDefined && !op.get.isLoop)) Some(controlChain(layer+1)) else None
            if (ctrl.synchronizedStart(forkedIters, entry = true, stopAtChild = stopAtChild)) liters.foreach{iter => map += (iter -> iterOfs(iter, bundledIters.take(layer), bundledUID.take(layer), bundledBase.take(layer)))}
            else foundRandomLayer = true
          }
          // 3.3) If forked MOP, check synchronization for next layer's ctrl and ignore outermost iterator. I.e. Set forkpoint at next layer's ctrl
          else if (ctrl.isOuterControl && ctrl.willUnrollAsMOP) {
            if ((layer + 1 < controlChain.size) && controlChain(layer+1).synchronizedStart(forkedIters ++ liters, entry = true)) liters.foreach{ iter => map += (iter -> iterOfs(iter, bundledIters.take(layer), bundledUID.take(layer), bundledBase.take(layer)))}
            else foundRandomLayer = true
          }
        } else if (forked) {
          // 3.5) Otherwise assume we can mark synchronization because firstFork determined we are still synchronized
          liters.foreach{iter => map += (iter -> iterOfs(iter, bundledIters.take(layer), bundledUID.take(layer), bundledBase.take(layer)))}
        } else {
          liters.foreach{iter => map += (iter -> Some(0))}
        }
        firstFork = false
        layer = layer + 1
      }
      // dbgs(s"itersynch info for $leaf $iters $baseUID $uid = $map")
      map.collect{case (i, o) if o.isDefined => (i -> o.get)}.toMap
    }

    /** Computes the ofs between iter from uid to baseline if applicable, due to iter's cchain having a start value that depends on uid divergence */
    @stateful def iterOfs(iter: Idx, itersAbove: Seq[Seq[Idx]], uid: Seq[Seq[Int]], base: Seq[Seq[Int]]): Option[Int] = {
      import spatial.util.modeling._
      val startSym = iter.ctrStart
      val stepSym = iter.ctrStep
      startSym match {
        case Op(LaneStatic(dep, elems)) => // If LaneStatic, just detect difference between startSyms
          val position = itersAbove.flatten.indexOf(dep)
          val upper = elems(uid.flatten.apply(position))
          val lower = elems(base.flatten.apply(position))
          Some(upper - lower)
        case _ => // Otherwise, check if startSym or stepSym mutates with any diverging iters
          val startDivergingLayer = itersAbove.zipWithIndex.collectFirst{case (is,j) if is.intersect(mutatingBounds(startSym)).nonEmpty => j}
          val stepDivergingLayer = itersAbove.zipWithIndex.collectFirst{case (is,j) if is.intersect(mutatingBounds(stepSym)).nonEmpty => j}
          val startDiverges = if (startDivergingLayer.isDefined) uid(startDivergingLayer.get) != base(startDivergingLayer.get) else false
          val stepDiverges = if (stepDivergingLayer.isDefined) uid(stepDivergingLayer.get) != base(stepDivergingLayer.get) else false
          if (startDiverges || stepDiverges) None else Some(0)
      }
    }

    /** Returns true if the subtree rooted at ctrl run for the same number of cycles (i.e. iterations) regardless of uid.
      * "entry" flag identifies whether the outermost iterator of the cchain should be ignored or not
      * "entry" indicates whether the binding Parallel controller is placed as a child of the parallelized LCA or a parent of the parallelized LCA
      */
    @stateful def synchronizedStart(forkedIters: Seq[Idx], entry: Boolean = false, stopAtChild: Option[Sym[_]] = None): Boolean = {
      val meSynch = cchainIsInvariant(forkedIters, entry)
      val childrenTodo = if (stopAtChild.isDefined) childrenPriorTo(Ctrl.Node(stopAtChild.get,-1)) else children
      val childrenSynch = childrenTodo.filter(_.s.get != s.get).forall(_.synchronizedStart(forkedIters))
      meSynch && childrenSynch
    }
    /** Returns true if this counterchain is invariant with iterators above it that diverge */
    @stateful def cchainIsInvariant(forkedIters: Seq[Idx], entry: Boolean): Boolean = {
      import spatial.util.modeling._
      if (isFSM || isStreamControl) false
      else if (isSwitch && parent.s.get.isOuterControl) { // If this is a switch serving as a controller (i.e. not a dataflow primitive)
        val conditions = s.get match { case Op(Switch(conds,_)) => conds; case _ => Seq() }
        val condMutators = conditions.flatMap(mutatingBounds(_))
        condMutators.intersect(forkedIters).isEmpty
      } else {
        val ctrsToDrop = if (entry) 1 else 0
        cchains.forall{cchain =>
          cchain.counters.drop(ctrsToDrop).forall{ctr => ctr.isFixed(forkedIters)}
        }
      }
    }

    @stateful def nestedWrittenDRAMs: Set[Sym[_]] = {
      s.flatMap{sym => Some((Seq(sym.toCtrl) ++ sym.nestedChildren).flatMap(_.writtenDRAMs).toSet) }.getOrElse(Set.empty)
    }
    def writtenDRAMs: Set[Sym[_]] = {
      s.flatMap{sym => metadata[WrittenDRAMs](sym).map(_.drams) }.getOrElse(Set.empty)
    }
    def writtenDRAMs_=(drams: Set[Sym[_]]): Unit = {
      s.foreach{sym => metadata.add(sym, WrittenDRAMs(drams)) }
    }

    @stateful def nestedReadDRAMs: Set[Sym[_]] = {
      s.flatMap{sym => Some((Seq(sym.toCtrl) ++ sym.nestedChildren).flatMap(_.readDRAMs).toSet) }.getOrElse(Set.empty)
    }
    def readDRAMs: Set[Sym[_]] = {
      s.flatMap{sym => metadata[ReadDRAMs](sym).map(_.drams) }.getOrElse(Set.empty)
    }
    def readDRAMs_=(drams: Set[Sym[_]]): Unit = {
      s.foreach{sym => metadata.add(sym, ReadDRAMs(drams)) }
    }

    @stateful def nestedWrittenMems: Set[Sym[_]] = {
      s.flatMap{sym => Some((Seq(sym.toCtrl) ++ sym.nestedChildren).flatMap(_.writtenMems).toSet) }.getOrElse(Set.empty)
    }
    def writtenMems: Set[Sym[_]] = {
      s.flatMap{sym => metadata[WrittenMems](sym).map(_.mems) }.getOrElse(Set.empty)
    }
    def writtenMems_=(mems: Set[Sym[_]]): Unit = {
      s.foreach{sym => metadata.add(sym, WrittenMems(mems)) }
    }

    @stateful def nestedReadMems: Set[Sym[_]] = {
      s.flatMap{sym => Some((Seq(sym.toCtrl) ++ sym.nestedChildren).flatMap(_.readMems).toSet) }.getOrElse(Set.empty)
    }
    def readMems: Set[Sym[_]] = {
      s.flatMap{sym => metadata[ReadMems](sym).map(_.mems) }.getOrElse(Set.empty)
    }
    def readMems_=(mems: Set[Sym[_]]): Unit = {
      s.foreach{sym => metadata.add(sym, ReadMems(mems)) }
    }

    @stateful def nestedTransientReadMems: Set[Sym[_]] = {
      s.flatMap{sym => Some((Seq(sym.toCtrl) ++ sym.nestedChildren).flatMap(_.transientReadMems).toSet) }.getOrElse(Set.empty)
    }
    def transientReadMems: Set[Sym[_]] = {
      s.flatMap{sym => metadata[TransientReadMems](sym).map(_.mems) }.getOrElse(Set.empty)
    }
    def transientReadMems_=(mems: Set[Sym[_]]): Unit = {
      s.foreach{sym => metadata.add(sym, TransientReadMems(mems)) }
    }

    def shouldNotBind: Boolean = s.flatMap{sym => metadata[ShouldNotBind](sym).map(_.f) }.getOrElse(false)
    def shouldNotBind_=(f: Boolean): Unit = s.foreach{sym => metadata.add(sym, ShouldNotBind(f))}
    def haltIfStarved: Boolean = s.flatMap{sym => metadata[HaltIfStarved](sym).map(_.f) }.getOrElse(false)
    def haltIfStarved_=(f: Boolean): Unit = s.foreach{sym => metadata.add(sym, HaltIfStarved(f))}

    def shouldConvertToStreamed: Option[Boolean] = s.flatMap{sym => metadata[ConvertToStreamed](sym).map(_.flag)}
    def shouldConvertToStreamed_=(f: Boolean): Unit = s.foreach{sym => metadata.add(sym, ConvertToStreamed(f))}

    def getLoweredTransfer: Option[TransferType] = {
      s.flatMap{sym => metadata[LoweredTransfer](sym).map(_.typ) }
    }
    def loweredTransfer: TransferType = {
      s.flatMap{sym => metadata[LoweredTransfer](sym).map(_.typ) }.head
    }
    def loweredTransfer_=(typ: TransferType): Unit = {
      s.foreach{sym => metadata.add(sym, LoweredTransfer(typ)) }
    }

    def getUnrollDirective: Option[UnrollStyle] = {
      s.flatMap{sym =>
        val pom: Option[Boolean] = metadata[UnrollAsPOM](sym).map(_.should)
        val mop: Option[Boolean] = metadata[UnrollAsMOP](sym).map(_.should)
        if (pom.isDefined) Some(ParallelOfMetapipes) else if (mop.isDefined) Some(MetapipeOfParallels) else None
      }
    }
    def unrollDirective: UnrollStyle = getUnrollDirective.get
    @stateful def willUnrollAsPOM: Boolean = {getUnrollDirective.contains(ParallelOfMetapipes) || (!getUnrollDirective.contains(MetapipeOfParallels) && spatialConfig.unrollParallelOfMetapipes)} && willUnroll && !isAnyReduce
    @stateful def willUnrollAsMOP: Boolean = {getUnrollDirective.contains(MetapipeOfParallels) || (!getUnrollDirective.contains(ParallelOfMetapipes) && spatialConfig.unrollMetapipeOfParallels)} && willUnroll
    def unrollAsPOM(): Unit = {
      s.foreach{sym => metadata.add(sym, UnrollAsPOM(true)) }
    }
    def unrollAsMOP(): Unit = {
      s.foreach{sym => metadata.add(sym, UnrollAsMOP(true)) }
    }
    @stateful def willUnroll: Boolean = cchains.exists(_.willUnroll) && isOuterControl

    def getLoweredTransferSize: Option[(Sym[_], Sym[_], Int)] = {
      s.flatMap{sym => metadata[LoweredTransferSize](sym).map(_.info) }
    }
    def loweredTransferSize: (Sym[_], Sym[_], Int) = {
      s.flatMap{sym => metadata[LoweredTransferSize](sym).map(_.info) }.head
    }
    def loweredTransferSize_=(info: (Sym[_], Sym[_], Int)): Unit = {
      s.foreach{sym => metadata.add(sym, LoweredTransferSize(info)) }
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
    @stateful def hasStreamAncestor: Boolean = ancestors.exists(_.isStreamControl)
    /** True if this controller or symbol has a streaming controller parent. */
    @stateful def hasStreamParent: Boolean = toCtrl.parent.isStreamControl

    @stateful def isInBlackboxImpl: Boolean = ancestors.exists(_.s.exists(_.isBlackboxImpl))

    /** True if this controller or symbol has an ancestor which runs forever. */
    def hasForeverAncestor: Boolean = ancestors.exists(_.isForever)

    /** True if this is an inner controller which directly contains
      * stream enablers/holder accesses.
      */
    @stateful def hasStreamAccess: Boolean = isInnerControl && (op match {
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

    @stateful def approxIters: Int = (cchains map {_.approxIters}).product

    @stateful def isForeach: Boolean = op match {
      case Some(_: OpForeach) => true
      case _ => false
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
    def toCtrl: Ctrl = if (s.isControl) Ctrl.Node(s,-1) else if (s.isBlackboxImpl) Ctrl.SpatialBlackbox(s) else s.parent
    def toScope: Scope = if (s.isControl) Scope.Node(s,-1,-1) else if (s.isBlackboxImpl) Scope.SpatialBlackbox(s) else s.scope
    def isControl: Boolean = s.op.exists(_.isControl)
    def stopWhen: Option[Sym[_]] = if (s.isControl) Ctrl.Node(s,-1).stopWhen else None

    @stateful def children: Seq[Ctrl.Node] = {
      if (s.isControl || s.isCtrlBlackbox) toCtrl.children
      else throw new Exception(s"Cannot get children of non-controller ${stm(s)}")
    }

    @stateful def nestedChildren: Seq[Ctrl.Node] = {
      if (s.isControl) toCtrl.nestedChildren
      else throw new Exception(s"Cannot get nestedChildren of non-controller ${stm(s)}")
    }

    @stateful def siblings: Seq[Ctrl.Node] = {
      if (s.isControl) parent.children
      else throw new Exception(s"Cannot get siblings of non-controller ${stm(s)}")
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

    @stateful def getRawSchedule: Option[CtrlSchedule] = if (state.scratchpad[ControlSchedule](s).isDefined) state.scratchpad[ControlSchedule](s).map(_.sched) else metadata[ControlSchedule](s).map(_.sched)
    @stateful def rawSchedule: CtrlSchedule = getRawSchedule.getOrElse{ throw new Exception(s"Undefined schedule for $s") }
    @stateful def rawSchedule_=(sched: CtrlSchedule): Unit = state.scratchpad.add(s, ControlSchedule(sched))
    @stateful def finalizeRawSchedule(sched: CtrlSchedule): Unit = metadata.add(s, ControlSchedule(sched))

    @stateful def isRawSeq: Boolean = rawSchedule == Sequenced
    @stateful def isRawPipe: Boolean = rawSchedule == Pipelined
    @stateful def isRawStream: Boolean = rawSchedule == Streaming
    @stateful def isForkJoin: Boolean = rawSchedule == ForkJoin
    @stateful def isFork: Boolean = rawSchedule == Fork

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
      if (!s.isControl && !s.isCtrlBlackbox) throw new Exception(s"Cannot get children of non-controller.")
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

    def compilerII: Double = metadata[CompilerII](s).map(_.interval).getOrElse(1.0)
    def compilerII_=(interval: Double): Unit = metadata.add(s, CompilerII(interval))

    def unrollBy: Option[Int] = metadata[UnrollBy](s).map(_.par)
    def unrollBy_=(par: Int): Unit = metadata.add(s, UnrollBy(par))

    def progorder: Option[Int] = metadata[ProgramOrder](s).map(_.id)
    def progorder_=(id: Int): Unit = metadata.add(s, ProgramOrder(id))
  }

  implicit class ScopeOperations(scp: Scope) extends ScopeHierarchyOps(scp.s) {
    def toCtrl: Ctrl = scp match {
      case Scope.Node(sym,id,_) => Ctrl.Node(sym,id)
      case Scope.Host           => Ctrl.Host
      case Scope.SpatialBlackbox(sym) => Ctrl.SpatialBlackbox(sym)
    }
    def toScope: Scope = scp
    def isControl: Boolean = true

    @stateful def children: Seq[Ctrl.Node] = toCtrl.children
    @stateful def nestedChildren: Seq[Ctrl.Node] = toCtrl.nestedChildren
    @stateful def siblings: Seq[Ctrl.Node] = toCtrl.siblings
    def parent: Ctrl = toCtrl.parent
    def scope: Scope = scp

    def iters: Seq[I32] = Try(scp match {
      case Scope.Host => Nil
      case Scope.SpatialBlackbox(sym) => Nil
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

        case Op(_: IfThenElse[_]) => sym.rawChildren // Fixme?

        case _ => throw new Exception(s"Cannot get children of non-controller.")
      }
      // Subcontroller case - return all children which have this subcontroller as an owner
      case Ctrl.Node(sym,_) => sym.rawChildren.filter(_.scope.toCtrl == ctrl)

      // The children of the host controller is all Accel scopes in the program
      case Ctrl.Host => AccelScopes.all

      case Ctrl.SpatialBlackbox(sym) => sym match {
        case Op(prim: SpatialBlackboxImpl[_,_]) => Seq()
        case Op(ctrl: SpatialCtrlBlackboxImpl[_,_]) => sym.rawChildren
      }
    }

    @stateful def nestedChildren: Seq[Ctrl.Node] = ctrl match {
      // "Master" controller case
      case Ctrl.Node(sym, -1) => sym match {
        case Op(ctrl: Control[_]) => ctrl.bodies.zipWithIndex.flatMap{case (body,id) =>
          val stage = Ctrl.Node(sym,id)
          // If this is a pseudostage, transparently return all controllers under this pseudostage
          if (body.isPseudoStage) sym.rawChildren.filter(_.scope.toCtrl == stage) ++ sym.rawChildren.filter(_.scope.toCtrl == stage).flatMap(_.nestedChildren)
          // Otherwise return the subcontroller for this "future" stage
          else Seq(Ctrl.Node(sym, id))
        }

        case Op(_: IfThenElse[_]) => sym.rawChildren ++ sym.rawChildren.flatMap(_.nestedChildren) // Fixme?

        case _ => throw new Exception(s"Cannot get children of non-controller.")
      }
      // Subcontroller case - return all children which have this subcontroller as an owner
      case Ctrl.Node(sym,_) => sym.rawChildren.filter(_.scope.toCtrl == ctrl) ++ sym.rawChildren.filter(_.scope.toCtrl == ctrl).flatMap(_.nestedChildren)

      // The children of the host controller is all Accel scopes in the program
      case Ctrl.Host => AccelScopes.all
      case Ctrl.SpatialBlackbox(s) => throw new Exception(s"Not sure how to give nested children for $s yet")
    }

    @stateful def siblings: Seq[Ctrl.Node] = parent.children

    def parent: Ctrl = ctrl match {
      case Ctrl.Node(sym,-1) => sym.parent
      case Ctrl.Node(sym, _) => Ctrl.Node(sym, -1)
      case Ctrl.SpatialBlackbox(sym) => Ctrl.SpatialBlackbox(sym)
      case Ctrl.Host => Ctrl.Host
    }

    def scope: Scope = ctrl match {
      case Ctrl.Node(sym,-1) => sym.scope
      case Ctrl.Node(sym, _) => Scope.Node(sym, -1, -1)
      case Ctrl.SpatialBlackbox(sym) => Scope.SpatialBlackbox(sym)
      case Ctrl.Host         => Scope.Host
    }

    def stopWhen: Option[Sym[_]] = ctrl match {
      case Ctrl.Node(Op(x:UnrolledForeach), _) => x.stopWhen
      case Ctrl.Node(Op(x:OpForeach), _) => x.stopWhen
      case Ctrl.Node(Op(x:UnrolledReduce), _) => x.stopWhen
      case Ctrl.Node(Op(x:OpReduce[_]), _) => x.stopWhen
      case Ctrl.Node(Op(x:OpMemReduce[_,_]), _) => x.stopWhen
      case Ctrl.Node(Op(x:UnitPipe), _) => x.stopWhen
      case _ => None
    }
  }

  implicit class BlkOps(blk: Blk) {
    def toScope: Scope = blk match {
      case Blk.Host      => Scope.Host
      case Blk.SpatialBlackbox(s)      => Scope.SpatialBlackbox(s)
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

    @stateful def counters: Seq[Counter[_]] = x.node.counters
    @stateful def pars: Seq[I32] = counters.map(_.ctrPar)
    @stateful def parsOr1: Seq[Int] = counters.map(_.ctrParOr1)
    @rig def widths: Seq[Int] = counters.map(_.ctrWidth)
    @stateful def constPars: Seq[Int] = pars.map(_.toInt)
    @stateful def willFullyUnroll: Boolean = counters.forall(_.willFullyUnroll)
    @stateful def willUnroll: Boolean = parsOr1.exists(_ > 1)
    @stateful def isUnit: Boolean = counters.forall(_.isUnit)
    @stateful def isStatic: Boolean = counters.forall(_.isStatic)
    @stateful def approxIters: Int = (counters map {
      _.nIters match {
        case Some(bound) =>
          bound.toInt
      }
    }).product
  }


  implicit class CounterHelperOps[F](x: Counter[F]) {
    def node: CounterNew[F] = x match {
      case Op(c: CounterNew[_]) => c.asInstanceOf[CounterNew[F]]
      case _ => throw new Exception(s"Could not find counter definition for $x")
    }

    @stateful def start: Sym[F] = if (x.isForever) I32(0).asInstanceOf[Sym[F]] else x.node.start
    @stateful def step: Sym[F] = if (x.isForever) I32(1).asInstanceOf[Sym[F]] else x.node.step
    @stateful def end: Sym[F] = if (x.isForever) boundVar[I32].asInstanceOf[Sym[F]] else x.node.end
    @stateful def ctrPar: I32 = if (x.isForever) I32(1) else x.node.par
    @stateful def ctrParOr1: Int = ctrPar.toInt
    @rig def ctrWidth: Int = if (x.isForever) 32 else x.node.A.nbits
    @stateful def isStatic: Boolean = (start,step,end) match {
      case (Final(_), Final(_), Final(_)) => true
      case _ => false
    }
    @stateful def isStaticStartAndStep: Boolean = {
      if (x.isForever) true
      else {
        (start,step) match {
          case (Final(_: scala.Int), Final(_: scala.Int)) => true
          case _ => false
        }
      }
    }
    /** Returns true if this counter runs for the same number of cycles regardless of uid of forkedIters */
    @stateful def isFixed(forkedIters: Seq[Idx]): Boolean = {
      import spatial.util.modeling._
      val startDeps = mutatingBounds(start)
      val endDeps = mutatingBounds(end)
      val stepDeps = mutatingBounds(step)
      val allDeps = startDeps ++ endDeps ++ stepDeps
      nIters match {
        case Some(Expect(_)) => !forkedIters.exists(allDeps.contains)
        case _ =>
          val startFixed = !forkedIters.exists(startDeps.contains) // start match {case Expect(_) => true; case x if x.isArgInRead => true; case _ => false} //!forkedIters.exists(startDeps.contains)
          val stepFixed = !forkedIters.exists(stepDeps.contains) // step match {case Expect(_) => true; case x if x.isArgInRead => true; case _ => false} //!forkedIters.exists(stepDeps.contains)
          val endFixed = !forkedIters.exists(endDeps.contains) // end match {case Expect(_) => true; case x if x.isArgInRead => true; case _ => false} //!forkedIters.exists(endDeps.contains)
          val distFixed = (start.asInstanceOf[Sym[_]],end.asInstanceOf[Sym[_]]) match {
            case (Op(FixAdd(_,a)),Op(FixAdd(_,b))) if a == b => true
            case (Op(FixSub(_,a)),Op(FixSub(_,b))) if a == b => true
            case _ => false
          }
          startFixed && (distFixed || (stepFixed && endFixed))
      }
    }

    @stateful def nIters: Option[Bound] = (start,step,end) match {
      case (Final(min), Final(stride), Final(max)) =>
        Some(Final(Math.ceil((max - min).toDouble / stride).toInt))

      case (Expect(min), Expect(stride), Expect(max)) =>
        Some(Expect(Math.ceil((max - min).toDouble / stride).toInt))

      case _ => None
    }
    @stateful def willFullyUnroll: Boolean = {
      if (x.isForever) false
      else (nIters,ctrPar) match {
        case (Some(Final(nIter)), Final(par)) => par >= nIter
        case _ => false
      }
    }
    @stateful def isUnit: Boolean = {
      if (x.isForever) false
      else nIters match {
        case Some(Final(1)) => true
        case _ => false
      }
    }

    def iter: Option[Sym[_]] = metadata[IterInfo](x).map(_.iter)
  }

  implicit class IndexHelperOps[W](i: Ind[W]) {
    @stateful def ctrStart: Ind[W] = i.counter.ctr.start.unbox
    @stateful def ctrStep: Ind[W] = i.counter.ctr.step.unbox
    @stateful def ctrEnd: Ind[W] = i.counter.ctr.end.unbox
    @rig def ctrPar: I32 = if (i.counter.ctr.isForever) I32(1) else i.counter.ctr.ctrPar
    @stateful def ctrParOr1: Int = if (i.counter.ctr.isForever) 1 else i.getCounter.map(_.ctr.ctrPar.toInt).getOrElse(1)
  }

  implicit class IndexCounterOps[A](i: Num[A]) {
    def getCounter: Option[IndexCounterInfo[A]] = metadata[IndexCounter](i).map(_.info.asInstanceOf[IndexCounterInfo[A]])
    def counter: IndexCounterInfo[A] = getCounter.getOrElse{throw new Exception(s"No counter associated with $i") }
    def counter_=(info: IndexCounterInfo[_]): Unit = {
      metadata.add(i, IndexCounter(info))
      metadata.add(info.ctr, IterInfo(i))
    }
  }

  implicit class BitsCounterOps(i: Bits[_]) {
    def getCounter: Option[IndexCounterInfo[_]] = metadata[IndexCounter](i).map(_.info)
    def counter: IndexCounterInfo[_] = getCounter.getOrElse{throw new Exception(s"No counter associated with $i") }
    def counter_=(info: IndexCounterInfo[_]): Unit = {
      metadata.add(i, IndexCounter(info))
      metadata.add(info.ctr, IterInfo(i))
    }
  }

  /** True if the given symbol is allowed to be defined on the Host and used in Accel
    * This is true for the following types:
    *   - Global (shared) memories (e.g. DRAM, ArgIn, ArgOut, HostIO, StreamIn, StreamOut)
    *   - Bit-based types (if ArgIn inference is allowed)
    *   - Text (staged strings) (TODO: only allowed in debug mode?)
    */
  def allowSharingBetweenHostAndAccel(s: Sym[_], allowArgInference: Boolean): Boolean = {
    s.isRemoteMem || s.isBlackboxImpl ||
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
      // logs(s"  PathA: " + pathA.mkString(", "))
      // logs(s"  PathB: " + pathB.mkString(", "))
      // logs(s"  Ctrl: $ctrl")
      // logs(s"  ctrlIdxA: $ctrlIdxA")
      // logs(s"  ctrlIdxB: $ctrlIdxB")

      if (ctrlIdxA < 0 || ctrlIdxB < 0) None        // ctrl is not common to a and b
      else if (ctrlIdxA >= pathA.length - 1) None   // implies ctrl == a
      else if (ctrlIdxB >= pathB.length - 1) None   // implies ctrl == b
      else {
        // TODO[2]: Revise to account for arbitrary dataflow graphs for children controllers
        val topA = pathA(ctrlIdxA + 1)
        val topB = pathB(ctrlIdxB + 1)
        val idxA = ctrl.children.indexOf(topA)
        val idxB = ctrl.children.indexOf(topB)
        // logs(s"  A: $a, B: $b")
        // logs(s"  ${ctrl.children.mkString(" ")}")
        // logs(s"  CtrlA: $topA ($idxA), CtrlB: $topB ($idxB)")
        // logs(s"  Dist = ${idxB - idxA}")
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
    if (ctrl.isOuterPipeLoop) dist else None
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
      val hierIssue = if (hierarchicalBuffer) Some(AmbiguousMetaPipes(mem, metapipeLCAs)) else None

      metapipeLCAs.keys.headOption match {
        case Some(metapipe) =>
          val group: Set[(Sym[_],Sym[_])] = metapipeLCAs(metapipe)
          val anchor: Sym[_] = group.head._1
          val dists = accesses.map{a =>
            val dist = getCoarseDistance(metapipe, anchor, a)
            dbgs(s"$a <-> $anchor # LCA: $metapipe, Dist: $dist")

            // As long as a is in the metapipeLCAs map, or is a reader that was unmapped because its LCA with writer is inner control, assign its dist
            if (group.exists{x => a.isReader || a == x._1 || a == x._2 }) a -> dist else a -> None
          }
          val buffers = dists.filter{_._2.isDefined}.map(_._2.get)
          val minDist = buffers.minOrElse(0)
          val ports = dists.map{case (a,dist) => a -> dist.map{d => d - minDist} }.toMap
          val bufferHazards = ports.toList.collect{case (a, i) if a.isWriter && i.getOrElse(0) > 0 => (a,i.getOrElse(0))}
          val bufIssue = if (bufferHazards.nonEmpty && !mem.isWriteBuffer && !mem.isNonBuffer) Some(PotentialBufferHazard(mem, bufferHazards)) else None
          val issue = if (hierIssue.isDefined) hierIssue else bufIssue

          (Some(metapipe), ports, issue)

        case None =>
          (None, accesses.map{a => a -> Some(0)}.toMap, hierIssue)
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
        case Ctrl.SpatialBlackbox(s) => "  "*tab + s"${shortStm(s)} [Blackbox]"
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
    LocalMemories.all.filter{mem => mem.readers.exists{ x =>
          x.parent.s == ctrl.s && { x match { case Op(_:FIFOBankedPriorityDeq[_]) => false; case _ => true } }
      }}
      .filter{mem => mem.isStreamIn || mem.isFIFO || mem.isMergeBuffer || mem.isFIFOReg || mem.isCtrlBlackbox || mem.isInstanceOf[StreamStruct[_]]}
  }

   // Return inbound streams that are consumed by priority deq, grouped together by the priority deq they are part of
    @stateful def getReadPriorityStreams(ctrl: Ctrl): Set[Set[Sym[_]]] = {
      val inboundFifos = LocalMemories.all.filter { mem =>
        mem.readers.exists { x =>
          x.parent.s == ctrl.s && { x match { case Op(_:FIFOBankedPriorityDeq[_]) => true; case _ => false } }
        }
      }
      inboundFifos.groupBy { f => f.readers.collectFirst { case x@Op(_: FIFOBankedPriorityDeq[_]) => x.prDeqGrp.get } }.values.toSet
    }

  @stateful def getWriteStreams(ctrl: Ctrl): Set[Sym[_]] = {
    LocalMemories.all.filter{mem => mem.writers.exists{c => c.parent.s == ctrl.s }}
      .filter{mem => mem.isStreamOut || mem.isFIFO || mem.isMergeBuffer || mem.isFIFOReg || mem.isCtrlBlackbox}
  }

  @stateful def getUsedFields(bbox: Sym[_], ctrl: Ctrl): Seq[String] = {
    ctrl.s.get.blocks.flatMap(_.nestedStms.flatMap{case x@Op(FieldDeq(ss, field, _)) if ss == bbox => Some(field); case _ => None})
  }
}
