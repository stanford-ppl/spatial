package spatial.util

import argon._
import argon.node._
import forge.tags.stateful
import models.Area
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.targets.{AreaModel, HardwareTarget, LatencyModel}

import utils.implicits.collections._

import scala.collection.mutable



object modeling {
  private var fmaAccumRes = Set[(Sym[_], Double)]()

  def blockNestedScheduleAndResult(block: Block[_]): (Seq[Sym[_]], Seq[Sym[_]]) = {
    val schedule = block.nestedStms.filter{e => e.isBits | e.isVoid }
    val result   = (block +: schedule.flatMap{_.blocks}).flatMap{b => exps(b) }
    (schedule, result)
  }

  /** Returns all nodes on all paths from start --> end, including start and end
    * If there are no such paths, returns an empty set.
    */
  def getAllNodesBetween(start: Sym[_], end: Sym[_], scope: Set[Sym[_]]): Set[Sym[_]] = {
    def inputsDfs(frontier: Set[Sym[_]], nodes: Set[Sym[_]]): Set[Sym[_]] = frontier.flatMap{x: Sym[_] =>
      if (scope.contains(x)) {
        if (x == start) nodes + x
        else inputsDfs(x.inputs.toSet, nodes + x)
      }
      else Set.empty[Sym[_]]
    }
    inputsDfs(Set(end),Set(end))
  }

  @stateful def target: HardwareTarget = spatialConfig.target
  @stateful def areaModel: AreaModel = spatialConfig.target.areaModel
  @stateful def NoArea: Area = areaModel.NoArea
  @stateful def latencyModel: LatencyModel = spatialConfig.target.latencyModel

  @stateful def latencyOf(e: Sym[_], inReduce: Boolean = false): Double = {
    if (spatialConfig.enableRetiming) latencyModel.latencyOf(e, inReduce)
    else if (latencyModel.requiresRegisters(e, inReduce)) 0
    else latencyModel.latencyOf(e, inReduce)
  }
  @stateful def builtInLatencyOf(e: Sym[_]): Double = latencyModel.builtInLatencyOfNode(e)

  @stateful def latencyOfCycle(b: Block[_]): (Double, Double) = latencyOfPipe(b, inReduce = true)

  @stateful def latencyOfPipe(
    block:    Block[_],
    inReduce: Boolean = false,
    verbose:  Boolean = false
  ): (Double, Double) = latencyAndInterval(block, inReduce, verbose)

  @stateful def latencyAndInterval(
    block:    Block[_],
    inReduce: Boolean = false,
    verbose:  Boolean = false
  ): (Double, Double) = {
    val (latencies, cycles) = latenciesAndCycles(block, verbose = verbose)
    val scope = latencies.keySet

    val latency = latencies.values.fold(0.0){(a,b) => Math.max(a,b) }
    // TODO: Safer way of determining if THIS cycle is the reduceType
    val interval = (cycles.map{c =>
      // Sketchy things for issue #63
      val scopeContainsSpecial = scope.exists(x => x.reduceType.contains(FixPtFMA) )
      val cycleContainsSpecial = c.symbols.exists{case Op(FixFMA(_,_,_)) => true; case _ => false}
      if (cycleContainsSpecial && scopeContainsSpecial && spatialConfig.enableOptimizedReduce) 1 else c.length
    } + 0).max
    // HACK: Set initiation interval to 1 if it contains a specialized reduction
    // This is a workaround for chisel codegen currently specializing and optimizing certain reduction types
    val compilerII = interval
    (latency, compilerII)
  }


  @stateful def latenciesAndCycles(block: Block[_], verbose: Boolean = false): (Map[Sym[_],Double], Set[Cycle]) = {
    val (schedule, result) = blockNestedScheduleAndResult(block)
    pipeLatencies(result, schedule, verbose = verbose)
  }

  case class AccessPair(mem: Sym[_], access: Sym[_])
  case class AccumTriple(mem: Sym[_], read: Sym[_], write: Sym[_])

  case class ScopeAccumInfo(
    readers: Map[Sym[_],Set[Sym[_]]], // Memory -> readers
    writers: Map[Sym[_],Set[Sym[_]]], // Memory -> writers
    accums:  Set[AccumTriple],
    cycles:  mutable.Map[Sym[_], mutable.Set[AccumTriple]]
  )

  @stateful def findAccumCycles(scope: Set[Sym[_]], verbose: Boolean = false): ScopeAccumInfo = {
    val cycles = mutable.HashMap[Sym[_],mutable.Set[AccumTriple]]()
    def addCycle(sym: Sym[_], triple: AccumTriple): Unit = {
      val set = cycles.getOrElseAdd(sym, () => mutable.HashSet.empty)
      set += triple
    }

    val readers = scope.collect{
      case reader @ Reader(mem,_,_)     => AccessPair(mem, reader)
      case reader @ StatusReader(mem,_) => AccessPair(mem, reader)
      case reader @ BankedReader(mem,_,_,_) => AccessPair(mem, reader)
    }
    val writers = scope.collect{
      case writer @ Writer(mem,_,_,_)     => AccessPair(mem, writer)
      case writer @ DequeuerLike(mem,_,_) => AccessPair(mem, writer)
      case writer @ BankedWriter(mem,_,_,_,_)     => AccessPair(mem, writer)
    }
    val readersByMem = readers.groupBy(_.mem).filter{x => x._2.size > 1 | writers.map(_.mem).contains(x._1)}.mapValues(_.map(_.access))
    val writersByMem = writers.groupBy(_.mem).filter{x => x._2.size > 1 | readers.map(_.mem).contains(x._1)}.mapValues(_.map(_.access))
    val memories = readersByMem.keySet intersect writersByMem.keySet
    val accums = memories.flatMap{mem =>
      val rds = readersByMem(mem)
      val wrs = writersByMem(mem)
      rds.cross(wrs).flatMap{case (rd, wr) =>
        lazy val triple = AccumTriple(mem, rd, wr)
        val path = getAllNodesBetween(rd, wr, scope)
        path.foreach{sym => addCycle(sym, triple) }

        if (verbose && path.nonEmpty) {
          dbgs("Found cycle between: ")
          dbgs(s"  ${stm(wr)}")
          dbgs(s"  ${stm(rd)}")
          path.foreach{node => dbgs(s"    ${stm(node)}") }
        }

        if (path.nonEmpty) Some(triple) else None
      }
    }

    ScopeAccumInfo(readersByMem, writersByMem, accums, cycles)
  }


  @stateful def pipeLatencies(
    result:   Seq[Sym[_]],
    schedule: Seq[Sym[_]],
    oos:      Map[Sym[_],Double] = Map.empty,
    verbose:  Boolean = false
  ): (Map[Sym[_],Double], Set[Cycle]) = {

    dbgs(s"----------------------------------")
    dbgs(s"Computing pipeLatencies for scope:")
    schedule.foreach{e => dbgs(s"  ${stm(e)}")}

    val scope = schedule.toSet

    val accumInfo = findAccumCycles(scope,verbose)
    val accums      = accumInfo.accums
    val accumReads  = accums.map(_.read)
    val accumWrites = accums.map(_.write)
    val knownCycles = accumInfo.cycles

    val paths  = mutable.HashMap[Sym[_],Double]() ++ oos
    val cycles = mutable.HashMap[Sym[_],Set[Sym[_]]]()

    accumReads.foreach{reader => dbgs(s"$reader is part of an accum cycle");cycles(reader) = Set(reader) }

    def fullDFS(cur: Sym[_]): Double = cur match {
      case Op(d) if scope.contains(cur) =>
        // Handles effect scheduling, even though there's no data to pass
        val deps = scope intersect cur.allDeps.toSet

        if (deps.nonEmpty) {
          val dlys = deps.map{e => paths.getOrElseAdd(e, () => fullDFS(e)) }

          // Primitives are not allowed to be loops, so the latency of nested symbols
          // must be some function of its blocks, e.g. the max of all or the sum of all
          // (For now, all cases are just the max of all inputs)
          val critical = d match {case _ => dlys.max }

          val cycleSyms = deps intersect cycles.keySet
          if (cycleSyms.nonEmpty) {
            cycles(cur) = cycleSyms.flatMap(cycles) + cur
            dbgs(s"cycle deps of $cur: ${cycles(cur)}")
          }

          val inReduce = knownCycles.contains(cur)

          // TODO[3]: + inputDelayOf(cur) -- factor in delays which are external to reduction cycles
          val delay = critical + latencyOf(cur, inReduce)

          if (verbose) dbgs(s"[$delay = max(" + dlys.mkString(", ") + s") + ${latencyOf(cur, inReduce)}] ${stm(cur)}" + (if (inReduce) "[cycle]" else ""))
          delay
        }
        else {
          val inReduce = knownCycles.contains(cur)
          val delay = latencyOf(cur, inReduce)
          if (verbose) dbgs(s"[$delay = max(0) + ${latencyOf(cur, inReduce)}] ${stm(cur)}" + (if (inReduce) "[cycle]" else ""))
          delay
        }

      case s => paths.getOrElse(s, 0) // Get preset out of scope delay, or assume 0 offset
    }

    // Perform backwards pass to push unnecessary delays out of reduction cycles
    // This can create extra registers, but decreases the initiation interval of the cycle
    def reverseDFS(cur: Sym[_], cycle: Set[Sym[_]]): Unit = cur match {
      case s: Sym[_] if cycle contains cur =>
        val forward = s.consumers intersect scope
        if (forward.nonEmpty) {
          if (verbose) dbgs(s"${stm(s)} [${paths.getOrElse(s,0L)}]")

          val earliestConsumer = forward.map{e =>
            val in = paths.getOrElse(e, 0.0) - latencyOf(e, inReduce=cycle.contains(e))
            if (verbose) dbgs(s"  [$in = ${paths.getOrElse(e, 0L)} - ${latencyOf(e,inReduce = cycle.contains(e))}] ${stm(e)}")
            in
          }.min

          val push = Math.max(earliestConsumer, paths.getOrElse(cur, 0.0))

          if (verbose) dbgs(s"  [$push]")

          paths(cur) = push
        }
        s.allDeps.foreach{in => reverseDFS(in, cycle) }

      case _ => // Do nothing
    }

    if (scope.nonEmpty) {
      // Perform forwards pass for normal data dependencies
      result.foreach{e => paths.getOrElseAdd(e, () => fullDFS(e)) }

      // TODO[4]: What to do in case where a node is contained in multiple cycles?
      accumWrites.toList.zipWithIndex.foreach{case (writer,i) =>
        val cycle = cycles.getOrElse(writer, Set.empty)
        if (verbose) dbgs(s"Cycle #$i: ")
        reverseDFS(writer, cycle)
      }
    }

    val warCycles = accums.collect{case AccumTriple(mem,reader,writer) if (!mem.isSRAM || {reader.parent.s.isDefined && reader.parent.parent.s.isDefined && {reader.parent.parent.s.get match {case Op(_:UnrolledReduce) => false; case _ => true}}}) => // Hack to specifically catch problem mentioned in #61
      val symbols = cycles(writer)
      val cycleLengthExact = paths(writer).toInt - paths(reader).toInt
      // Sketchy thing for issue #63
      val scopeContainsSpecial = scope.exists(x => x.reduceType.contains(FixPtFMA) )
      val cycleContainsSpecial = symbols.exists{case Op(FixFMA(_,_,_)) => true; case _ => false}
      if (cycleContainsSpecial && scopeContainsSpecial && spatialConfig.enableOptimizedReduce) {
        val (mul1,mul2,fma) = symbols.collect{case fma@Op(FixFMA(mul1,mul2,_)) => (mul1,mul2,fma)}.head
        val data = symbols.collect{case Op(RegWrite(_,d,_)) => d}.head
        fmaAccumRes += ((data, cycleLengthExact.toDouble))
        symbols.foreach{x => x.reduceType = Some(FixPtFMA); x.fmaReduceInfo = (data, mul1, mul2, fma, cycleLengthExact.toDouble)}
      }

      // TODO[2]: FIFO/Stack operations need extra cycle for status update?
      val cycleLength = if (reader.isStatusReader) cycleLengthExact + 1.0 else cycleLengthExact
      WARCycle(reader, writer, mem, symbols, cycleLength)
    }

    def consumersDfs(frontier: Set[Sym[_]], nodes: Set[Sym[_]]): Set[Sym[_]] = frontier.flatMap{x: Sym[_] =>
      if (scope.contains(x) && !nodes.contains(x)) {
        consumersDfs(x.consumers, nodes + x)
      }
      else nodes
    }

    def pushMultiplexedAccesses(accessors: Map[Sym[_],Set[Sym[_]]]) = accessors.flatMap{case (mem,accesses) =>
      dbgs(s"Multiplexed accesses for memory $mem: ")
      accesses.foreach{access => dbgs(s"  ${stm(access)}") }

      // NOTE: After unrolling there should be only one mux index per access
      // unless the common parent is a Switch
      val instances = mem.duplicates.length
      (0 until instances).map{id =>
        val accs = accesses.filter(_.dispatches.values.exists(_.contains(id)))

        val muxPairs = accs.map{access =>
          val muxes = access.ports(0).values.map(_.muxPort)
          (access, paths.getOrElse(access,0.0), muxes.maxOrElse(0))
        }.toSeq

        val length = muxPairs.map(_._3).maxOrElse(0) + 1

        // Keep accesses with the same mux index together, even if they have different delays
        // TODO[1]: This isn't quite right - should order by common parent instead?
        val groupedMuxPairs = muxPairs.groupBy(_._3)  // Group by maximum mux port
        val orderedMuxPairs = groupedMuxPairs.values.toSeq.sortBy{pairs => pairs.map(_._2).max }
        var writeStage = 0.0
        orderedMuxPairs.foreach{pairs =>
          val dlys = pairs.map(_._2) :+ writeStage
          val writeDelay = dlys.max
          writeStage = writeDelay + 1
          pairs.foreach{case (access, dly, _) =>
            val oldPath = paths(access)
            paths(access) = writeDelay
            dbgs(s"Pushing ${stm(access)} by ${writeDelay-oldPath} to $writeDelay due to muxing.")
            if (writeDelay-oldPath > 0) {
              dbgs(s"  Also pushing these by ${writeDelay-oldPath}:")
              // Attempted fix for issue #54. Not sure how this interacts with cycles
              val affectedNodes = consumersDfs(access.consumers, Set()) intersect scope
              affectedNodes.foreach{x => 
                dbgs(s"  $x")
                paths(x) = paths(x) + (writeDelay-oldPath)
              }
            }
          }
        }

        AAACycle(accesses, mem, length)
      }
    }

    def pushOptimizedReduce(): Unit = { // Issue #63 sketchiness
      fmaAccumRes.foreach{case (muxNode, ii) => 
        val extraLatency = scala.math.ceil(scala.math.log(ii)/scala.math.log(2)) + 1
        val affectedNodes = consumersDfs(muxNode.consumers, Set()) intersect scope
        affectedNodes.foreach{x => 
          dbgs(s"Pushing node $x by $extraLatency due to fma accumulator optimization")
          paths(x) = paths(x) + extraLatency
        }
      }
    }

    val wawCycles = pushMultiplexedAccesses(accumInfo.writers)
    val rarCycles = pushMultiplexedAccesses(accumInfo.readers)
    pushOptimizedReduce()  // Issue #63 sketchiness
    val allCycles: Set[Cycle] = (wawCycles ++ rarCycles ++ warCycles).toSet
    dbgs(s"Found cycles: ")
    allCycles.foreach{x => dbgs(s"$x")}

    if (verbose) {
      def dly(x: Sym[_]) = paths.getOrElse(x, 0.0)
      dbgs(s"  Schedule after pipeLatencies calculation:")
      schedule.sortWith{(a,b) => dly(a) < dly(b)}.foreach{node =>
        dbgs(s"  [${dly(node)}] ${stm(node)}")
      }
    }

    (paths.toMap, allCycles)
  }

}
