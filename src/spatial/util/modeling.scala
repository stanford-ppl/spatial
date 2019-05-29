package spatial.util

import argon._
import argon.node._
import forge.tags.stateful
import models.Area
import spatial.lang._
import spatial.node._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.targets.{AreaModel, HardwareTarget, LatencyModel}

import utils.implicits.collections._

import scala.collection.mutable
import scala.collection.immutable.SortedSet

object modeling {

  def consumersDfs(frontier: Set[Sym[_]], nodes: Set[Sym[_]], scope: Set[Sym[_]]): Set[Sym[_]] = frontier.flatMap{x: Sym[_] =>
    if (scope.contains(x) && !nodes.contains(x)) {
      consumersDfs(x.consumers, nodes + x, scope)
    }
    else nodes
  }

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

  @stateful def latencyOfCycle(b: Block[_]): Double = latencyOfPipe(b, inCycle = true)

  @stateful def latencyOfPipe(
    block:   Block[_],
    inCycle: Boolean = false,
    verbose: Boolean = false
  ): Double = latencyAndInterval(block, inCycle, verbose)._1

  @stateful def latencyAndInterval(
    block:   Block[_],
    inCycle: Boolean = false,
    verbose: Boolean = false
  ): (Double, Double) = {
    val (latencies, cycles) = latenciesAndCycles(block, verbose = verbose)
    val scope = latencies.keySet
    val latency = latencies.values.fold(0.0){(a,b) => Math.max(a,b) }
    // TODO: Safer way of determining if THIS cycle is the reduceType
    val interval = (cycles.map{c => c.length} + 0).max
    // Combine segmented cycleSyms
    val segmentedInterval = cycles.filter(_.memory.segmentMapping.size > 1).filter(_.isInstanceOf[WARCycle])
                                  .groupBy(_.memory)
                                  .map{case (mem, cycs) => cycs.toList.map{c => c.length}.sum}.toList
                                  .sorted.reverse.headOption.getOrElse(0.0)
    // Look across cycles from different segments
    val compilerII = scala.math.max(interval, segmentedInterval)
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

  @stateful def findAccumCycles(schedule: Seq[Sym[_]], verbose: Boolean = false): ScopeAccumInfo = {
    val scope = schedule.toSet
    val cycles = mutable.HashMap[Sym[_],mutable.Set[AccumTriple]]()
    def addCycle(sym: Sym[_], triple: AccumTriple): Unit = {
      val set = cycles.getOrElseAdd(sym, () => mutable.HashSet.empty)
      set += triple
    }

    val readers = scope.collect{
      case reader @ Reader(mem,_,_)     => AccessPair(mem, reader)
      case reader @ StatusReader(mem,_) => AccessPair(mem, reader)
      case reader @ BankedReader(mem,_,_,_) => AccessPair(mem, reader)
      case reader @ VectorReader(mem,_,_) => AccessPair(mem, reader)
    }
    val writers = scope.collect{
      case writer @ Writer(mem,_,_,_)     => AccessPair(mem, writer)
      case writer @ DequeuerLike(mem,_,_) => AccessPair(mem, writer)
      case writer @ BankedWriter(mem,_,_,_,_)     => AccessPair(mem, writer)
      case writer @ VectorWriter(mem,_,_,_) => AccessPair(mem, writer)
    }

    val readersByMem = readers.groupBy(_.mem).filter{x => !x._1.isArgIn && (x._2.size > 1 | writers.map(_.mem).contains(x._1))}.mapValues(_.map(_.access))
    val writersByMem = writers.groupBy(_.mem).filter{x => !x._1.isArgIn && (x._2.size > 1 | readers.map(_.mem).contains(x._1))}.mapValues(_.map(_.access))
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

    val scope = schedule.toSet

    val paths  = mutable.HashMap[Sym[_],Double]() ++ oos
    val cycles = mutable.HashMap[Sym[_],Set[Sym[_]]]()

    val accumInfo = findAccumCycles(schedule,verbose)
    val accums      = accumInfo.accums
    val accumReads  = accums.map(_.read)
    val accumWrites = accums.map(_.write)
    val knownCycles = accumInfo.cycles

    def debugs(x: => Any): Unit = if (verbose) dbgs(x)

    def findPseudoWARCycles(schedule: Seq[Sym[_]], verbose: Boolean = false): Set[WARCycle] = {
      val scope = schedule.toSet

      val readers = scope.collect{
        case reader @ Reader(mem,_,_)     => AccessPair(mem, reader)
        case reader @ StatusReader(mem,_) => AccessPair(mem, reader)
        case reader @ BankedReader(mem,_,_,_) => AccessPair(mem, reader)
        case reader @ VectorReader(mem,_,_) => AccessPair(mem, reader)
      }
      val writers = scope.collect{
        case writer @ Writer(mem,_,_,_)     => AccessPair(mem, writer)
        case writer @ DequeuerLike(mem,_,_) => AccessPair(mem, writer)
        case writer @ BankedWriter(mem,_,_,_,_)     => AccessPair(mem, writer)
        case writer @ VectorWriter(mem,_,_,_) => AccessPair(mem, writer)
      }

      val readersByMem = readers.groupBy(_.mem).filter{x => !x._1.isArgIn && (x._2.size > 1 | writers.map(_.mem).contains(x._1))}.mapValues(_.map(_.access))
      val writersByMem = writers.groupBy(_.mem).filter{x => !x._1.isArgIn && (x._2.size > 1 | readers.map(_.mem).contains(x._1))}.mapValues(_.map(_.access))
      val memories = readersByMem.keySet intersect writersByMem.keySet
      memories.flatMap{mem =>
        dbgs(s"pseudo cycles for $mem:")
        val rds = readersByMem(mem)
        val wrs = writersByMem(mem)
        rds.cross(wrs).collect{case (rd, wr) if (paths(rd) < paths(wr) && !accums.contains(AccumTriple(mem, rd, wr))) =>
          val cycleLengthExact = paths(wr).toInt - paths(rd).toInt + latencyOf(rd, true)
          dbgs(s" - $rd $wr cycle = $cycleLengthExact")

          // TODO[2]: FIFO/Stack operations need extra cycle for status update?
          val cycleLength = if (rd.isStatusReader) cycleLengthExact + 1.0 else cycleLengthExact
          WARCycle(rd, wr, mem, Set(rd,wr,mem), cycleLength)
        }
      }

    }

    def fullDFS(cur: Sym[_]): Double = {
      def precedingWrites: Set[Sym[_]] = {
        cur.readMem.map{mem => 
          val parentScope = cur.parent.innerBlocks.flatMap(_._2.stms)
          val writers = parentScope.filter(_.writtenMem == Some(mem)).toSet
          parentScope.zipWithIndex.collect{case (x,i) if i < parentScope.indexOf(cur) => x}.toSet intersect writers
        }.getOrElse(Set.empty)
      }
      cur match {
        case Op(d) if scope.contains(cur) =>
          // Handles effect scheduling, even though there's no data to pass
          val deps = scope intersect (cur.allDeps.toSet ++ precedingWrites)

          if (deps.nonEmpty) {
            val dlys = deps.map{e => paths.getOrElseAdd(e, () => fullDFS(e)) }

            // Primitives are not allowed to be loops, so the latency of nested symbols
            // must be some function of its blocks, e.g. the max of all or the sum of all
            // (For now, all cases are just the max of all inputs)
            val critical = d match {case _ => dlys.max }

            val cycleSyms = deps intersect cycles.keySet
            if (cycleSyms.nonEmpty) {
              cycles(cur) = cycleSyms.flatMap(cycles) + cur
              debugs(s"cycle deps of $cur: ${cycles(cur)}")
            }

            val inReduce = knownCycles.contains(cur)

            // TODO[3]: + inputDelayOf(cur) -- factor in delays which are external to reduction cycles
            val delay = critical + latencyOf(cur, inReduce)

            debugs(s"[$delay = max(" + dlys.mkString(", ") + s") + ${latencyOf(cur, inReduce)}] ${stm(cur)}" + (if (inReduce) "[cycle]" else ""))
            delay
          }
          else {
            val inReduce = knownCycles.contains(cur)
            val delay = latencyOf(cur, inReduce)
            debugs(s"[$delay = max(0) + ${latencyOf(cur, inReduce)}] ${stm(cur)}" + (if (inReduce) "[cycle]" else ""))
            delay
          }

        case s => paths.getOrElse(s, 0) // Get preset out of scope delay, or assume 0 offset
      }
    }

    // Perform backwards pass to push unnecessary delays out of reduction cycles
    // This can create extra registers, but decreases the initiation interval of the cycle
    def reverseDFS(cur: Sym[_], cycle: Set[Sym[_]]): Unit = cur match {
      case s: Sym[_] if cycle contains cur =>
        val forward = s.consumers intersect scope
        if (forward.nonEmpty) {
          debugs(s"${stm(s)} [${paths.getOrElse(s,0L)}]")

          val earliestConsumer = forward.map{e =>
            val in = paths.getOrElse(e, 0.0) - latencyOf(e, inReduce=cycle.contains(e))
            debugs(s"  [$in = ${paths.getOrElse(e, 0L)} - ${latencyOf(e,inReduce = cycle.contains(e))}] ${stm(e)}")
            in
          }.min

          val push = Math.max(earliestConsumer, paths.getOrElse(cur, 0.0))

          debugs(s"  [$push]")

          paths(cur) = push
        }
        s.allDeps.foreach{in => reverseDFS(in, cycle) }

      case _ => // Do nothing
    }


    def pushMultiplexedAccesses(accessors: Map[Sym[_],Set[Sym[_]]]) = accessors.flatMap{case (mem,accesses) =>
      if (accesses.nonEmpty && verbose){
        dbgs(s"Multiplexed accesses for memory $mem: ")
        accesses.foreach{access => dbgs(s"  ${stm(access)}") }
      }

      // NOTE: After unrolling there should be only one mux index per access
      // unless the common parent is a Switch
      val instances = if (mem.getDuplicates.isDefined) mem.duplicates.length else 0
      (0 to instances-1).map{id =>
        val accs = accesses.filter(_.dispatches.values.exists(_.contains(id))).filter(!_.op.get.isInstanceOf[FIFOPeek[_]])

        val muxPairs = accs.map{access =>
          val muxes = access.ports(0).values.map(_.muxPort)
          (access, paths.getOrElse(access,0.0), muxes.maxOrElse(0))
        }.toSeq

        val length = muxPairs.map(_._3).maxOrElse(0) - muxPairs.map(_._3).minOrElse(0) + 1

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
            dbgs(s"Pushing ${stm(access)} by ${writeDelay-oldPath} to $writeDelay due to muxing.")
            if (writeDelay-oldPath > 0) {
              paths(access) = writeDelay
              dbgs(s"  Also pushing these by ${writeDelay-oldPath}:")
              // Attempted fix for issue #54. Not sure how this interacts with cycles
              val affectedNodes = consumersDfs(access.consumers, Set(), scope) intersect scope
              affectedNodes.foreach{case x if (paths.contains(x)) => 
                  dbgs(s"  $x")
                  paths(x) = paths(x) + (writeDelay-oldPath)
                case _ =>
              }
            }
          }
        }

        AAACycle(accesses, mem, length)
      }
    }

    def pushSegmentationAccesses(): Unit = {
      accums.foreach{case AccumTriple(mem, reader, writer) => 
        if (reader.segmentMapping.nonEmpty && reader.segmentMapping.values.head > 0 && paths.contains(reader)) {
          dbgs(s"pushing segmentation access for $mem, $reader, $writer.. metadata ${reader.segmentMapping}")
          // Find any writer of previous segment
          val prevWriter = accums.collectFirst{case AccumTriple(m,_,w) if (scope.contains(w) && (m == mem) && (w.segmentMapping.values.head == reader.segmentMapping.values.head-1)) => w}
          dbgs(s"Found writer $prevWriter in segment ${reader.segmentMapping.values.head-1}")
          // Find latency where this previous writer occurs
          val baseLatency = if (paths.contains(prevWriter.get)) paths(prevWriter.get) else 0
          dbgs(s"reader $reader of $mem must begin after $baseLatency because it is part of segment ${reader.segmentMapping.values.head}")
          // Place reader at this latency
          val originalReadLatency = paths(reader)
          paths(reader) = baseLatency + 2 /*sram load latency*/
          val affectedNodes = (consumersDfs(reader.consumers, Set(), scope) intersect scope) diff Set(reader)
          dbgs(s"consumers of $reader are ${reader.consumers}, all affected are $affectedNodes")
          // Push everyone who depends on this reader by baseLatency + its original relative latency to the read
          affectedNodes.foreach{case x if (paths.contains(x)) => 
            val relativeLatency = paths(x) - originalReadLatency
            dbgs(s"  $x - Originally at ${paths(x)}, relative latency from read of $relativeLatency")
            paths(x) = baseLatency + relativeLatency + 2
          }
        }
      }
    }

    def pushBreakNodes(regWrite: Sym[_]): Unit = {
      val reg = regWrite match {case Op(_@RegWrite(x,_,_)) => x; case _ => throw new Exception(s"Cannot break loop with non-reg ($regWrite)")}
      if (regWrite.ancestors.exists(_.stopWhen == Some(reg))) {
        val parentScope = regWrite.parent.innerBlocks.flatMap(_._2.stms)
        val toPush = parentScope.zipWithIndex.collect{case (x,i) if i > parentScope.indexOf(regWrite) => x}.toSet
        toPush.foreach{
          case x if (paths.contains(x)) => 
            dbgs(s"  $x - Originally at ${paths(x)}, but must push by ${paths(regWrite)}")
            paths(x) = if (paths(x) < paths(regWrite)) paths(regWrite) + 1 else paths(x)
          case _ => 

        }
      } else dbgs(s"  $regWrite is not modifying its Reg inside the loop it breaks")

    }

    def protectRAWCycle(regWrite: Sym[_]): Unit = {
      val reg = regWrite.writtenMem.get
      val parentScope = regWrite.parent.innerBlocks.flatMap(_._2.stms)
      val writePosition = parentScope.indexOf(regWrite)
      val readsAfter = parentScope.drop(writePosition).collect{case x if (x.isReader && paths.contains(x) && paths.contains(regWrite) && paths(x).toInt <= paths(regWrite).toInt && x.readMem.isDefined && x.readMem.get == reg && !reg.hotSwapPairings.getOrElse(x,Set()).contains(regWrite)) => x}
      readsAfter.foreach{r => 
        val dist = paths(regWrite).toInt - paths(r).toInt
        warn(s"Avoid reading register (${reg.name.getOrElse("??")}) after writing to it in the same inner loop, if this is not an accumulation (write: ${regWrite.ctx}, read: ${r.ctx})")
        val affectedNodes = (consumersDfs(r.consumers, Set(), scope) intersect scope) ++ Set(r)
        affectedNodes.foreach{
          case x if (paths.contains(x)) => 
            dbgs(s"  $x - Originally at ${paths(x)}, but must push by $dist due to RAW cycle ${paths(regWrite)} - ${paths(r)}")
            paths(x) = paths(x) + dist
          case _ => 
        }
      }
    }

    debugs(s"----------------------------------")
    debugs(s"Computing pipeLatencies for scope:")
    schedule.foreach{ e => debugs(s"  ${stm(e)}") }

    accumReads.foreach{reader => cycles(reader) = Set(reader) }

    if (scope.nonEmpty) {
      // Perform forwards pass for normal data dependencies
      result.foreach{e => paths.getOrElseAdd(e, () => fullDFS(e)) }

      // TODO[4]: What to do in case where a node is contained in multiple cycles?
      accumWrites.toList.zipWithIndex.foreach{case (writer,i) =>
        val cycle = cycles.getOrElse(writer, Set.empty)
        dbgs(s"Cycle #$i: write: $writer, cycle: ${cycle.mkString(", ")}")
        reverseDFS(writer, cycle)
      }
    }

    val trueWarCycles = accums.collect{case AccumTriple(mem,reader,writer) => 
      val symbols = cycles(writer)
      val cycleLengthExact = paths(writer).toInt - paths(reader).toInt + latencyOf(reader, true)

      // TODO[2]: FIFO/Stack operations need extra cycle for status update?
      val cycleLength = if (reader.isStatusReader) cycleLengthExact + 1.0 else cycleLengthExact
      WARCycle(reader, writer, mem, symbols, cycleLength)
    }
    val pseudoWarCycles = findPseudoWARCycles(schedule)
    val warCycles = trueWarCycles ++ pseudoWarCycles

    val wawCycles = pushMultiplexedAccesses(accumInfo.writers)
    val rarCycles = pushMultiplexedAccesses(accumInfo.readers)
    val allCycles: Set[Cycle] = (wawCycles ++ rarCycles ++ warCycles).toSet      

    if (verbose) {
      if (allCycles.nonEmpty) {
        debugs(s"Found cycles: ")
        allCycles.foreach{x => debugs(s"$x")}
      }

      def dly(x: Sym[_]) = paths.getOrElse(x, 0.0)
      debugs(s"  Schedule after pipeLatencies calculation:")
      schedule.sortWith{(a,b) => dly(a) < dly(b)}.foreach{node =>
        debugs(s"  [${dly(node)}] ${stm(node)}")
      }
    }

    schedule.foreach{
      case x if x.isWriter && x.writtenMem.isDefined => 
        if (x.writtenMem.get.isBreaker) pushBreakNodes(x)
        if (x.writtenMem.get.isReg) protectRAWCycle(x)
      case _ => 
    }

    pushSegmentationAccesses()

    (paths.toMap, allCycles)
  }

  // Round out to nearest 1/1000 because numbers like 1.1999999997 - 0.2 < 1.0 and screws things up
  def scrubNoise(x: Double): Double = {
    if ( (x*1000) % 1 == 0) x
    else if ( (x*1000) % 1 < 0.5) (x*1000).toInt.toDouble/1000.0
    else ((x*1000).toInt + 1).toDouble/1000.0
  }


  @stateful def computeDelayLines(
    scope:      Seq[Sym[_]],
    latencies:  Map[Sym[_], Double],
    hierarchy:  Int,
    delayLines: Map[Sym[_], SortedSet[ValueDelay]],
    cycles:     Set[Sym[_]],
    createLine: Option[(Int, Sym[_], SrcCtx) => Sym[_]]
  ): Seq[(Sym[_], ValueDelay)] = {
    dbgs(s"computing delay lines for $scope $latencies $delayLines $cycles")
    val innerScope = scope.flatMap(_.blocks.flatMap(_.stms)).toSet

    def delayOf(x: Sym[_]): Double = latencies.getOrElse(x, 0.0)
    def requiresRegisters(x: Sym[_]): Boolean = latencyModel.requiresRegisters(x, cycles.contains(x))
    def retimingDelay(x: Sym[_]): Double = if (requiresRegisters(x)) latencyOf(x, cycles.contains(x)) else 0.0

    def delayLine(size: Int, in: Sym[_], ctx: SrcCtx): Option[() => Sym[_]] = createLine.map{func =>
      () => func(size, in, ctx)
    }

    def createValueDelay(input: Sym[_], reader: Sym[_], delay: Int): ValueDelay = {
      if (delay < 0) {
        bug("Compiler bug? Attempting to create a negative delay between input: ")
        bug(s"  ${stm(input)}")
        bug("and consumer: ")
        bug(s"  ${stm(reader)}")
        state.logBug()
      }
      // Retime inner block results as if we were already in the inner hierarchy
      val h = if (innerScope.contains(input)) hierarchy + 1 else hierarchy
      val existing = delayLines.getOrElse(input, SortedSet[ValueDelay]())
      existing.find{_.delay <= delay} match {
        case Some(prev) =>
          val size = delay - prev.delay
          if (size > 0) {
            logs(s"    Extending existing line of ${prev.delay}")
            ValueDelay(input, delay, size, h, Some(prev), delayLine(size, prev.value(), input.ctx))
          }
          else {
            logs(s"    Using existing line of ${prev.delay}")
            prev
          }

        case None =>
          logs(s"    Created new delay line of $delay")
          ValueDelay(input, delay, delay, h, None, delayLine(delay, input, input.ctx))
      }
    }

    val consumerDelays = scope.flatMap{case Stm(reader, d) =>
      val inReduce = cycles.contains(reader)
      val criticalPath = scrubNoise(delayOf(reader) - latencyOf(reader, inReduce))  // All inputs should arrive at this offset

      // Ignore non-bit based values
      val inputs = d.bitInputs //diff d.blocks.flatMap(blk => exps(blk))

      dbgs(s"[Arrive = Dly - Lat: $criticalPath = ${delayOf(reader)} - ${latencyOf(reader,inReduce)}] ${stm(reader)}")
      //logs(c"  " + inputs.map{in => c"in: ${delayOf(in)}"}.mkString(", ") + "[max: " + criticalPath + "]")
      inputs.flatMap{in =>
        val latency_required = scrubNoise(criticalPath)    // Target latency required upon reaching this reader
        val latency_achieved = scrubNoise(delayOf(in))                       // Latency already achieved at the output of this in (assuming latency_missing is already injected)
        val latency_missing  = scrubNoise(retimingDelay(in) - builtInLatencyOf(in)) // Latency of this input that still requires manual register injection
        val latency_actual   = scrubNoise(latency_achieved - latency_missing)
        val delay = latency_required.toInt - latency_actual.toInt
        dbgs(s"..[${latency_required - latency_actual} (-> $delay) = $latency_required - ($latency_achieved - $latency_missing) (-> ${latency_required.toInt} - ${latency_actual.toInt})] ${stm(in)}")
        if (delay.toInt != 0) Some(in -> (reader, delay.toInt)) else None
      }
    }
    val inputDelays = consumerDelays.groupBy(_._1).mapValues(_.map(_._2)).toSeq
    inputDelays.flatMap{case (input, consumers) =>
      val consumerGroups = consumers.groupBy(_._2).mapValues(_.map(_._1))
      val delays = consumerGroups.keySet.toList.sorted  // Presort to maximize coalescing
      delays.flatMap{delay =>
        val readers = consumerGroups(delay)
        readers.map{reader =>
          dbgs(s"  Creating value delay on $input for reader $reader with delay $delay: ")
          reader -> createValueDelay(input, reader, delay)
        }
      }
    }
  }


  /**
    * Calculate delay line costs:
    * a. Determine time (in cycles) any given input or internal signal needs to be delayed
    * b. Distinguish each delay line as a separate entity
    *
    * Is there a concise equation that can capture this? Haven't been able to come up with one.
    * E.g.
    *   8 inputs => perfectly balanced binary tree, no delay paths
    *   9 inputs => 1 path of length 3
    *   85 inputs => 3 paths with lengths 2, 1, and 1
    **/
  def reductionTreeDelays(nLeaves: Int): List[Long] = {
    if ( (nLeaves & (nLeaves - 1)) == 0) Nil // Specialize for powers of 2
    // Could also have 2^k + 1 case (delay = 1 path of length k)
    else {
      def reduceLevel(nNodes: Int, completePaths: List[Long], currentPath: Long): List[Long] = {
        if (nNodes <= 1) completePaths  // Stop when 1 node is remaining
        else if (nNodes % 2 == 0) {
          // For an even number of nodes, we don't need any delays - all current delay paths end
          val allPaths = completePaths ++ (if (currentPath > 0) List(currentPath) else Nil)
          reduceLevel(nNodes/2, allPaths, 0L)
        }
        // For odd number of nodes, always delay exactly one signal, and keep delaying that signal until it can be used
        else reduceLevel((nNodes-1)/2 + 1, completePaths, currentPath+1)
      }

      reduceLevel(nLeaves, Nil, 0L)
    }
  }

  def reductionTreeHeight(nLeaves: Int): Int = {
    def treeLevel(nNodes: Int, curHeight: Int): Int = {
      if (nNodes <= 1) curHeight
      else if (nNodes % 2 == 0) treeLevel(nNodes/2, curHeight + 1)
      else treeLevel((nNodes - 1)/2 + 1, curHeight + 1)
    }
    treeLevel(nLeaves, 0)
  }

}
