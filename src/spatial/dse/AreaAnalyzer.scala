package spatial.dse

import argon._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.util.modeling._
import spatial.metadata.bounds._
import spatial.metadata.control._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.traversal._
import spatial.targets._
import models._
import argon.node._

import scala.collection.mutable

case class AreaAnalyzer(IR: State, areaModel: AreaModel, latencyModel: LatencyModel) extends RerunTraversal with AccelTraversal  {
  private def NoArea: Area = areaModel.NoArea

  var totalArea: (Area, String) = _
  var scopeArea: Seq[Area] = Nil
  var savedArea: Area = _

  override def init(): Unit = if (needsInit) {
    areaModel.init()
    scopeArea = Nil
    savedArea = NoArea
    super.init()
  }

  override def silence(): Unit = {
    super.silence()
    areaModel.silence()
  }

  override def rerun(e: Sym[_], blk: Block[_]): Unit = {
    isRerun = true
    preprocess(blk)
    super.rerun(e, blk)
    postprocess(blk)
    isRerun = false
  }

  override protected def preprocess[R](block: Block[R]): Block[R] = {
    scopeArea = Nil
    inReduce = false
    areaModel.reset()
    super.preprocess(block)
  }

  override protected def postprocess[R](block: Block[R]): Block[R] = {
    val saved = if (isRerun) savedArea else NoArea
    val total = (saved +: scopeArea).fold(NoArea){_+_}
    val area = areaModel.summarize(total)
    totalArea = area

    if (config.enDbg) { areaModel.reportMissing() }

    super.postprocess(block)
  }

  def areaOf(e: Sym[_]): Area = areaModel.areaOf(e, inHw, inReduce)
  def requiresRegisters(x: Sym[_], inReduce: Boolean): Boolean = latencyModel.requiresRegisters(x, inReduce)
  def retimingDelay(x: Sym[_], inReduce: Boolean): Int = if (requiresRegisters(x,inReduce)) latencyOf(x).toInt else 0

  def bitBasedInputs(d: Op[_]): Seq[Sym[_]] = exps(d).filterNot(_.isGlobal).filter{e => Bits.unapply(e.tp).isDefined }.toSeq

  def pipeDelayLineArea(block: Block[_], par: Int): Area = {
    val (latencies, cycles) = latenciesAndCycles(block, verbose = false)
    val cycleSyms = cycles.flatMap(_.symbols)
    val scope = latencies.keySet
    def delayOf(x: Sym[_]): Int = latencies.getOrElse(x, 0.0).toInt
    /*
    Alternative (functional) implementation (it's a groupByReduce! plus a map, plus a reduce):
    scope.flatMap{
      case s@Def(d) =>
        val criticalPath = delayOf(s) - latencyOf(s)
        bitBasedInputs(d).flatMap{in =>
          val size = retimingDelay(in) + criticalPath - delayOf(in)
          if (size > 0) Some(in -> size) else None
        }
      case _ => Nil
    }.groupBy(_._1)
     .mapValues(_.map(_._2).max)
     .map{case (e, delay) => areaModel.areaOfDelayLine(delay.toInt, nbits(e), par) }
     .fold(NoArea){_+_}
   */

    val delayLines = mutable.HashMap[Sym[_],Long]()

    scope.foreach{
      case s@Def(d) =>
        val criticalPath = delayOf(s) - latencyOf(s)
        bitBasedInputs(d).foreach{in =>
          val inReduce = cycleSyms.contains(in)
          val size = retimingDelay(in, inReduce) + criticalPath - delayOf(in)
          if (size > 0) {
            delayLines(in) = Math.max(delayLines.getOrElse(in, 0L), size.toLong)
          }
        }
      case _ => // No inputs so do nothing
    }

    delayLines.map{case (e,len) => areaModel.areaOfDelayLine(len.toInt,spatial.metadata.types.nbits(e),par) }.fold(NoArea){_+_}
  }

  def areaOfBlock(block: Block[_], isInner: Boolean, par: Int): Area = {
    val outerArea = scopeArea
    scopeArea = Nil
    visitBlock(block)
    val area = scopeArea.fold(NoArea){_+_}
    scopeArea = outerArea

    if (isInner) {
      val delayArea = pipeDelayLineArea(block, par)
      area*par + delayArea
    }
    else {
      area*par
    }
  }

  def areaOfCycle(block: Block[_], par: Int): Area = {
    val outerReduce = inReduce
    inReduce = true
    val area = areaOfBlock(block, isInner=true, par)
    inReduce = outerReduce
    area
  }

  def areaOfPipe(block: Block[_], par: Int): Area = areaOfBlock(block, isInner = true, par)

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    val area: Area = rhs match {
      case AccelScope(block) =>
        inAccel{
          savedArea = scopeArea.fold(NoArea){_+_}
          val body = areaOfBlock(block, lhs.isInnerControl, 1)
          body
        }

      case ParallelPipe(en, block) =>
        val body = areaOfBlock(block, isInner = false, 1)
        dbgs(s"Parallel $lhs: ")
        dbgs(s" - Body: $body")
        body + areaOf(lhs)

      case UnitPipe(en, block)     =>
        val body = areaOfBlock(block, isInner = lhs.isInnerControl, 1)
        dbgs(s"UnitPipe: $lhs")
        dbgs(s" - Body: $body")
        body + areaOf(lhs)

      case OpForeach(en, cchain, block, iters) =>
        val P = cchain.constPars.product
        val body = areaOfBlock(block, lhs.isInnerControl, P)
        dbgs(s"Foreach: $lhs (P = $P)")
        dbgs(s" - Body: $body")
        body + areaOf(lhs)

      case op@OpReduce(en, cchain, accum, map, load, reduce, store, ident, fold, iters) =>
        val P = cchain.constPars.product
        val mapArea: Area = areaOfBlock(map, lhs.isInnerControl, P) // Map is duplicated P times
        /*
          Some simple math:
          A full binary (reduction) tree is a tree in which every node is either
          a leaf or has exactly two children.
          The number of internal (non-leaf) nodes of a full tree with L leaves is L - 1
          In our case, L is the map's parallelization factor P
          and internal nodes represent duplicates of the reduction function
          The reduction function is therefore duplicated P - 1 times
          Plus the special, tightly cyclic reduction function to update the accumulator
        */
        val treeArea: Area = areaOfBlock(reduce, isInner = true, P - 1)
        val reduceLength = latencyOfPipe(reduce)
        val treeDelayArea: Area = reductionTreeDelays(P).map{dly => areaModel.areaOfDelayLine((reduceLength*dly).toInt,op.A.nbits,1) }
                                                  .fold(NoArea){_+_}
        val loadArea: Area  = areaOfCycle(load, 1)
        val cycleArea: Area = areaOfCycle(reduce, 1)
        val storeArea: Area = areaOfCycle(store, 1)

        dbgs(s"Reduce: $lhs (P = $P)")
        dbgs(s" - Map:    $mapArea")
        dbgs(s" - Tree:   $treeArea")
        dbgs(s" - Delays: $treeDelayArea")
        dbgs(s" - Cycle:  ${loadArea + storeArea + cycleArea}")

        mapArea + treeArea + treeDelayArea + loadArea + cycleArea + storeArea + areaOf(lhs)

      case op@OpMemReduce(en,cchainMap,cchainRed,accum,map,loadRes,loadAcc,reduce,storeAcc,ident,fold,itersMap,itersRed) =>
        val Pm = cchainMap.constPars.product
        val Pr = cchainRed.constPars.product

        val mapArea = areaOfBlock(map,lhs.isInnerControl,Pm)

        val treeArea = areaOfPipe(reduce, 1)*Pm*Pr
        val reduceLength = latencyOfPipe(reduce)
        val treeDelayArea = reductionTreeDelays(Pm).map{dly => areaModel.areaOfDelayLine((reduceLength*dly).toInt, op.A.nbits, 1) }
                                                   .fold(NoArea){_+_}

        val loadResArea = areaOfCycle(loadRes, 1)*Pr*Pm
        val loadAccArea = areaOfCycle(loadAcc, Pr)
        val cycleArea   = areaOfCycle(reduce, Pr)
        val storeArea   = areaOfCycle(storeAcc, Pr)

        dbgs(s"MemReduce: $lhs (Pm = $Pm, Pr = $Pr)")
        dbgs(s" - Map:    $mapArea")
        dbgs(s" - Tree:   $treeArea")
        dbgs(s" - Delays: $treeDelayArea")
        dbgs(s" - Cycle:  ${loadResArea + loadAccArea + cycleArea + storeArea}")
        mapArea + treeArea + treeDelayArea + loadResArea + loadAccArea + cycleArea + storeArea + areaOf(lhs)

      case Switch(selects,body) =>
        val caseArea = areaOfBlock(body, lhs.isInnerControl, 1)

        dbgs(s"Switch: $lhs (#selects = ${selects.length})")
        dbgs(s" - Body: $caseArea")
        caseArea + areaOf(lhs)

      case StateMachine(en,start,notDone,action,nextState) =>
        val notDoneArea   = areaOfBlock(notDone,isInner = true,1)
        val actionArea    = areaOfBlock(action,lhs.isInnerControl,1)
        val nextStateArea = areaOfBlock(nextState,isInner = true,1)

        dbgs(s"State Machine: $lhs")
        dbgs(s" - Cond:   $notDoneArea")
        dbgs(s" - Action: $actionArea")
        dbgs(s" - Next:   $nextStateArea")
        notDoneArea + actionArea + nextStateArea + areaOf(lhs)

      case _ if inHw =>
        val blocks = rhs.blocks.map(blk => areaOfBlock(blk,false,1))
        val area = areaOf(lhs)
        dbgs(s"${lhs}: $area")
        blocks.zipWithIndex.foreach{case (blk,i) => dbgs(s" - Block #$i: $blk") }
        area + blocks.fold(NoArea){_+_}

      case _ => areaOf(lhs) + rhs.blocks.map(blk => areaOfBlock(blk,false,1)).fold(NoArea){_+_}
    }
    scopeArea = area +: scopeArea
  }

}
