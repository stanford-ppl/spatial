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
        val cycles = findAccumCycles(stms.toSet).accums
        if (cycles.nonEmpty) {
          dbgs(s"\n\nFound cycles in $lhs ($iters): ")
          cycles.foreach{c => dbgs(s"  $c")}
          cycles.collect{case AccumTriple(mem,reader,writer) if (mem.isLocalMem && reader != writer) => 

            val read = reader.affineMatrices.head.matrix
            val write = writer.affineMatrices.head.matrix
            val diff = write - read
            if (iters.nonEmpty) {
              val stride = iters.last.ctrStep match {case Expect(c) => Some(c.toInt); case _ => None}
              val par = iters.last.ctrParOr1
              if (stride.isDefined) {
                // iterDiff between iters
                val thisIterReads  = List.tabulate(par){i => read.incrementConst(i*stride.get)}  // TODO: Replace this with access.affineMatrices.map(_.matrix)?
                val thisIterWrites = List.tabulate(par){i => write.incrementConst(i*stride.get)}  // TODO: Replace this with access.affineMatrices.map(_.matrix)?
                val nextIterReads  = List.tabulate(par){i => read.increment(iters.last,1).incrementConst(i*stride.get)}
                val diff = thisIterWrites.last - thisIterReads.head // How far is the last write from the first read?
                val advancePerInc = nextIterReads.head - thisIterReads.head // How far do we advance in one tick?
                val minIterDiff = diff.collapse.zip(advancePerInc.collapse).map{case (a,b) => if(a != 0 && b == 0) 0 else if(a == 0 && b == 0) 1 else  a / b}.sorted.headOption
                dbgs(s"Iteration Diff = ${minIterDiff}")
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
                                                  laneWaits = Map( 0 -> 0, 1 -> 1 )
              
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
                                                      laneWaits = Map( 0 -> 0, 1 -> 0, 2 -> 1)
                    */
                    val advancePerLane = (thisIterReads(1) - thisIterReads.head).collapse.max
                    val diffPerLane = (write-read).collapse.max
                    if (diffPerLane > 0) { // diffPerLane = 0 is a case of mem(i) = mem(i) + ...
                      val dependsOnLane = List.tabulate(par){i => i * advancePerLane - diffPerLane}
                      val laneWaitMapping = dependsOnLane.zipWithIndex.map{case(d,i) =>
                        val laneWait = if (d < 0) 0 else {
                          ((0 max dependsOnLane(i))/diffPerLane).toInt + 1
                        }
                        (i -> laneWait)
                      }.toMap
                      dbgs(s"advancePerLane = $advancePerLane, diffPerLane = $diffPerLane, lane dependencies $dependsOnLane")
                      dbgs(s"laneWaits = ${laneWaitMapping}")
                      reader.laneWaits = laneWaitMapping
                      writer.laneWaits = laneWaitMapping
                      mem.laneWaits = laneWaitMapping
                      // TODO: Tie this in to unrolling
                      if (laneWaitMapping.values.toList.distinct.length > 1) throw new Exception("Inter-lane access dependencies in cycles not yet supported")
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
        case Op(OpMemReduce(_,_,_,accum,_,_,loadAcc,_,storeAcc,_,_,_,_)) => 
          // Known that the accum cycle has iterDiff of 0 (touch-and-go)
          accum.asInstanceOf[Sym[_]].iterDiff = 0
          loadAcc.result.iterDiff = 0
          storeAcc.result.iterDiff = 0
        case Op(OpReduce(_,_,accum,_,load,_,store,_,_,_)) =>
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
