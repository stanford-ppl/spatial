package spatial.data

import forge.tags._
import core._
import spatial.data._
import spatial.node._
import spatial.util._

trait FlowRules {
  @flow def consumers(a: Sym[_], op: Op[_]): Unit = {
    a.inputs.foreach{in => consumersOf.add(in, a) }
  }

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

  @flow def controlLevel(s: Sym[_], op: Op[_]): Unit = if (isControl(s)) {
    val children = op.blocks.flatMap(_.stms.filter(isControl))
    isOuter(s) = children.exists{s => !isBranch(s) || isOuterControl(s) } || isAccel(s)
    childrenOf(s) = children
    op.blocks.zipWithIndex.foreach{case (blk,id) =>
      blk.stms.foreach{lhs => parentOf(lhs) = Ctrl(s,id) }
    }
  }

  @flow def loopIterators(s: Sym[_], op: Op[_]): Unit = op match {
    case loop: Loop[_] =>
      loop.cchains.foreach{case (cchain,is) =>
        cchain.ctrs.zip(is).foreach{case (ctr, i) => ctrOf(i) = ctr }
      }

    case _ =>
  }

}

