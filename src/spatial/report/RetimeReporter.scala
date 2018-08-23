package spatial.report

import argon._
import spatial.metadata.retiming._
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.util.spatialConfig
import spatial.util.modeling._

case class RetimeReporter(IR: State) extends AccelTraversal {
  override def shouldRun: Boolean = config.enInfo && spatialConfig.enableRetiming

  override def process[R](block: Block[R]): Block[R] = {
    inGen(config.repDir, "Retime.report"){ super.process(block) }
  }

  private def printBlocks(lhs: Sym[_], blocks: Seq[Block[_]]): Unit = blocks.zipWithIndex.foreach{case (blk,i) =>
    state.incGenTab()
    emit(s"block $i: $blk {")
    state.incGenTab()
    visitBlock(blk)
    state.decGenTab()
    emit(s"} // End of $lhs block #$i")
    state.decGenTab()
  }

  private def findDelays(x: Sym[_]): Set[(Int,Seq[Sym[_]])] = x match {
    case s: Sym[_] => s.consumers.flatMap{
      case d @ Op(DelayLine(size,_)) => findDelays(d).map{case (dly,cs) => (dly+size,cs) }
      case d => Seq((0,Seq(d)))
    }.filter(_._1 > 0)
    case _ => Set.empty
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    if (rhs.blocks.nonEmpty) emit(s"$lhs = $rhs {")
    else                     emit(s"$lhs = $rhs")
    lhs.name.foreach{name => emit(s" - Name: $name") }
    emit(s" - Type: ${lhs.tp}")
    val inCycle = lhs.isInCycle
    if (!inCycle) {
      emit(s" - Cycle: <no cycle>")
    }
    else {
      val cycle = lhs.getReduceCycle
      emit(s" - Cycle: " + cycle.get.symbols.mkString(", "))
    }
    emit(s" - Latency:          ${latencyModel.latencyOf(lhs,inCycle)}")
    emit(s" - Reduce Latency:   ${latencyModel.latencyOf(lhs,inCycle)}")
    emit(s" - Requires Regs:    ${latencyModel.requiresRegisters(lhs,inCycle)}")
    emit(s" - Built-In Latency: ${latencyModel.builtInLatencyOfNode(lhs)}")
    val delays = findDelays(lhs).toSeq.sortBy(_._1)
    if (delays.isEmpty) {
      emit(s" - Delays: <none>")
    }
    else {
      emit(s" - Delays: ")
      delays.foreach{case (d,cons) =>  emit(s"     [$d] ${cons.mkString(",")}") }
    }

    printBlocks(lhs, rhs.blocks)

    if (rhs.blocks.nonEmpty) emit(s"} // End of $lhs")
  }
}

