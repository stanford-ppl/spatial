package spatial.traversal

import argon._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.util.modeling._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.blackbox._

case class InitiationAnalyzer(IR: State) extends AccelTraversal {

  private def visitOuterControl(lhs: Sym[_], rhs: Op[_]): Unit = {
    dbgs(stm(lhs))
    rhs.blocks.foreach{blk => visitBlock(blk) }
    val interval = (1.0 +: lhs.children.map{child => child.sym.II }).max
    dbgs(s" - Interval: $interval")
    lhs.II = lhs.userII.getOrElse(interval)
    lhs.compilerII = interval
  }

  private def visitInnerControl(lhs: Sym[_], rhs: Op[_]): Unit = {
    dbgs(stm(lhs))
    val blks = rhs.blocks.map{block => latencyAndInterval(block) }
    val latency = blks.map(_._1).sum
    val slowestBbox: Double = rhs.blocks.map{block => block.stms.map{x => x.bboxII}.sorted.lastOption.getOrElse(1.0)}.sorted.lastOption.getOrElse(1.0)
    val interval: Double = (slowestBbox +: blks.map(_._2)).max
    val iterdiffs = rhs.blocks.flatMap { block => block.stms.collect { case x if x.getIterDiff.isDefined => x.iterDiff } }.sorted
    val forceII1 = iterdiffs.reverse.headOption.getOrElse(1) <= 0
    val minIterDiff = iterdiffs.find(_ > 0)
    rhs.blocks.foreach{ block => block.stms.foreach{ x => dbgs(s"   stm: $x, ${x.getIterDiff}")}}
    dbgs(s" - Latency:  $latency")
    dbgs(s" - Interval: $interval ($slowestBbox bbox)")
    dbgs(s" - Iter Diff: $minIterDiff (from $iterdiffs)")
    lhs.bodyLatency = latency
    val compilerII = {
      if (forceII1) 1.0
      else if (minIterDiff.isEmpty) interval
      else if (minIterDiff.get == 1) interval
      else scala.math.ceil(interval/minIterDiff.get)
    }
    lhs.II = scala.math.max(1,
      if (lhs.getUserSchedule.isDefined && lhs.userSchedule == Sequenced) latency
      else lhs.userII.getOrElse(scala.math.min(compilerII,latency))
    )
    lhs.compilerII = compilerII
  }

  private def visitControl(lhs: Sym[_], rhs: Op[_]): Unit = {
    if (lhs.isInnerControl | lhs.isSpatialPrimitiveBlackbox) visitInnerControl(lhs,rhs) else visitOuterControl(lhs,rhs)
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _:AccelScope => inAccel{ visitControl(lhs,rhs) }
    case _:BlackboxImpl[_,_,_] => inBox{ visitControl(lhs,rhs) }

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

    case _: Switch[_] if lhs.isInnerControl =>
      visitControl(lhs, rhs)
      lhs.children.foreach{c => c.s.get.bodyLatency = lhs.bodyLatency; c.s.get.II = lhs.II}

    case _ if lhs.isControl => visitControl(lhs, rhs)
    case _ => super.visit(lhs, rhs)
  }

}
