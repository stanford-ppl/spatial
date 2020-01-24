package spatial.traversal

import argon._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.util.modeling._
import spatial.metadata.control._
import spatial.metadata.access._
import spatial.metadata.memory._
import spatial.metadata.bounds.Expect
import poly.SparseMatrix
import utils.implicits.collections._

case class IterationDiffAnalyzer(IR: State) extends AccelTraversal {

  private def findCycles(lhs: Sym[_], ctrl: Control[_]): Unit = {
    ctrl.bodies.foreach{body =>
      body.blocks.foreach{case (iters, block) =>
        val stms = block.stms
        val cycles = findAccumCycles(stms).accums
        if (cycles.nonEmpty) {
          dbgs(s"\n\nFound cycles in $lhs ($iters): ")
          cycles.foreach{c => dbgs(s"  $c")}
          cycles.collect{case AccumTriple(mem,reader,writer) if mem.isLocalMem && reader != writer && !mem.shouldIgnoreConflicts =>

            if (reader.affineMatrices.nonEmpty && writer.affineMatrices.nonEmpty) {
              val huge = 99999
              if (iters.nonEmpty) {
                // Figure out the minimum "ticks until exhaustion" of each iterator relative to itself.  One "tick" is 
                //   one cycle where the iterator increments.  Sorry about using MaxJ terminology here
                val accumIters = iters.dropWhile(!writer.affineMatrices.head.matrix.keys.contains(_))
                val selfRelativeData = iters.map{i => 
                  val p = i.ctrParOr1
                  // TODO: I think this will be incorrect if writer leads reader, but iteration count of cchain cannot be determined
                  (i.ctrStart, i.ctrEnd, i.ctrStep) match {
                    case (Expect(start), Expect(end), Expect(step)) => 
                      ((scala.math.ceil((end-start) / step) / p).toInt, step*p)
                    case _ => 
                      val step = i.ctrStep match {case Expect(st) => st; case _ => huge}
                      if (spatialConfig.enableLooseIterDiffs && i != accumIters.head) {
                        warn(s"Cannot determine lower bound for iteration difference on controller $lhs (${lhs.ctx}), but --looseIterDiffs flag is set!  Assuming you won't reduce on ${mem.ctx} so rapidly that you run into data correctness issues!")
                        (huge, step*p)
                      } else if (lhs.userII.isDefined) {
                        warn(s"Cannot determine lower bound for iteration difference on controller $lhs (${lhs.ctx}), but II for this controller is set so this is ok (II = ${lhs.userII.get}).")
                        (huge, step*p)
                      } else if (accumIters.size > 0 && i != accumIters.head) {
                        warn(s"Cannot determine lower bound for iteration difference on controller $lhs (${lhs.ctx})! You should:")
                        warn(s"    1) Set bounds for $i to static start/stop/step values")
                        warn(s"    2) Be ok with compiler using the most conservative possible II for this loop")
                        warn(s"    3) Compile with --looseIterDiffs flag to ignore potential loop-carry dependency issues on ${mem.ctx}")
                        warn(s"    4) Explicitly set II for this loop")
                        (1, step*p)
                      } else {
                        (1, step*p)
                      }
                  }
                }
                val selfRelativeTicks: Map[I32, Int] = iters.zip(selfRelativeData).map{case (i, d) => i -> d._1 }.toMap
                val strides: Map[I32, Int] = iters.zip(selfRelativeData).map{case (i, d) => i -> d._2 }.toMap

                // Figure out number of ticks of innermost iterator that each iterator takes to increment. 
                // TODO: What to do when iterators come from different levels of control hierarchy?
                val ticks: Map[I32, Int] = List.tabulate(iters.size){i => iters(i) -> selfRelativeTicks.values.drop(i+1).product }.toMap

                dbgs(s"Iter info:")
                iters.map{it => dbgs(s"$it:");dbgs(s"  ${selfRelativeTicks(it)} personal ticks to exhaust");dbgs(s"  ${ticks(it)} global ticks to increment");dbgs(s"  ${strides(it)} stride")}

                // Figure out distances between accesses 
                // TODO: Assumes last write lane of current tick and first read lane of next tick are closest in "tick-space."  i.e. no x(5-j) = x(j)
                val thisIterReads  = reader.affineMatrices.map(_.matrix)
                val thisIterWrites = writer.affineMatrices.map(_.matrix)
                // val nextIterReads  = reader.affineMatrices.map(_.matrix.increment(iters.last,1))

                def ticksToCoverDist(w: SparseMatrix[Idx], r: SparseMatrix[Idx]): Int = {
                  val diff = w - r
                  // Compute ticks required to cover this distance
                  val itersContributingToDim = r.rows.map{a => a.cols.collect{case (k,v) if v != 0 => k}}
                  val innermostIterNotContributing = iters.filter(!itersContributingToDim.flatten.contains(_)).sortBy(iters.indexOf(_)).reverse.headOption
                  val ticksToCoverDist = diff.collapse.zip(itersContributingToDim).zipWithIndex.map{case ((dist, its),i) => 
                    // Figure out which iterator closes gap the fastest. TODO: What if both are required to close it?
                    if (its.nonEmpty) {
                      val innermostDep = its.toList.maxBy(accumIters.indexOf(_))
                      (dist * ticks.getOrElse(innermostDep.asInstanceOf[I32], 1)) / strides.getOrElse(innermostDep.asInstanceOf[I32], 1)
                    } else 0
                  }.sum
                  val variantTicksPerInvariantTick = if (innermostIterNotContributing.isDefined) ticks(innermostIterNotContributing.get) else 0

                  // Minimum ticks between repeat addresses are issues is the smaller nonzero value of ticks due to distance and due to invariance
                  val ticksBeforeRepeatAddr = 
                    if (ticksToCoverDist > 0 && variantTicksPerInvariantTick > 0) variantTicksPerInvariantTick min ticksToCoverDist
                    else if (ticksToCoverDist == 0) variantTicksPerInvariantTick
                    else ticksToCoverDist
                  dbgs(s"Interference Info:")
                  dbgs(s"$ticksBeforeRepeatAddr required before a repeat address is seen ($ticksToCoverDist ticks to cover gap, $variantTicksPerInvariantTick before invariant iter ($innermostIterNotContributing) increments")
                  dbgs(s"write: ${w}")
                  dbgs(s"read: ${r}")
                  dbgs(s"diff: ${diff}")
                  ticksBeforeRepeatAddr
                }

                def corners(writers: Seq[SparseMatrix[Idx]], readers: Seq[SparseMatrix[Idx]])(func: (SparseMatrix[Idx],SparseMatrix[Idx]) => Int): List[Int] = {
                  List(func(writers.head, readers.head), func(writers.last, readers.head), func(writers.head, readers.last), func(writers.last, readers.last))
                }

                // Figure out worst-case-scenario number of ticks (i.e. smallest positive value) for the very first reader to catch up to a writer.
                //   To get worst case, check every combination of the four corners of iteration space (write lane 0, write lane N, read lane 0, read lane N)
                //   If all values are 0, then take 0
                val minTicksPairings = corners(thisIterWrites, thisIterReads){(w,r) => ticksToCoverDist(w, r)}
                val minTicksToOverlap = if (minTicksPairings.forall(_ == 0)) 0 else minTicksPairings.filter(_ != 0).min
                dbgs(s"minTickslist is $minTicksPairings")
                dbgs(s"This accumulation needs result written $minTicksToOverlap (or more) ticks prior")

                reader.iterDiff = if (reader.getIterDiff.isDefined && reader.iterDiff != 0) {reader.iterDiff min minTicksToOverlap} else minTicksToOverlap
                writer.iterDiff = if (writer.getIterDiff.isDefined && writer.iterDiff != 0) {writer.iterDiff min minTicksToOverlap} else minTicksToOverlap
                mem.iterDiff = if (mem.getIterDiff.isDefined && mem.iterDiff != 0) {mem.iterDiff min minTicksToOverlap} else minTicksToOverlap

                // Check for segmentation due to inter-lane conflicts
                if (thisIterReads.size > 1) {
                  // iterDiff within iter
                  /* 
                            Here are the motivating examples used to get to this point

                                              Foreach(N by 1 par 2){i => mem(i) = mem(i-1)}
                          MEM     O   O   O   O 
                         ACCESS   |___^|__^
                                LANE RETIMING       0   1 
                                      |   
                                      |
                                          |
                                          |     II  = lat
                                                lat = 2 * single lane's latency 
                                                segmentMapping = Map( 0 -> 0, 1 -> 1 )
                                              Foreach(N by 1 par 3){i => mem(i) = mem(i-2)}

                                  O   O   O   O   O
                                  |___|___^|  ^   ^
                                      |____|__|   |
                                           |______|
                                                                      
                   LANE RETIMING           0   1  2                                                 
                                           |   |                                                 
                                           |   |                                               
                                                  |                                                
                                                  |
                                                    II  = lat
                                                    lat = 2 * single lane's latency 
                                                    segmentMapping = Map( 0 -> 0, 1 -> 0, 2 -> 1)
                  */

                  // For each reader, figure out first writer who requires the same address and push to next available segment
                  
                  val segments = scala.collection.mutable.HashMap[Int, Int](0 -> 0)
                  val existingSegments = reader.segmentMapping
                  thisIterReads.drop(1).zipWithIndex.foreach{case (r,i) => 
                    val laneDep = thisIterWrites.take(i+1).zipWithIndex.collectFirst{case (w, j) if (w-r).collapse.forall(_ == 0) => j}.getOrElse(-1)
                    val segmentDep = segments.getOrElse(laneDep,-1) + 1
                    val existingDep = existingSegments.getOrElse(i+1, 0)
                    segments += ({i + 1} -> {existingDep max segmentDep})
                  }
                  dbgs(s"Establishing segmentation")
                  segments.foreach{case (lane, segment) => 
                    dbgs(s"Lane $lane is part of segment $segment")
                  }
                  reader.segmentMapping = segments.toMap
                  writer.segmentMapping = segments.toMap
                  mem.segmentMapping = segments.toMap
                }
                
              }
            }
          }
        }
      }
    }

 }

  private var _progorder = 0
  def setProgramOrder(lhs:Sym[_]) = {
    lhs.progorder = _progorder
    _progorder += 1
  }
  def resetProgramOrder = { _progorder = 0 }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _:AccelScope => 
      inAccel{ super.visit(lhs, rhs) }
      resetProgramOrder
    case _:BlackboxImpl[_,_,_] => inBox{ super.visit(lhs, rhs) }

    case ctrl: Control[_] => 
      lhs match {
        case Op(OpMemReduce(_,_,_,accum,_,_,loadAcc,_,storeAcc,_,_,_,_,_)) => 
          // Known that the accum cycle has iterDiff of 0 (touch-and-go)
          accum.asInstanceOf[Sym[_]].iterDiff = 0
          loadAcc.result.iterDiff = 0
          storeAcc.result.iterDiff = 0
        case Op(OpReduce(_,_,accum,_,load,_,store,_,_,_,_)) =>
          // Known that the accum cycle has iterDiff of 1 (always reading and writing to same place)
          accum.asInstanceOf[Sym[_]].iterDiff = 1
          load.result.iterDiff = 1
          store.result.iterDiff = 1
          super.visit(lhs,rhs)
        case _ =>
      }
      findCycles(lhs, ctrl)
      setProgramOrder(lhs)
      super.visit(lhs,rhs)
      
    case _ => 
      if (inHw) setProgramOrder(lhs)
      super.visit(lhs, rhs)
  }

}
