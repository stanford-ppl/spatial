package spatial.traversal

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.retiming._
import spatial.util.modeling._

case class AccumAnalyzer(IR: State) extends AccelTraversal {

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case AccelScope(_) => inAccel{ super.visit(lhs,rhs) }
    case _ => super.visit(lhs, rhs)
  }

  override def visitBlock[R](block: Block[R]): Block[R] = {
    if (inHw) {
      // Find standard write-after-read accumulation candidates
      val (_, cycles) = latenciesAndCycles(block)
      val warCycles = cycles.collect{case cycle:WARCycle => cycle }

      var distinctCycles: Set[WARCycle] = Set.empty

      warCycles.foreach{c1 =>
        val overlapping = distinctCycles.filter{c2 => (c1.symbols intersect c2.symbols).nonEmpty }
        if (overlapping.isEmpty) distinctCycles += c1
        else distinctCycles --= overlapping
      }


    }
    super.visitBlock(block)
  }

}
