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
import utils.implicits.collections._

case class IterationDiffAnalyzer(IR: State) extends AccelTraversal {


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

  private def findCycles(lhs: Sym[_], ctrl: Control[_]): Unit = {
    ctrl.bodies.foreach{body =>
      body.blocks.foreach{case (iters, block) =>
        val stms = block.stms
        val cycles = findAccumCycles(stms).accums
        if (cycles.nonEmpty) {
          dbgs(s"\n\nFound cycles in $lhs ($iters): ")
          cycles.foreach{c => dbgs(s"  $c")}
          val allWritePositions = cycles.map(_.write.affineMatrices.head.matrix.collapse.sorted.headOption.getOrElse(0))
          cycles.collect{case AccumTriple(mem,reader,writer) if (mem.isLocalMem && reader != writer) => 

            if (reader.affineMatrices.nonEmpty && writer.affineMatrices.nonEmpty) {
              val read = reader.affineMatrices.head.matrix
              val write = writer.affineMatrices.head.matrix
              val diff = write - read
              if (iters.nonEmpty) {
                val stride = iters.last.ctrStep match {case Expect(c) => Some(c.toInt); case _ => None}
                val par = iters.last.ctrParOr1
                if (stride.isDefined) {
                  // iterDiff between iters
                  val thisIterReads  = reader.affineMatrices.map(_.matrix)
                  val thisIterWrites = writer.affineMatrices.map(_.matrix)
                  val nextIterReads  = reader.affineMatrices.map(_.matrix.increment(iters.last,1))
                  val diff = thisIterWrites.last - thisIterReads.head // How far is the last write from the first read?
                  val advancePerInc = nextIterReads.head - thisIterReads.head // How far do we advance in one tick?

                  // Figure out how many iterations pass between writing to an addr and then needing that addr due to iterator advancement
                  val dynamicDiff = diff.collapse.zip(advancePerInc.collapse).map{case (a,b) => 
                    if(a != 0 && b == 0) 0       // i.e. A(i,j) = A(i-1,j) + 1, relative to iterator i advances "0" per iter, reads from 1 ago each iter
                    else if(a == 0 && b == 0) 1  // i.e. A(0) = A(0) + 1, iterators involved in access don't increment with cchain AND don't change from iter to iter
                    else  a / b}
                  .sorted.headOption

                  // Also figure out how many iterations pass before an addr is used a second time (i.e. Foreach(0::L,0::M,0::N){(l,m,n) => A(l,n) = A(l,n) + 1} requires A(l,n) every N iters)
                  dbgs(s"iterators $iters, keys ${thisIterWrites.head.keys}")
                  val repeatDist: Option[Int] = if (!iters.forall(thisIterWrites.head.keys.contains)) {
                    val missingIterator = iters.reverse.collectFirst{case iter if !thisIterWrites.head.keys.contains(iter) => iter}.get
                    val otherIters = iters.reverse.takeWhile(_ != missingIterator)//.filter(_ != missingIterator)
                    dbgs(s"$missingIterator is missing!  Iter Diff could be dependent on $otherIters")
                    val starts: Seq[Option[Int]] = otherIters.map(_.ctrStart match {case Expect(c) => Some(c.toInt); case _ => None})
                    val stops: Seq[Option[Int]] = otherIters.map(_.ctrEnd match {case Expect(c) => Some(c.toInt); case _ => None})
                    val steps: Seq[Option[Int]] = otherIters.map(_.ctrStep match {case Expect(c) => Some(c.toInt); case _ => None})
                    val pars: Seq[Int] = otherIters.map(_.ctrParOr1)
                    if (starts.forall(_.isDefined) && stops.forall(_.isDefined) && steps.forall(_.isDefined)) {
                      Some((starts,stops,steps).zipped.toList.zip(pars).map{case ((s,e,st),p) => (scala.math.ceil((e.get - s.get) / (st.get)) / p).toInt}.product)
                    }
                    else if (spatialConfig.enableLooseIterDiffs) {
                      warn(s"Cannot determine lower bound for iteration difference on controller $lhs (${lhs.ctx}), but --looseIterDiffs flag is set!  Assuming you won't reduce on ${mem.ctx} so rapidly that you run into data correctness issues!")
                      None
                    } else if (lhs.userII.isDefined) {
                      warn(s"Cannot determine lower bound for iteration difference on controller $lhs (${lhs.ctx}), but II for this controller is set so this is ok (II = ${lhs.userII.get}).")
                      None
                    } else {
                      warn(s"Cannot determine lower bound for iteration difference on controller $lhs (${lhs.ctx})! You should:")
                      warn(s"    1) Set iterators $otherIters to static start/stop/step values")
                      warn(s"    2) Be ok with compiler using the most conservative possible II for this loop")
                      warn(s"    3) Compile with --looseIterDiffs flag to ignore potential loop-carry dependency issues on ${mem.ctx}")
                      warn(s"    4) Explicitly set II for this loop")
                      Some(1)
                    }
                  } else None

                  val minIterDiff = if (repeatDist.isDefined && dynamicDiff.isDefined && dynamicDiff.get != 0) Some(repeatDist.get min dynamicDiff.get)
                                    else if (repeatDist.isDefined && dynamicDiff.isDefined) repeatDist
                                    else if (dynamicDiff.isDefined) dynamicDiff
                                    else if (repeatDist.isDefined) repeatDist
                                    else None
                  dbgs(s"Each iter needs result written ${minIterDiff} (or more) iters ago (i.e. Iter Diff = ${minIterDiff} (dynamic: $dynamicDiff min repeat: $repeatDist)")
                  if (minIterDiff.isDefined) {
                    reader.iterDiff = minIterDiff.get
                    writer.iterDiff = minIterDiff.get
                    mem.iterDiff = minIterDiff.get                  
                    if (par > 1) { 
                      // iterDiff within iter
                      /* 
                          TODO: This metadata probably needs to be worked on better.  Here
                                are the motivating examples used to get to this point

                
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

                      // Want to figure out if this read at an upper lane has any overlap with the write range of a previous lane

                      val upperLaneStart = thisIterReads(1).collapse.max
                      val overlapLimit = if (advancePerInc.collapse.max > 0) allWritePositions.max else allWritePositions.min
                      // If the diff within a lane requires data from a previous lane, we must segment
                      if ((advancePerInc.collapse.max > 0 && upperLaneStart <= overlapLimit) || (advancePerInc.collapse.max < 0 && upperLaneStart <= overlapLimit)) { 
                        // Figure out how many times to rewind loop until the write bounds contain the upperLaneStart
                        val dependsOnRelativeIter = (upperLaneStart - overlapLimit) / (advancePerInc.collapse.max/par) - 1
                        val segMapping = List.tabulate(par){i =>
                          val segment = 0 max {i + dependsOnRelativeIter + 1}
                          (i -> segment)
                        }.toMap
                        dbgs(s"upperLaneStart = $upperLaneStart, advancePerInc = ${advancePerInc.collapse.max}, relativeIter = $dependsOnRelativeIter")
                        dbgs(s"segmentMapping = ${segMapping}")
                        reader.segmentMapping = segMapping
                        writer.segmentMapping = segMapping
                        mem.segmentMapping = segMapping
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

 }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _:AccelScope => inAccel{ super.visit(lhs, rhs) }

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
      super.visit(lhs,rhs)
      
    case _ => super.visit(lhs, rhs)
  }

}
