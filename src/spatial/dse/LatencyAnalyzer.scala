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


case class LatencyAnalyzer(IR: State, latencyModel: LatencyModel) extends RerunTraversal with AccelTraversal {
  var cycleScope: List[Double] = Nil
  var intervalScope: List[Double] = Nil
  var totalCycles: Long = 0L

  override def silence(): Unit = {
    latencyModel.silence()
    super.silence()
  }

  override protected def preprocess[A](b: Block[A]): Block[A] = {
    cycleScope = Nil
    intervalScope = Nil
    latencyModel.reset()
    super.preprocess(b)
  }

  override protected def postprocess[A](b: Block[A]): Block[A] = {
    val CLK = spatialConfig.target.clockRate
    val startup = spatialConfig.target.baseCycles
    // TODO: Could potentially have multiple accelerator designs in a single program
    // Eventually want to be able to support multiple accel scopes
    totalCycles = cycleScope.sum.toLong + startup

    if (config.enDbg) {
      latencyModel.reportMissing()
    }

    dbgs(s"Estimated cycles: $totalCycles")
    dbgs(s"Estimated runtime (at " + "%.2f".format(CLK) +"MHz): " + "%.8f".format(totalCycles/(CLK*1000000f)) + "s")
    super.postprocess(b)
  }

  // TODO: Default number of iterations if bound can't be computed?
  // TODO: Warn user if bounds can't be found?
  // TODO: Move this elsewhere
  def nIters(x: Sym[CounterChain], ignorePar: Boolean = false): Long = x match {
    case Def(CounterChainNew(ctrs)) =>
      val loopIters = ctrs.map{
        case Def(CounterNew(start,end,stride,par)) =>
          val min = start.getBound.map(_.toInt).getOrElse{warn(start.ctx, s"Don't know bound of ${start}"); 0}.toDouble
          val max = end.getBound.map(_.toInt).getOrElse{warn(start.ctx, s"Don't know bound of ${end}"); 1}.toDouble
          val step = stride.getBound.map(_.toInt).getOrElse{warn(start.ctx, s"Don't know bound of ${stride}"); 1}.toDouble
          val p = par.getBound.map(_.toInt).getOrElse{warn(start.ctx, s"Don't know bound of ${par}"); 1}.toDouble

          val nIters = Math.ceil((max - min)/step)
          if (ignorePar)
            nIters.toLong
          else
            Math.ceil(nIters/p).toLong

        case Def(ForeverNew()) => 0L
      }
      loopIters.fold(1L){_*_}
  }

  def latencyOfBlock(b: Block[_], par_mask: Boolean = false): (List[Double], List[Double]) = {
    val outerCycles = cycleScope
    val outerIntervals = intervalScope
    cycleScope = Nil
    intervalScope = Nil
    getControlNodes(b).foreach{
      case s@Op(d) => visit(s.asInstanceOf[Sym[_]])
      case _ =>
    }

    val cycles = cycleScope
    val intervals = intervalScope
    cycleScope = outerCycles
    intervalScope = outerIntervals
    (cycles, intervals)
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    val (cycles, ii) = rhs match {
      case AccelScope(blk) if lhs.isInnerControl =>
        inAccel {
          val body = latencyOfPipe(blk)

          dbgs(s"Inner Accel $lhs: (D = $body)")
          dbgs(s"- body = $body")
          (body, 1.0)
        }


      case AccelScope(blk) if lhs.isOuterControl =>
        inAccel {
          val (latencies, iis) = latencyOfBlock(blk)
          val body = latencies.sum

          dbgs(s"Accel $lhs: (D = $body)")
          dbgs(s"- body = $body")
          (body, 1.0)
        }

      case ParallelPipe(en, func) =>
        val (latencies, iis) = latencyOfBlock(func, true)
        val delay = latencies.max + latencyOf(lhs)
        val ii = lhs.userII.getOrElse(iis.max)

        dbgs(s"Parallel $lhs: (II = $ii, D = $delay)")
        latencies.reverse.zipWithIndex.foreach{case (s,i) => dbgs(s"- $i. $s")}
        (delay, ii)

      // --- Pipe
      case UnitPipe(en, func) if lhs.isInnerControl =>
        val (latency, compilerII) = latencyAndInterval(func)
        val ii = lhs.userII.getOrElse(compilerII)

        val delay = latency + latencyOf(lhs)

        dbgs(s"Pipe $lhs: (II = $ii, D = $delay)")
        dbgs(s"- pipe = $latency")

        (delay, lhs.userII.getOrElse(ii))

      case OpForeach(en, cchain, func, iters) if lhs.isInnerControl =>
        val N = nIters(cchain)
        val (latency, compilerII) = latencyAndInterval(func)
        val ii = lhs.userII.getOrElse(compilerII)

        val delay = latency + (N - 1)*ii + latencyOf(lhs)

        dbgs(s"Foreach $lhs (N = $N, II = $ii, D = $delay):")
        dbgs(s"- pipe = $latency")

        (delay, 1.0)

      case OpReduce(en, cchain,accum,map,ld,reduce,store,_,_,iters) if lhs.isInnerControl =>
        val N = nIters(cchain)
        val P = cchain.constPars.product

        val fuseMapReduce = false //canFuse(map,reduce,rV,P)

        val (mapLat, mapII) = latencyAndInterval(map)

        val ldLat = latencyOfCycle(ld)
        val reduceLat = latencyOfCycle(reduce)
        val storeLat = latencyOfCycle(store)

        val treeLat = latencyOfPipe(reduce) * reductionTreeHeight(P)

        val cycle = ldLat + reduceLat + storeLat

        val compilerII = Math.max(mapII, cycle)
        val ii = lhs.userII.getOrElse(compilerII)
        val delay = mapLat + treeLat + (N - 1)*ii + latencyOf(lhs)

        dbgs(s"Reduce $lhs (N = $N, II = $ii, D = $delay):")
        dbgs(s"- map   = $mapLat (ii = $mapII)")
        dbgs(s"- tree  = $treeLat")
        dbgs(s"- cycle = $cycle")

        (delay, 1.0)

      case UnrolledForeach(en,cchain,func,iters,valids) if lhs.isInnerControl =>
        val N = nIters(cchain)
        val (pipe, compilerII) = latencyAndInterval(func)
        val ii = lhs.userII.getOrElse(compilerII)

        val delay = pipe + (N - 1)*ii + latencyOf(lhs)

        dbgs(s"Unrolled Foreach $lhs (N = $N, II = $ii, D = $delay):")
        dbgs(s"- pipe = $pipe")

        (delay, 1.0)

      case UnrolledReduce(en,cchain,func,iters,valids) if lhs.isInnerControl =>
        val N = nIters(cchain)

        val (body, compilerII) = latencyAndInterval(func)
        val ii = lhs.userII.getOrElse(compilerII)
        val delay = body + (N - 1)*ii + latencyOf(lhs)

        dbgs(s"Unrolled Reduce $lhs (N = $N, II = $ii, D = $delay):")
        dbgs(s"- body  = $body")

        (delay, 1.0)

      case StateMachine(en, start, notDone, action, nextState) if lhs.isInnerControl =>
        // TODO: Any way to predict number of iterations, or annotate expected number?
        val N = 1
        val (cont, contII) = latencyAndInterval(notDone)
        val (act, actII)   = latencyAndInterval(action)
        val (next, nextII) = latencyAndInterval(nextState)

        val ii = List(contII, actII, nextII).max
        val delay = cont + act + next + (N-1)*ii + latencyOf(lhs)

        dbgs(s"Inner FSM $lhs: (N = $N, II = $ii, D = $delay)")
        dbgs(s"-notDone = $cont (ii = $contII)")
        dbgs(s"-action  = $act (ii = $actII)")
        dbgs(s"-next    = $next (ii = $nextII)")

        (delay, 1.0)

      // --- Sequential
      case UnitPipe(en, func) if lhs.isOuterControl =>
        val (stages, iis) = latencyOfBlock(func)

        val delay = stages.sum + latencyOf(lhs)
        val compilerII = (1.0 +: iis).max
        val ii = lhs.userII.getOrElse(compilerII)

        dbgs(s"Outer Pipe $lhs: (D = $delay)")
        stages.reverse.zipWithIndex.foreach{case (s,i) => dbgs(s"- $i. $s")}

        (delay, ii)


      // --- Metapipeline and Sequential
      case OpForeach(en, cchain, func, _) =>
        val N = nIters(cchain)
        val (stages, iis) = latencyOfBlock(func)
        val compilerII = (1.0 +: iis).max
        val ii = lhs.userII.getOrElse(compilerII)

        val delay = if (lhs.isPipeControl) { stages.max * (N - 1)*ii + stages.sum + latencyOf(lhs) }
                    else                 { stages.sum * N + latencyOf(lhs) }

        dbgs(s"Outer Foreach $lhs: (N = $N, II = $ii, D = $delay)")
        dbgs(lhs.ctx)
        val stageNames = getControlNodes(func).map(s => s"$s")
        stages.reverse.zip(stageNames).zipWithIndex.foreach{case ((s,n),i) => dbgs(s"- $i. $n: $s")}

        (delay, 1.0)

      case OpReduce(en, cchain,accum,map,ld,reduce,store,_,_,iters) =>
        val N = nIters(cchain)
        val P = cchain.constPars.product

        val (mapStages, mapII) = latencyOfBlock(map)
        val internal = latencyOfPipe(reduce) * reductionTreeHeight(P)
        val cycle = latencyOfCycle(ld) + latencyOfCycle(reduce) + latencyOfCycle(store)

        val reduceStage = internal + cycle
        val stages = reduceStage +: mapStages
        val compilerII = (cycle +: mapII).max
        val ii = lhs.userII.getOrElse(compilerII)

        val delay = if (lhs.isPipeControl) { stages.max * (N - 1)*ii + stages.sum + latencyOf(lhs) }
                    else                 { stages.sum * N + latencyOf(lhs) }

        dbgs(s"Outer Reduce $lhs (N = $N, II = $ii, D = $delay):")
        dbgs(lhs.ctx)
        val stageNames = getControlNodes(map).map(s => s"$s") :+ "Reduce"
        stages.reverse.zip(stageNames).zipWithIndex.foreach{case ((s,n),i) => dbgs(s"- $i. $n: $s")}

        (delay, 1.0)

      case OpMemReduce(en, cchainMap,cchainRed,accum,map,ldRes,ldAcc,reduce,store,_,_,itersMap,itersRed) =>
        val Nm = nIters(cchainMap)
        val Nr = nIters(cchainRed)
        val Pm = cchainMap.constPars.product // Parallelization factor for map
        val Pr = cchainRed.constPars.product // Parallelization factor for reduce

        val (mapStages, mapIIs) = latencyOfBlock(map)
        val internal: Double = latencyOfPipe(ldRes) + latencyOfPipe(reduce) * reductionTreeHeight(Pm)
        val accumulate: Double = latencyOfCycle(ldAcc) + latencyOfCycle(reduce) + latencyOfCycle(store)
        val reduceStage: Double = internal + accumulate + (Nr - 1)*accumulate
        val stages =  reduceStage +: mapStages
        val mapII = (1.0 +: mapIIs).max
        val ii = lhs.userII.getOrElse(mapII)

        val delay = if (lhs.isPipeControl) { stages.max * (Nm - 1)*ii + stages.sum + latencyOf(lhs) }
                    else                 { stages.sum * Nm + latencyOf(lhs) }

        dbgs(s"Block Reduce $lhs (Nm = $Nm, IIm = $mapII, Nr = $Nr, IIr = $accumulate, D = $delay)")
        dbgs(lhs.ctx.content.map(_.trim).getOrElse(lhs.ctx.toString))
        dbgs(s"internal: $internal")
        dbgs(s"IIr:      $accumulate")
        dbgs(s"reduce:   $reduceStage = internal + IIr + (Nr-1)*IIr")
        val stageNames = getControlNodes(map).map(s => s"$s") :+ "Reduce"
        stages.reverse.zip(stageNames).zipWithIndex.foreach{case ((s,n),i) => dbgs(s"- $i. $n: $s")}

        (delay, 1.0)

      case UnrolledForeach(en,cchain,func,iters,valids) =>
        val N = nIters(cchain)
        val (stages, iis) = latencyOfBlock(func)
        val compilerII = (1.0 +: iis).max
        val ii = lhs.userII.getOrElse(compilerII)

        val delay = if (lhs.isPipeControl) { stages.max * (N - 1)*ii + stages.sum + latencyOf(lhs) }
                    else                 { stages.sum * N + latencyOf(lhs) }

        dbgs(s"Unrolled Outer Foreach $lhs (N = $N, II = $ii, D = $delay):")
        stages.reverse.zipWithIndex.foreach{case (s,i) => dbgs(s"- $i. $s")}

        (delay, 1.0)

      case UnrolledReduce(en,cchain,func,iters,valids) =>
        val N = nIters(cchain)
        val (stages, iis) = latencyOfBlock(func)
        val compilerII = (1.0 +: iis).max
        val ii = lhs.userII.getOrElse(compilerII)

        val delay = if (lhs.isPipeControl) { stages.max * (N - 1)*ii + stages.sum + latencyOf(lhs) }
                    else                 { stages.sum * N + latencyOf(lhs) }

        dbgs(s"Unrolled Outer Reduce $lhs (N = $N, II = $ii, D = $delay):")
        stages.reverse.zipWithIndex.foreach{case (s,i) => dbgs(s"- $i. $s")}

        (delay, 1.0)

      case StateMachine(en, start, notDone, action, nextState) =>
        val N = 1 // TODO
        val (cont, contII) = latencyAndInterval(notDone)
        val (acts, actIIs) = latencyOfBlock(action)
        val (next, nextII) = latencyAndInterval(nextState)

        val ii = (List(contII, nextII) ++ actIIs).max
        val delay = (cont + acts.sum + next) * N + latencyOf(lhs)

        dbgs(s"Outer FSM $lhs: (N = $N, II = $ii, D = $delay)")
        dbgs(s" - notDone = $cont")
        dbgs(s" - action: ")
        acts.reverse.zipWithIndex.foreach{case (s,i) => dbgs(s"   - $i. $s") }
        dbgs(s" - next    = $next")


        (delay, 1.0)

      case Switch(selects,body) =>
        val (stages, iis) = latencyOfBlock(body)
        val delay = stages.max
        val ii = (1.0 +: iis).max
        dbgs(s"Switch $lhs: (II = $ii, D = $delay)")
        stages.reverse.zipWithIndex.foreach{case (dly, i) => dbgs(s" - $i. $dly") }
        (delay, ii)

      case SwitchCase(body) if lhs.isInnerControl =>
        val (delay, ii) = latencyAndInterval(body)
        dbgs(s"Case $lhs: (II = $ii, D = $delay)")
        dbgs(s" - body: $delay")
        (delay, ii)

      case SwitchCase(body) if lhs.isOuterControl =>
        val (stages, iis) = latencyOfBlock(body)
        val delay = if (lhs.isSeqControl) stages.sum else (0.0 +: stages).max
        val ii = (1.0 +: iis).max
        dbgs(s"Case $lhs: (II = $ii, D = $delay)")
        stages.reverse.zipWithIndex.foreach{case (dly,i) => dbgs(s" - $i. $dly") }
        (delay, ii)

      case op: DenseTransfer[_,_,_] =>
        val delay = latencyOf(lhs)
        val name = if (op.isTileLoad) "Load" else "Store"
        dbgs(s"Dense$name $lhs: (D = $delay)")
        (delay,1.0)

      case op:SparseTransfer[_,_] =>
        val delay = latencyOf(lhs)
        val name = if (op.isTileLoad) "Load" else "Store"
        dbgs(s"Sparse$name $lhs: (D = $delay)")
        (delay,1.0)

      case _ =>
        // No general rule for combining blocks
        rhs.blocks.foreach{blk => visitBlock(blk) }
        val delay = latencyOf(lhs)
        (delay, 1.0)
    }
    cycleScope ::= cycles
    intervalScope ::= ii
  }



}
