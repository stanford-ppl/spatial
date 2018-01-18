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

  }

  @flow def controlLevel(s: Sym[_], op: Op[_]): Unit = if (isControl(s)) {
    val children = op.blocks.flatMap(_.stms.filter(isControl))
    isOuter(s) = children.nonEmpty || isAccel(s)
    childrenOf(s) = children
    op.blocks.zipWithIndex.foreach{case (blk,id) => blk.stms.foreach{s => parentOf(s) = Ctrl(s,id) }}
  }

}

