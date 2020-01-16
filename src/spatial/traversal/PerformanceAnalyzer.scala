package spatial.traversal

import argon._
import spatial.metadata.control._
import spatial.metadata.modeling._
import spatial.node._
import spatial.util.modeling._

import utils.implicits.collections._

case class PerformanceAnalyzer(IR: State) extends AccelTraversal with RerunTraversal {
  var totalRuntime: Double = 0
  var inCycle: Boolean = false

  override def init(): Unit = super.init()

  override def rerun(sym: Sym[_], block: Block[_]): Unit = {
    inAccel = true
    totalRuntime = 0
    super.rerun(sym, block)
    inAccel = false
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    val (nodeRuntime, nodeII) = nestedRuntimeOfNode(lhs, rhs)
    lhs.latency = nodeRuntime
    lhs.II = nodeII
    totalRuntime = totalRuntime + nodeRuntime
  }

  private def ctrlHeader(lhs: Sym[_]): Unit = {
    dbgs(s"")
    dbgs(s"${lhs.ctx}: ${lhs.ctx.content.map(_.trim).getOrElse(stm(lhs))}")
  }

  private def nestedRuntimeOfNode[A](lhs: Sym[A], rhs: Op[A]): (Double,Double) = rhs match {
    case AccelScope(block) if lhs.isInnerControl =>
      inAccel{
        val body = latencyOfPipe(block)
        val total = body + latencyOf(lhs)
        ctrlHeader(lhs)
        dbgs(s"Inner Accel $lhs: $total cycles")
        (total, 1.0)
      }
    case AccelScope(block) =>
      inAccel{
        visitBlock(block)
        val stages = lhs.children.map(_.sym.latency)
        val total = latencyModel.outerControlModel(N=1,ii=1,stages,lhs)
        ctrlHeader(lhs)
        dbgs(s"Accel $lhs: $total cycles")
        lhs.children.zipWithIndex.foreach{case (child,i) =>
          dbgs(s" - $i: ${child.sym.latency} [${child.sym}]")
        }
        (total, 1.0)
      }

    case ParallelPipe(_,block) =>
      visitBlock(block)
      val stages = lhs.children.map(_.sym.latency)
      val IIs    = lhs.children.map(_.sym.II)
      val II     = lhs.userII.getOrElse(IIs.maxOrElse(1))
      val total  = latencyModel.parallelModel(stages, lhs)
      ctrlHeader(lhs)
      dbgs(s"Parallel $lhs: $total cycles [II: $II]")
      lhs.children.zipWithIndex.foreach{case (child,i) =>
        dbgs(s" - $i: ${child.sym.latency} [${child.sym}]")
      }
      (total, II)

    case UnitPipe(_, block, _) if lhs.isInnerControl =>
      val (latency, blockII) = latencyAndInterval(block)
      val II = lhs.userII.getOrElse(blockII)
      val total = latency + latencyOf(lhs)
      ctrlHeader(lhs)
      dbgs(s"Inner Pipe $lhs: $total cycles [II: $II]")
      (total, II)

    case UnitPipe(_, block, _) if lhs.isOuterControl =>
      visitBlock(block)
      val stages = lhs.children.map(_.sym.latency)
      val IIs    = lhs.children.map(_.sym.II)
      val II     = lhs.userII.getOrElse(IIs.maxOrElse(1))
      val total  = latencyModel.sequentialModel(N=1,ii=1,stages, lhs)
      ctrlHeader(lhs)
      dbgs(s"Outer Pipe $lhs: $total cycles [II: $II]")
      lhs.children.zipWithIndex.foreach{case (child,i) =>
        dbgs(s" - $i: ${child.sym.latency} [${child.sym}]")
      }
      (total, II)

    // case OpForeach(ens,cchain,block,_,_) if lhs.isInnerControl =>
    case _ => (0,0) // ??
      

  }

}
