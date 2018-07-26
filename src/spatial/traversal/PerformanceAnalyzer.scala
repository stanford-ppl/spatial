package spatial.traversal

import argon._
import spatial.metadata.control._
import spatial.metadata.modeling._
import spatial.node._
import spatial.util.modeling._

case class PerformanceAnalyzer(IR: State) extends AccelTraversal with RerunTraversal {
  var totalRuntime: Double = 0
  var inCycle: Boolean = false

  override def init(): Unit = super.init()

  override def rerun(sym: Sym[_], block: Block[_]): Unit = {
    inHw = true
    totalRuntime = 0
    super.rerun(sym, block)
    inHw = false
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    val (nodeRuntime, nodeII) = nestedRuntimeOfNode(lhs, rhs)
    lhs.latency = nodeRuntime
    lhs.II = nodeII
    totalRuntime = totalRuntime + nodeRuntime
  }

  private def latencyOfNode(sym: Sym[_]): Double = latencyModel.latencyOf(sym, inCycle)

  private def latencyOfBlock(b: Block[_], parMask: Boolean = false): Seq[Double] = {

  }

  private def nestedRuntimeOfNode[A](lhs: Sym[A], rhs: Op[A]): (Double,Double) = rhs match {
    case AccelScope(block) if lhs.isInnerControl =>
      inAccel{
        val body = latencyOfPipe(block)
        val totalTime = body + latencyOf(lhs)
        dbgs(s"Inner Accel $lhs: $totalTime cycles")
        (totalTime, 1.0)
      }
    case AccelScope(block) =>
      inAccel{
        lhs.children.foreach{child => visit(child.sym) }

      }

  }

}
