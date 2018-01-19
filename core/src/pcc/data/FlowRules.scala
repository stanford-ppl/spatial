package pcc.data

import forge._
import pcc.core._
import pcc.lang._
import pcc.node._

trait FlowRules {

  @flow def memories(a: Sym[_], op: Op[_]): Unit = a match {
    case Memory(mem) if mem.isLocalMem => localMems += mem
    case _ =>
  }

  @flow def accesses(s: Sym[_], op: Op[_]): Unit = op match {
    case Accessor(wrs,rds) =>
      wrs.foreach{wr => writersOf(wr.mem) += s }
      rds.foreach{rd => readersOf(rd.mem) += s }
    case _ =>
  }

  @flow def accumulator(s: Sym[_], op: Op[_]): Unit = {
    if (s.isReader) readUsesOf(s) += s
    readUsesOf(s) ++= s.dataInputs.flatMap{in => readUsesOf(in) }

    s match {
      case Writer(wrs) =>
        val readers = readUsesOf(s)
        val reads = readers.flatMap{case Reader(rds) => rds }
        wrs.foreach{wr =>
          reads.foreach{rd => if (rd.mem == wr.mem) {
            isAccum(rd.mem) = true
            isAccum(s) = true
          }}
        }
      case _ =>
    }
  }

  @flow def controlLevel(s: Sym[_], op: Op[_]): Unit = if (isControl(s)) {
    val children = op.blocks.flatMap(_.stms.filter(isControl))
    isOuter(s) = children.nonEmpty || isAccel(s)
    childrenOf(s) = children
    op.blocks.zipWithIndex.foreach{case (blk,id) =>
      blk.stms.foreach{lhs => parentOf(lhs) = Ctrl(s,id) }
    }
  }

}

