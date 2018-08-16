package spatial.traversal

import argon._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.lang._
import spatial.node._
import spatial.util.modeling._



case class RetimingAnalyzer(IR: State) extends BlkTraversal {

  private def analyzeCtrl[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    // Switches aren't technically inner controllers from PipeRetimer's point of view.
    if (inHw) {
      rhs.blocks.foreach{block => 
        val (retiming, _) = latenciesAndCycles(block)
        retiming.foreach{case (sym, lat) => dbgs(s"Setting $sym -> $lat");sym.fullDelay = lat}
      }
    }
  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _:AccelScope => inAccel { analyzeCtrl(lhs,rhs) }
    case _ if (lhs.isInnerControl) => analyzeCtrl(lhs, rhs)
    case _ => super.visit(lhs,rhs)
  }

}
