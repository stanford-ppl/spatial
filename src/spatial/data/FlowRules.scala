package spatial.data

import forge.tags._
import argon._
import spatial.data._
import spatial.node._
import spatial.util._

trait FlowRules {
  @flow def memories(a: Sym[_], op: Op[_]): Unit = a match {
    case MemAlloc(mem) if mem.isLocalMem => localMems += mem
    case _ =>
  }

  @flow def accesses(s: Sym[_], op: Op[_]): Unit = op match {
    case Accessor(wr,rd) =>
      wr.foreach{w => writersOf(w.mem) = writersOf(w.mem) + s }
      rd.foreach{r => readersOf(r.mem) = readersOf(r.mem) + s }
    case _ =>
  }

  @flow def accumulator(s: Sym[_], op: Op[_]): Unit = {
    if (s.isReader) readUsesOf(s) = readUsesOf(s) + s
    readUsesOf(s) = readUsesOf(s) ++ s.inputs.flatMap{in => readUsesOf(in) }

    s match {
      case Writer(wrMem,_,_,_) =>
        val readers = readUsesOf(s)
        readers.foreach{case Reader(rdMem,_,_) =>
          if (rdMem == wrMem) {
            accumTypeOf(rdMem) = AccumType.Fold
            accumTypeOf(s) = AccumType.Fold
          }
        }
      case _ =>
    }
  }

  @flow def controlLevel(s: Sym[_], op: Op[_]): Unit = op match {
    case ctrl: Control[_] =>
      val children = op.blocks.flatMap(_.stms.filter(isControl))
      isOuter(s) = children.exists{s => !isBranch(s) || isOuterControl(s) } || isAccel(s)
      s.children = children.map{c => Parent(c,-1) }
      val bodies = ctrl.bodies
      op.blocks.foreach{blk =>
        val id = bodies.zipWithIndex.collectFirst{case (grp,i) if grp._2.contains(blk) => i }
                       .getOrElse{throw new Exception(s"Block $blk is not associated with an ID in control $ctrl")}
        blk.stms.foreach{lhs => lhs.parent = Parent(s,id) }
      }
    case _ => // Nothin'
  }

  @flow def loopIterators(s: Sym[_], op: Op[_]): Unit = op match {
    case loop: Loop[_] =>
      loop.cchains.foreach{case (cchain,is) =>
        cchain.ctrs.zip(is).foreach{case (ctr, i) => ctrOf(i) = ctr }
      }

    case _ =>
  }

}

