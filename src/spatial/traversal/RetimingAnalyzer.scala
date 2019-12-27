package spatial.traversal

import argon._
import spatial.metadata.blackbox._
import spatial.metadata.control._
import spatial.metadata.math._
import spatial.metadata.retiming._
import spatial.node._
import spatial.util.modeling._
import spatial.util.spatialConfig
import utils.implicits.collections._

case class RetimingAnalyzer(IR: State) extends AccelTraversal {
  override def shouldRun: Boolean = spatialConfig.enableRetiming

  var retimeBlocks: List[Boolean] = Nil
  var pushBlocks: List[Boolean] = Nil // Marker for whether a block needs to begin after the end of the previous block
  var lastLatency: Double = 0
  var cycles: Set[Sym[_]] = Set.empty

  override protected def preprocess[R](block: Block[R]): Block[R] = {
    block.nestedStms.collect{case x if (x.delayDefined) => x.fullDelay = 0.0}
    super.preprocess(block)
  }

  private def retimeBlock[T](block: Block[T], saveLatency: Boolean): Unit = {
    val scope = block.nestedStms.toSortedSeq
    val result = (scope.flatMap{case Op(d) => d.blocks; case _ => Nil} :+ block).flatMap(exps(_)).toSortedSeq

    dbgs(s"Retiming block $block:")
    //scope.foreach{e => dbgs(s"  ${stm(e)}") }
    //dbgs(s"Result: ")
    //result.foreach{e => dbgs(s"  ${stm(e)}") }
    // The position AFTER the given node
    val (newLatencies, newCycles) = pipeLatencies(result, scope)
    val adjustedLatencies = newLatencies.map{case (k,v) => (k , v + lastLatency)}

    adjustedLatencies.toList.sortBy(_._2).foreach{case (s,l) => dbgs(s"[$l] ${stm(s)}") }
    cycles ++= newCycles.flatMap(_.symbols)

    dbgs("")
    dbgs("")
    dbgs("Sym Delays:")
    adjustedLatencies.toList.map{case (s,l) => s -> scrubNoise(l - latencyOf(s, inReduce = cycles.contains(s))) }
      .sortBy(_._2)
      .foreach{case (s,l) =>
        s.fullDelay = l
        if (cycles.contains(s)) s.inCycle = true
        dbgs(s"  [$l = ${adjustedLatencies(s)} - ${latencyOf(s, inReduce = cycles.contains(s))}]: ${stm(s)} [cycle = ${cycles.contains(s)}]")
      }
    if (saveLatency) {
      lastLatency = adjustedLatencies.toList.map(_._2).sorted.reverse.headOption.getOrElse(0.0)
      dbgs(s"Storing latency of block: $lastLatency")
    } else lastLatency = 0.0
  }

  override protected def visitBlock[T](b: Block[T]): Block[T] = {
    val doWrap = retimeBlocks.headOption.getOrElse(false)
    val saveLatency = pushBlocks.headOption.getOrElse(false)
    if (retimeBlocks.nonEmpty) retimeBlocks = retimeBlocks.drop(1)
    if (pushBlocks.nonEmpty) pushBlocks = pushBlocks.drop(1)
    dbgs(s"Visiting Block $b [$retimeBlocks => $doWrap, $pushBlocks => $saveLatency]")
    if (doWrap) retimeBlock(b,saveLatency)
    else super.visitBlock(b)
    b
  }


  def withRetime[A](wrap: List[Boolean], push: List[Boolean])(x: => A): A = {
    val prevRetime = retimeBlocks
    val prevPush = pushBlocks
    val prevCtx = ctx

    retimeBlocks = wrap
    pushBlocks = push
    val result = x

    retimeBlocks = prevRetime
    pushBlocks = prevPush
    result
  }


  private def analyzeCtrl[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    // // Switches aren't technically inner controllers from PipeRetimer's point of view.
    // if (inHw) {
    //   rhs.blocks.foreach{block => 
    //     val (retiming, _) = latenciesAndCycles(block)
    //     retiming.foreach{case (sym, lat) => dbgs(s"Setting $sym -> $lat");sym.fullDelay = lat}
    //   }
    // }


    // Switches aren't technically inner controllers from PipeRetimer's point of view.
    if ((lhs.isInnerControl || lhs.isBlackboxImpl) && !rhs.isSwitch && inHw) {
      val retimeEnables = rhs.blocks.map{_ => true }.toList
      val retimePushLaterBlock = rhs.blocks.map{_ => false }.toList
      rhs match {
        case _:StateMachine[_] => withRetime(retimeEnables, List(false,true,false)) { super.visit(lhs, rhs) }
        case _ => withRetime(retimeEnables, retimePushLaterBlock) { super.visit(lhs, rhs) }
      }
      
    }
    else rhs match {
      case _:StateMachine[_] => withRetime(List(true,false,false), List(false,false,false)){ super.visit(lhs, rhs) }
      case _ => if (inHw) withRetime(Nil, Nil){ super.visit(lhs, rhs) } else super.visit(lhs, rhs)
    }

  }


  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _:AccelScope => inAccel { analyzeCtrl(lhs,rhs) }
    case _:BlackboxImpl[_,_,_] => inAccel { analyzeCtrl(lhs,rhs) }
    case _ if lhs.isInnerControl => analyzeCtrl(lhs, rhs)
    case _ => super.visit(lhs,rhs)
  }

}

