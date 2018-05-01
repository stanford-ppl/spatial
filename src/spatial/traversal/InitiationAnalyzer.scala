package spatial.traversal

import argon._
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._
import spatial.internal.spatialConfig

case class InitiationAnalyzer(IR: State) extends AccelTraversal {

  private def visitOuterControl(lhs: Sym[_], rhs: Op[_]): Unit = {
    dbgs(stm(lhs))
    rhs.blocks.foreach{blk => visitBlock(blk) }
    val interval = (1.0 +: lhs.children.map{child => child.sym.II }).max
    dbgs(s" - Interval: $interval")
    lhs.II = lhs.userII.getOrElse(interval)
  }

  private def visitInnerControl(lhs: Sym[_], rhs: Op[_]): Unit = {
    dbgs(stm(lhs))
    val blks = rhs.blocks.map{block => latencyAndInterval(block) }
    val latency = blks.map(_._1).sum
    val interval = (1.0 +: blks.map(_._2)).max
    dbgs(s" - Latency:  $latency")
    dbgs(s" - Interval: $interval")
    lhs.bodyLatency = latency
    lhs.II = lhs.userII.getOrElse(interval)
  }

  private def visitControl(lhs: Sym[_], rhs: Op[_]): Unit = {
    if (lhs.isInnerControl) visitInnerControl(lhs,rhs) else visitOuterControl(lhs,rhs)
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _:AccelScope => inAccel{ visitControl(lhs,rhs) }

    // TODO[4]: Still need to verify that this rule is generally correct
    case StateMachine(_,_,notDone,action,nextState) if lhs.isInnerControl =>
      dbgs(stm(lhs))
      rhs.blocks.foreach{blk => visitBlock(blk) }
      val (latNotDone, iiNotDone) = latencyAndInterval(notDone)
      val (latNextState, iiNextState) = latencyAndInterval(nextState)
      val (actionLats, actionCycles) = latenciesAndCycles(action)

      val actionWritten = action.nestedStms.collect{case Writer(mem,_,_,_) => mem }.toSet
      val nextStateRead = nextState.nestedStms.collect{case Reader(mem,_,_) => mem  }.toSet
      val dependencies  = nextStateRead intersect actionWritten

      val writeLatency = if (!spatialConfig.enableRetiming || latencyModel.requiresRegisters(nextState.result, inReduce = true)) 1.0 else 0.0

      val actionLatency = latNextState + writeLatency + (1.0 +: actionLats.values.toSeq).max

      dbgs("Written memories: " + actionWritten.mkString(", "))
      dbgs("Read memories: " + nextStateRead.mkString(", "))
      dbgs("Intersection: " + dependencies.mkString(", "))

      val latency = latNotDone + actionLatency  // Accounts for hidden register write to state register

      val rawInterval = (Seq(latNextState+1, iiNotDone, iiNextState) ++ actionCycles.map(_.length)).max

      val interval = if (dependencies.nonEmpty) Math.max(rawInterval, actionLatency) else rawInterval

      dbgs(s" - Latency:  $latency")
      dbgs(s" - Interval: $interval")
      lhs.II = lhs.userII.getOrElse(interval)
      lhs.bodyLatency = latency

    case StateMachine(_,_,notDone,action,nextState) if lhs.isOuterControl =>
      dbgs(stm(lhs))
      rhs.blocks.foreach{blk => visitBlock(blk) }
      val (latNotDone, iiNotDone) = latencyAndInterval(notDone)
      val (latNextState, iiNextState) = latencyAndInterval(nextState)
      val interval = (Seq(1.0, iiNotDone, iiNextState) ++ lhs.children.map{child => child.sym.II }).max
      dbgs(s" - Latency: $latNotDone, $latNextState")
      dbgs(s" - Interval: $interval")
      lhs.II = lhs.userII.getOrElse(interval)
      lhs.bodyLatency = Seq(latNotDone, latNextState)

    case _ if lhs.isControl => visitControl(lhs, rhs)
    case _ => super.visit(lhs, rhs)
  }

}
