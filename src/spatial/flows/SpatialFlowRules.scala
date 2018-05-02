package spatial.flows

import argon._
import forge.tags._
import spatial.data._
import spatial.node._
import spatial.util._

case class SpatialFlowRules(IR: State) extends FlowRules {
  @flow def memories(a: Sym[_], op: Op[_]): Unit = a match {
    case MemAlloc(mem) if mem.isLocalMem => localMems += mem
    case _ =>
  }

  @flow def accesses(s: Sym[_], op: Op[_]): Unit = op match {
    case Accessor(wr,rd) =>
      wr.foreach{w => w.mem.writers += s }
      rd.foreach{r => r.mem.readers += s }

    case BankedAccessor(wr,rd) =>
      wr.foreach{w => w.mem.writers += s }
      rd.foreach{r => r.mem.readers += s }

    case Resetter(mem,ens) => mem.resetters += s

    case _ =>
  }

  @flow def accumulator(s: Sym[_], op: Op[_]): Unit = {
    if (s.isReader) s.readUses += s
    s.readUses ++= s.inputs.flatMap{in => in.readUses }

    s match {
      case Writer(wrMem,_,_,_) =>
        s.readUses.foreach{case Reader(rdMem,_,_) =>
          if (rdMem == wrMem) {
            rdMem.accumType = AccumType.Fold
            s.accumType = AccumType.Fold
          }
        }
      case _ =>
    }
  }

  @flow def controlLevel(s: Sym[_], op: Op[_]): Unit = op match {
    case ctrl: Control[_] =>
      val children = op.blocks.flatMap(_.stms.filter(_.isControl))
      // Branches (Switch, SwitchCase) only count as controllers here if they are outer controllers
      s.isOuter = children.exists{c => !c.isBranch || c.isOuter }
      s.cchains.foreach{cchain => cchain.owner = s }
      s.children = children.map{c => Controller(c,-1) }
      val bodies = ctrl.bodies
      op.blocks.foreach{blk =>
        val id = bodies.zipWithIndex.collectFirst{case (grp,i) if grp._2.contains(blk) => i }
                       .getOrElse{throw new Exception(s"Block $blk is not associated with an ID in control $ctrl")}
        blk.stms.foreach{lhs => lhs.parent = Controller(s,id) }
      }
      op.blocks.zipWithIndex.foreach{case (blk,bId) =>
        blk.stms.foreach{lhs => lhs.blk = Controller(s, bId) }
      }

    case _ => // Nothin'
  }

  /** Set the control schedule of controllers based on the following ordered rules:
    *   1. Parallel is always ForkJoin
    *   2. Switch is always Fork
    *   3. Sparse and dense transfer black boxes are always Pipe
    *   3. For all other controllers:
    *      a. If the compiler has not yet defined a schedule, take the user schedule if defined.
    *      b. If the user and compiler both have not defined a schedule:
    *         - UnitPipe and Accel are Seq by default
    *         - All other controllers are Pipe by default
    *      c. Otherwise, use the compiler's schedule
    *   4. Accel, UnitPipe, and fully unrolled loops cannot be Pipe - override these with Seq.
    *   5. Inner controllers cannot be Stream - override these with Pipe [TODO: Confirm]
    */
  @flow def controlSchedule(s: Sym[_], op: Op[_]): Unit = op match {
    case _: ParallelPipe         => s.schedule = Sched.ForkJoin
    case _: Switch[_]            => s.schedule = Sched.Fork
    case _: DenseTransfer[_,_,_] => s.schedule = Sched.Pipe
    case _: SparseTransfer[_,_]  => s.schedule = Sched.Pipe
    case _: Control[_] =>
      (s.getUserSchedule, s.getSchedule) match {
        case (Some(s1), None) => s.schedule = s1
        case (_   , Some(s2)) => s.schedule = s2
        case (None, None)     =>
          val default = if (s.isUnitPipe || s.isAccel) Sched.Seq else Sched.Pipe
          s.schedule = default
      }
      if (!s.isLoop && s.isPipeline) s.schedule = Sched.Seq
      if (s.isInnerControl && s.isStreamPipe) s.schedule = Sched.Pipe

      dbgs(s"Setting $s = $op to ${s.schedule} (user schedule: ${s.getUserSchedule.map(_.toString).getOrElse("<none>")})")

    case _ => // No schedule for non-control nodes
  }

  @flow def loopIterators(s: Sym[_], op: Op[_]): Unit = op match {
    case loop: Loop[_] =>
      loop.cchains.foreach{case (cchain,is) =>
        cchain.ctrs.zip(is).foreach{case (ctr, i) => i.counter = ctr }
      }

    case _ =>
  }

  @flow def streams(s: Sym[_], op: Op[_]): Unit = {
    if (s.isStreamLoad)   streamLoadCtrls += s
    if (s.isTileTransfer) tileTransferCtrls += s
    if (s.isParEnq)       streamParEnqs += s

    if (s.isStreamStageEnabler) streamEnablers += s
    if (s.isStreamStageHolder)  streamHolders += s
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
      if (rhs.inputs.nonEmpty && rhs.inputs.forall(_.isGlobal)) lhs.isGlobal = true

    case _ => // Not global
  }

}

