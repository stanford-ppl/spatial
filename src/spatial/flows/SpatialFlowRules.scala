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
      s.isOuter = children.exists{c => c.isBranch || c.isOuterControl }
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

  @flow def controlSchedule(s: Sym[_], op: Op[_]): Unit = op match {
    case _: ParallelPipe => s.schedule = Sched.ForkJoin
    case _: Switch[_]    => s.schedule = Sched.Fork
    case _: Control[_] =>
      (s.getUserSchedule, s.getSchedule) match {
        case (None, None) if s.isUnitPipe || s.isAccel => s.schedule = Sched.Seq
        case (None, None)        => s.schedule = Sched.Pipe
        case (Some(s1), None)    => s.schedule = s1
        case (None, Some(s1))    => s.schedule = s1
        case (Some(_), Some(s2)) => s.schedule = s2  // Override user
      }
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


  /**
    * In Spatial, a "global" is any value which is solely a function of input arguments
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

