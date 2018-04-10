package spatial.report

import argon._
import spatial.data._
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.util._

case class RetimeReporter(IR: State) extends AccelTraversal {
  override def shouldRun: Boolean = config.enInfo

  private def printBlocks(lhs: Sym[_], blocks: Seq[Block[_]]): Unit = blocks.zipWithIndex.foreach{case (blk,i) =>
    state.logTab += 1
    dbgs(s"block $i: $blk {")
    state.logTab += 1
    visitBlock(blk)
    state.logTab -= 1
    dbgs(s"} // End of $lhs block #$i")
    state.logTab -= 1
  }

  private def findDelays(x: Sym[_]): Set[(Int,Seq[Sym[_]])] = x match {
    case s: Sym[_] => s.consumers.flatMap{
      case d @ Op(DelayLine(size,_)) => findDelays(d).map{case (dly,cs) => (dly+size,cs) }
      case d => Seq((0,Seq(d)))
    }.filter(_._1 > 0)
    case _ => Set.empty
  }
  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    if (rhs.blocks.nonEmpty) dbgs(s"$lhs = $rhs {")
    else                     dbgs(s"$lhs = $rhs")
    lhs.name.foreach{name => dbgs(s" - Name: $name") }
    val cycle = reduceCycleOf(lhs)
    val inReduce = cycle.nonEmpty
    dbgs(s" - Type: ${lhs.tp}")
    if (cycle.isEmpty) {
      dbgs(s" - Cycle: <no cycle>")
    }
    else {
      dbgs(s" - Cycle: " + cycle.mkString(", "))
    }
    dbgs(s" - Latency:          ${latencyModel.latencyOf(lhs,inReduce)}")
    dbgs(s" - Reduce Latency:   ${latencyModel.latencyOf(lhs,inReduce)}")
    dbgs(s" - Requires Regs:    ${latencyModel.requiresRegisters(lhs,inReduce)}")
    dbgs(s" - Built-In Latency: ${latencyModel.builtInLatencyOfNode(lhs)}")
    val delays = findDelays(lhs).toSeq.sortBy(_._1)
    if (delays.isEmpty) {
      dbgs(s" - Delays: <none>")
    }
    else {
      dbgs(s" - Delays: ")
      delays.foreach{case (d,cons) =>  dbgs(s"     [$d] ${cons.mkString(",")}") }
    }

    printBlocks(lhs, rhs.blocks)

    if (rhs.blocks.nonEmpty) dbgs(s"} // End of $lhs")
  }
}

