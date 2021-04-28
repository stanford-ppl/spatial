package spatial.flows

import argon._
import argon.node._
import forge.tags._
import spatial.metadata.bounds._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.node._

case class SpatialFlowRules(IR: State) extends FlowRules {
  @flow def memories(a: Sym[_], op: Op[_]): Unit = a match {
    case MemAlloc(mem) if mem.isLocalMem => LocalMemories += mem
    case MemAlloc(mem) if (mem.isRemoteMem || a.isStreamIn || a.isStreamOut) => RemoteMemories += a
    case _ =>
  }

  @flow def accesses(s: Sym[_], op: Op[_]): Unit = op match {
    case Accessor(wr,rd) =>
      wr.foreach{w => w.mem.writers += s; logs(s"  Writers of ${w.mem} is now: ${w.mem.writers}") }
      rd.foreach{r => r.mem.readers += s; logs(s"  Readers of ${r.mem} is now: ${r.mem.readers}") }

    case x:RMWDoer[_,_] =>
      x.mem.readers += s
      x.mem.writers += s

    case UnrolledAccessor(wr,rd) =>
      wr.foreach{w => w.mem.writers += s; logs(s"  Writers of ${w.mem} is now: ${w.mem.writers}") }
      rd.foreach{r => r.mem.readers += s; logs(s"  Readers of ${r.mem} is now: ${r.mem.readers}") }

    case Resetter(mem,ens) => mem.resetters += s

    case _ =>
  }

  @flow def accumulator(s: Sym[_], op: Op[_]): Unit = {
    if (s.isReader) s.readUses += s
    s.readUses ++= s.inputs.flatMap{in => in.readUses }

    def matchReader(rd: Sym[_], mem: Sym[_]): Unit = rd match {
      case Reader(rdMem,_,_) =>
        if (rdMem == mem) {
          mem.accumType = AccumType.Fold & mem.accumType
          s.accumType   = AccumType.Fold & s.accumType
        }
      case UnrolledReader(read) =>
        if (read.mem == mem) {
          mem.accumType = AccumType.Fold & mem.accumType
          s.accumType   = AccumType.Fold & s.accumType
        }
      case _ =>
    }

    s match {
      case Writer(mem,_,_,_)  => s.readUses.foreach{rd => matchReader(rd, mem) }
      case UnrolledWriter(wr) => s.readUses.foreach{rd => matchReader(rd, wr.mem) }
      case _ =>
    }
  }

  @flow def controlLevel(s: Sym[_], op: Op[_]): Unit = op match {
    case ctrl: IfThenElse[_] => 
      val children = op.blocks.flatMap(_.stms.filter(_.isControl))
      s.rawChildren = children.map{c => Ctrl.Node(c,-1)}

      val isOuter = children.exists{c => !c.isBranch || c.isOuterControl} || op.isMemReduce
      s.rawLevel = if (isOuter) Outer else Inner

    case ctrl: Control[_] =>
      // Find all children controllers within this controller
      val children = op.blocks.flatMap(_.stms.filter(_.isControl))
      s.rawChildren = children.map{c => Ctrl.Node(c,-1) }

      // Branches (Switch, SwitchCase) only count as controllers here if they are outer controllers
      // MemReduce is always an outer controller
      val isOuter = children.exists{c => !c.isBranch || c.isOuterControl } || op.isMemReduce
      s.rawLevel = if (isOuter) Outer else Inner

      val master: Ctrl  = Ctrl.Node(s, -1)
      val masterScope: Scope = Scope.Node(s, -1, -1)

      // Set metadata for counter owners
      s.cchains.foreach{cchain =>
        cchain.owner = s
        cchain.rawParent = master
        cchain.rawScope  = masterScope
        cchain.counters.foreach{ctr =>
          ctr.owner = s
          ctr.rawParent = master
          ctr.rawScope  = masterScope
        }
      }

      // Special cases for blocks with return values - should correspond to their outer use
      var specialCases: Set[Sym[_]] = Set.empty
      ctrl match {
        case node: OpReduce[_] if isOuter =>
          val result = node.map.result
          result.rawParent = Ctrl.Node(s, 1)
          result.rawScope  = Scope.Node(s, 1, 1)
          specialCases += result

        case _ =>
      }


      // Set scope and parent metadata for children controllers
      val bodies = ctrl.bodies

      op.blocks.foreach{block =>
        val (body,stageId) = bodies.zipWithIndex.collectFirst{case (stg,i) if stg.blocks.exists(_._2 == block) => (stg,i) }
                                    .getOrElse{throw new Exception(s"Block $block is not associated with an ID in control $ctrl")}

        // --- Ctrl Hierarchy --- //
        // Don't track pseudoscopes in the control hierarchy (use master controller instead)
        val control: Ctrl = Ctrl.Node(s, stageId)
        val parent: Ctrl  = if (body.isPseudoStage) master else control
        ctrl.iters.foreach{b => b.rawParent = parent}
        ctrl match {
          case ctrl:UnrolledLoop[_] => {
            ctrl.valids.foreach{b => b.rawParent = parent}
            ctrl.resets.foreach{b => b.rawParent = parent}
          }
          case _ =>
        }

        // --- Scope Hierarchy --- //
        // Always track all scopes in the scope hierarchy
        val blockId = body.blocks.indexWhere(_._2 == block)
        val scope: Scope = Scope.Node(s, stageId, blockId)

        // Iterate from last to first
        block.stms.reverse.foreach{lhs =>
          if (lhs.isCounter || lhs.isCounterChain || specialCases.contains(lhs)) {
            // Ignore
          }
          else if (lhs.isTransient) {
            val consumerParents = lhs.consumers.map{c => c.toCtrl }
            val nodeMaster = consumerParents.find{c => c != master && c != parent && c != Ctrl.Host }
            val nodeParent = nodeMaster.getOrElse(parent)
            val nodeScope  = nodeMaster.map{c => Scope.Node(c.s.get,-1,-1) }.getOrElse(scope)
            lhs.rawParent = nodeParent
            lhs.rawScope  = nodeScope
          }
          else {
            lhs.rawParent = parent
            lhs.rawScope  = scope
          }
        }
      }

      // --- Blk Hierarchy --- //
      // Set the reads and writes of each symbol defined in this controller
      op.blocks.zipWithIndex.foreach{case (block,bId) =>
        block.stms.foreach{lhs =>
          lhs match {
            case RMWDoer(mem, _, _, _, _, _, _) =>
              dbgs(s"Handle RMWDoer: $lhs mem: $mem")
              dbgs(s"\tPrev written: ${s.writtenMems}")
              dbgs(s"\tPrev read: ${s.readMems}")
              s.writtenMems += mem
              s.readMems += mem
              dbgs(s"\tPost written: ${s.writtenMems}")
              dbgs(s"\tPost read: ${s.readMems}")

            case Accessor(wr,rd) =>
              dbgs(s"Handle Accessor: $lhs")
              dbgs(s"\tPrev written: ${s.writtenMems}")
              dbgs(s"\tPrev read: ${s.readMems}")
              wr.foreach{w => s.writtenMems += w.mem }
              rd.foreach{r => s.readMems += r.mem }
              dbgs(s"\tPost written: ${s.writtenMems}")
              dbgs(s"\tPost read: ${s.readMems}")

            case BankedRMWDoer(mem, _, _, _, _, _, _, _) =>
              dbgs(s"Handle Banked RMWDoer: $lhs mem: $mem")
              dbgs(s"\tPrev written: ${s.writtenMems}")
              dbgs(s"\tPrev read: ${s.readMems}")
              s.writtenMems += mem
              s.readMems += mem
              dbgs(s"\tPost written: ${s.writtenMems}")
              dbgs(s"\tPost read: ${s.readMems}")

            case UnrolledAccessor(wr,rd) =>
              dbgs(s"Handle Unrolled Accessor: $lhs")
              dbgs(s"\tPrev written: ${s.writtenMems}")
              dbgs(s"\tPrev read: ${s.readMems}")
              wr.foreach{w => s.writtenMems += w.mem }
              rd.foreach{r => s.readMems += r.mem }
              dbgs(s"\tPost written: ${s.writtenMems}")
              dbgs(s"\tPost read: ${s.readMems}")

            case StatusReader(mem, _) => 
              s.readMems += mem

            case Op(_@FringeDenseStore(d,_,_,_)) =>
              s.writtenDRAMs += d.asInstanceOf[Sym[_]]
            case Op(_@FringeSparseStore(d,_,_)) =>
              s.writtenDRAMs += d.asInstanceOf[Sym[_]]
            case Op(_@FringeCoalStore(d,_,_,_,_)) =>
              s.writtenDRAMs += d.asInstanceOf[Sym[_]]
            case Op(_@FringeDynStore(d,_,_,_,_)) =>
              s.writtenDRAMs += d.asInstanceOf[Sym[_]]
            case Op(_@FringeStreamLoad(d,_,_,_,_,_)) =>
              s.readDRAMs += d.asInstanceOf[Sym[_]]
            case Op(_@DRAMAlloc(d,_)) => 
              s.writtenDRAMs += d.asInstanceOf[Sym[_]]
              s.readDRAMs += d.asInstanceOf[Sym[_]]

            case Op(_@FringeDenseLoad(d,_,_)) =>
              s.readDRAMs += d.asInstanceOf[Sym[_]]
            case Op(_@FringeSparseLoad(d,_,_)) =>
              s.readDRAMs += d.asInstanceOf[Sym[_]]
            case Op(_@DRAMDealloc(d)) => 
              s.readDRAMs += d.asInstanceOf[Sym[_]]
              s.writtenDRAMs += d.asInstanceOf[Sym[_]]

            case Op(_@(BreakpointIf(_) | ExitIf(_))) =>
              lhs.parent.s.get.shouldNotBind = true

            case _ =>
          }
        }
      }

    case _ =>
  }

  @flow def blockLevel(s: Sym[_], op: Op[_]): Unit = {
    // Set blk for nodes inside ctrl
    op.blocks.zipWithIndex.foreach{case (block,bId) =>
      block.stms.foreach{lhs =>
        lhs.blk = Blk.Node(s, bId)
      }
    }
    op.binds.filter(_.isBound).foreach{ b =>
      b.blk = Blk.Node(s, -1)
    }
  }

  /** Set the control schedule of controllers based on the following ordered rules:
    *   1. Parallel is always ForkJoin
    *   2. Switch is always Fork
    *   3. Sparse and dense transfer black boxes are always Pipe
    *   3. For all other controllers:
    *      a. If the compiler has not yet defined a schedule, take the user schedule if defined.
    *      b. If the user and compiler both have not defined a schedule:
    *         - UnitPipe and Accel are Sequenced by default
    *         - All other controllers are Pipelined by default
    *      c. Otherwise, use the compiler's schedule
    *   4. "Single" iteration control (Accel, UnitPipe, fully unrolled loops) cannot be Pipelined - override these with Sequenced.
    *   5. Inner controllers cannot be Streaming - override these with Pipelined
    *   6. Outer controllers with only one child cannot be Pipelined - override these to Sequenced
    */
  @flow def controlSchedule(s: Sym[_], op: Op[_]): Unit = op match {
    case _: ParallelPipe           => s.rawSchedule = ForkJoin
    case _: Switch[_]              => s.rawSchedule = Fork
    case _: IfThenElse[_]          => s.rawSchedule = Fork
    case _: SwitchCase[_]          => s.rawSchedule = Sequenced
    case _: DenseTransfer[_,_,_]   => s.rawSchedule = Pipelined
    case _: CoalesceStore[_,_,_]   => s.rawSchedule = Pipelined
    case _: StreamStore[_,_,_]   => s.rawSchedule = Pipelined
    case _: SparseTransfer[_,_]    => s.rawSchedule = Pipelined
    case ctrl: Control[_] =>
      logs(s"Determining schedule of $s = $op")
      logs(s"  User Schedule:    ${s.getUserSchedule}")
      logs(s"  Raw Schedule:     ${s.getRawSchedule}")
      logs(s"  Control Level:    ${s.rawLevel}")

      (s.getUserSchedule, s.getRawSchedule) match {
        case (Some(s1), None) => s.rawSchedule = s1
        case (_   , Some(s2)) => s.rawSchedule = s2
        case (None, None)     =>
          val default = if (s.isUnitPipe || s.isAccel) Sequenced else Pipelined
          s.rawSchedule = default
      }

      logs(s"=>")
      logs(s"  Initial Schedule: ${s.rawSchedule}")
      logs(s"  Single Control:   ${s.isSingleControl}")
      logs(s"  # Children:       ${s.children.size}")
      logs(s"  # Childen (Ctrl): ${s.toCtrl.children.size}")
      val hasPrimitives = s.outerBlocks.exists(_._2.stms.exists(s => s.isPrimitive && !s.isTransient))
      val isSingleChildOuter = {
        s.isOuterControl && s.children.size == 1 && s.toCtrl.children.size == 1 && !hasPrimitives
      }

      if (s.isSingleControl && s.rawSchedule == Pipelined)  s.rawSchedule = Sequenced
      if (s.isInnerControl && s.rawSchedule == Streaming)   s.rawSchedule = Pipelined
      if (isSingleChildOuter && s.rawSchedule == Pipelined) s.rawSchedule = Sequenced
      if (s.isUnitPipe && s.rawSchedule == Fork) s.rawSchedule = Sequenced // Undo transfer of metadata copied from Switch in PipeInserter

      logs(s"  Final Schedule:   ${s.rawSchedule}")

    case _ => // No schedule for non-control nodes
  }

  // Now set in unroller and ForeachClass, ReduceClass, and MemReduceClass
  //@flow def loopIterators(s: Sym[_], op: Op[_]): Unit = op match {
    //case uloop: UnrolledLoop[_] => 
      //uloop.cchainss.foreach{case (cchain,is) =>
        //cchain.counters.zip(is).foreach{case (ctr, i) => i.zipWithIndex.foreach{case (it, j) => it.counter = IndexCounterInfo(ctr, Seq(j))} }
      //}
    //case loop: Loop[_] =>
      //loop.cchains.foreach{case (cchain,is) =>
        //cchain.counters.zip(is).foreach{case (ctr, i) => i.counter = IndexCounterInfo(ctr, Seq(0)) }
      //}

    //case _ =>
  //}

  @flow def streams(s: Sym[_], op: Op[_]): Unit = {
    if (s.isStreamLoad)   StreamLoads += s
    if (s.isTileTransfer) TileTransfers += s
    if (s.isParEnq)       StreamParEnqs += s

    if (s.isStreamStageEnabler) StreamEnablers += s
    if (s.isStreamStageHolder)  StreamHolders += s
  }


  /** In Spatial, a "global" is any value which is solely a function of input arguments
    * and constants. These are computed prior to starting the main computation, and
    * therefore appear constant to the majority of the program.
    *
    * Note that this is only true for stateless nodes. These rules should not be generated
    * for stateful hardware (e.g. accumulators, pseudo-random generators)
    **/
  @flow def globals(lhs: Sym[_], rhs: Op[_]): Unit = lhs match {
    case Impure(_,_) =>
    case Op(RegRead(reg)) if reg.isArgIn => lhs.isGlobal = true

    case Primitive(_) =>
      if (rhs.expInputs.nonEmpty && rhs.expInputs.forall(_.isGlobal)) lhs.isGlobal = true
      if (rhs.expInputs.nonEmpty && rhs.expInputs.forall(_.isFixedBits)) lhs.isFixedBits = true

    case _ => // Not global
  }

}
