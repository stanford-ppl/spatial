package spatial.traversal

import argon._
import models._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.modeling._
import spatial.metadata.retiming.ValueDelay
import spatial.metadata.types._
import spatial.util.modeling._
import spatial.util.reductionTreeDelays

case class AreaAnalyzer(IR: State) extends AccelTraversal with RerunTraversal {
  var totalArea: Area = NoArea
  var savedArea: Area = NoArea
  var inCycle: Boolean = false

  override def init(): Unit = super.init()

  override def rerun(sym: Sym[_], block: Block[_]): Unit = {
    inHw = true
    totalArea = savedArea
    super.rerun(sym, block)
    inHw = false
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    val nodeArea = nestedAreaOfNode(lhs, rhs)
    lhs.area = nodeArea
    totalArea = totalArea + nodeArea
  }

  private def pipelineDelayArea(block: Block[_], par: Int): Area = {
    val (latencies, cycles) = latenciesAndCycles(block)
    val cycleSymbols = cycles.flatMap(_.symbols)

    val delayLines = computeDelayLines(
      scope = block.stms,
      latencies = latencies,
      hierarchy = 0,
      delayLines = Map.empty,
      cycles = cycleSymbols,
      createLine = None
    )

    delayLines.map{case (_, ValueDelay(in,_,size,_,_,_)) =>
      val nbits = in.tp match {case Bits(bt) => bt.nbits; case _ => 0 }
      areaModel.areaOfDelayLine(size, nbits, par)
    }.fold(NoArea){_+_}
  }

  private def areaOfNode(sym: Sym[_]): Area = areaModel.areaOf(sym, inHw, inCycle)

  private def areaOfBlock(block: Block[_], isInner: Boolean, par: Int): Area = {
    val blockArea = block.stms.map{
      case Stm(lhs,rhs) => nestedAreaOfNode(lhs,rhs)
      case _ => NoArea
    }.fold(NoArea){_+_}

    val delayLineArea = if (isInner) {
      pipelineDelayArea(block, par)
    }
    else {
      NoArea
    }

    blockArea + delayLineArea
  }
  private def areaOfPipe(block: Block[_], par: Int): Area = {
    areaOfBlock(block, isInner = true, par)
  }
  private def areaOfCycle(block: Block[_], isInner: Boolean, par: Int): Area = {
    val saveInCycle = inCycle
    inCycle = true
    val result = areaOfBlock(block, isInner, par)
    inCycle = saveInCycle
    result
  }


  private def nestedAreaOfNode(lhs: Sym[_], rhs: Op[_]): Area = rhs match {
    case AccelScope(block) =>
      savedArea = totalArea
      inAccel{
        val body = areaOfBlock(block, lhs.isInnerControl, par = 1)
        val totalArea = body + areaOfNode(lhs)
        dbgs(s"Accel: $lhs: $totalArea")
        dbgs(s" - Body: $body")
        totalArea
      }

    case op @ OpReduce(_, cchain, _, map, load, reduce, store, _, _, _, _) =>
      /** The number of nodes in a binary tree with P leaves is P - 1 */
      val P = cchain.constPars.product  // Parallelization factor of map

      val mapArea: Area = areaOfBlock(map, lhs.isInnerControl, P)

      // TODO: Account for mux area in tree reduction
      val reduceCycles: Double = latencyOfPipe(reduce)
      val treeArea_nodes: Area = areaOfBlock(reduce, isInner = true, P - 1)
      val treeArea_paths: Area = reductionTreeDelays(P).map{delay =>
        areaModel.areaOfDelayLine( (delay*reduceCycles).toInt, op.A.nbits, par = 1)
      }.fold(NoArea){_+_}
      val treeArea = treeArea_nodes + treeArea_paths

      val cycleArea_load: Area   = areaOfCycle(load, isInner = true, par = 1)
      val cycleArea_reduce: Area = areaOfCycle(reduce, isInner = true, par = 1)
      val cycleArea_store: Area  = areaOfCycle(store, isInner = true, par = 1)
      val cycleArea = cycleArea_load + cycleArea_reduce + cycleArea_store

      val totalArea: Area = mapArea + treeArea + cycleArea + areaOfNode(lhs)
      dbgs(s"Reduce: $lhs (P = $P): $totalArea")
      dbgs(s" - Map:   $mapArea")
      dbgs(s" - Tree:  $treeArea")
      dbgs(s"   - Nodes: $treeArea_nodes")
      dbgs(s"   - Paths: $treeArea_paths")
      dbgs(s" - Cycle: $cycleArea")
      dbgs(s"   - Load:   $cycleArea_load")
      dbgs(s"   - Reduce: $cycleArea_reduce")
      dbgs(s"   - Store:  $cycleArea_store")
      totalArea

    case op @ OpMemReduce(_, cchainMap, cchainRed, _, map, loadRes, loadAcc, reduce, storeAcc, ident, fold, itersMap, itersRed, _) =>
      val Pm = cchainMap.constPars.product
      val Pr = cchainRed.constPars.product

      val mapArea: Area = areaOfBlock(map, isInner = false, Pm)

      val reduceCycles: Double = latencyOfPipe(reduce)
      val treeArea_nodes: Area = areaOfPipe(reduce, par = 1) * Pm * Pr
      val treeArea_paths: Area = reductionTreeDelays(Pm).map{delay =>
        areaModel.areaOfDelayLine( (delay*reduceCycles).toInt, op.A.nbits, 1)
      }.fold(NoArea){_+_}
      val treeArea = treeArea_nodes + treeArea_paths

      val cycleArea_loadRes: Area = areaOfPipe(loadRes, Pr)*Pm
      val cycleArea_loadAcc = areaOfCycle(loadAcc, isInner = true, par = Pr)
      val cycleArea_reduce  = areaOfCycle(reduce, isInner = true, par = 1) * Pr
      val cycleArea_storeAcc = areaOfCycle(storeAcc, isInner = true, par = Pr)
      val cycleArea = cycleArea_loadRes + cycleArea_loadAcc + cycleArea_reduce + cycleArea_storeAcc

      val totalArea = mapArea + treeArea + cycleArea + areaOfNode(lhs)

      dbgs(s"MemReduce: $lhs (Pm = $Pm, Pr = $Pr): $totalArea")
      dbgs(s" - Map: $mapArea")
      dbgs(s" - Tree: $treeArea")
      dbgs(s"   - Nodes: $treeArea_nodes")
      dbgs(s"   - Paths: $treeArea_paths")
      dbgs(s" - Cycle: $cycleArea")
      dbgs(s"   - LoadRes: $cycleArea_loadRes")
      dbgs(s"   - LoadAcc: $cycleArea_loadAcc")
      dbgs(s"   - Reduce:  $cycleArea_reduce")
      dbgs(s"   - Store:   $cycleArea_storeAcc")
      totalArea

    case ctrl: Control[_] =>
      val bodyAreas = ctrl.bodies.map{body =>
        val blocks = body.blocks
        val isInner = lhs.isInnerControl || body.isInnerStage
        blocks.map{case (iters, block) =>
          val P = iters.map(_.ctrPar.toInt).product
          areaOfBlock(block, isInner, P)
        }.fold(NoArea){_+_}
      }

      val totalArea = areaOfNode(lhs) + bodyAreas.fold(NoArea){_+_}

      def bd(i: Int): String = if (bodyAreas.length > 1) s" #${i+1}" else ""
      dbgs(s"${stm(lhs)}: $totalArea")
      bodyAreas.zipWithIndex.foreach{case (body,i) =>
        dbgs(s" - Body${bd(i)}: $body")
      }
      totalArea

    case _ =>
      val blockAreas = lhs.blocks.map{blk => areaOfBlock(blk, isInner = false, par = 1) }
      val totalArea = areaOfNode(lhs) + blockAreas.fold(NoArea){_+_}

      def bd(i: Int): String = if (blockAreas.length > 1) s" #${i+1}" else ""
      dbg(s"${stm(lhs)}: $totalArea")
      blockAreas.zipWithIndex.foreach{case (blk,i) =>
        dbgs(s" - Block${bd(i)}: $blk")
      }
      totalArea
  }

}
