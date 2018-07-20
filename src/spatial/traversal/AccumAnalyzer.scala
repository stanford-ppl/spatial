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
      // TODO: May want to keep this in metadata from a different common traversal
      val (_, cycles) = latenciesAndCycles(block)
      val warCycles = cycles.collect{case cycle:WARCycle => cycle }

      var disjointCycles: Set[WARCycle] = Set.empty

      // Find sets of cycles which are entirely disjoint
      warCycles.foreach{c1 =>
        dbgs(s"Candidate cycle: ")

        val overlapping = disjointCycles.filter{c2 => (c1.symbols intersect c2.symbols).nonEmpty }
        val closed = c1.symbols.forall{s =>
          dbgs(s"  $s")
          val outsideConsumers = s.consumers diff c1.symbols
          outsideConsumers.isEmpty
        }
        if (overlapping.isEmpty) disjointCycles += c1
        else disjointCycles --= overlapping
      }

      disjointCycles.foreach{c =>
        val cycle = c.copy(shouldSpecialize = true)
        c.symbols.foreach{s => s.reduceCycle = cycle }
      }
    }
    super.visitBlock(block)
  }

}
