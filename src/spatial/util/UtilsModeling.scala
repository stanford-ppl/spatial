package spatial.util

import core._
import forge.tags.stateful
import utils.implicits.collections._
import spatial.data._
import spatial.node._
import spatial.internal.spatialConfig
import spatial.targets.LatencyModel

import scala.collection.mutable

abstract class Cycle {
  def length: Double
  def symbols: Set[Sym[_]]
}

/** Write-after-read (WAR) cycle: Standard read-accumulate loop. **/
case class WARCycle(reader: Sym[_], writer: Sym[_], memory: Sym[_], symbols: Set[Sym[_]], length: Double) extends Cycle

/** Access-after-access (AAA) cycle: Time-multiplexed writes. **/
case class AAACycle(accesses: Set[Sym[_]], memory: Sym[_], length: Double) extends Cycle {
  def symbols: Set[Sym[_]] = accesses
}


trait UtilsModeling {

  def blockNestedScheduleAndResult(block: Block[_]): (Seq[Sym[_]], Seq[Sym[_]]) = {
    val schedule = block.nestedStms
      .filter{e => e.isBits }

    val result = (block +: schedule.flatMap{_.blocks}).flatMap{b => exps(b) }

    (schedule, result)
  }

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
    verbose:  Boolean = true
  ): (Double, Double) = {
    val (latencies, cycles) = latenciesAndCycles(block, verbose = verbose)
    val scope = latencies.keySet

    val latency = latencies.values.fold(0.0){(a,b) => Math.max(a,b) }
    // TODO: Safer way of determining if THIS cycle is the reduceType
    val interval = (cycles.map{c =>
      val scopeContainsSpecial = scope.exists(x => reduceType(x).contains(FixPtSum) )
      val cycleContainsAdd = c.symbols.exists{case Op(FixAdd(_,_)) => true; case _ => false}
      val length = if (cycleContainsAdd && scopeContainsSpecial) 1 else c.length
      length
    } + 0).max
    // HACK: Set initiation interval to 1 if it contains a specialized reduction
    // This is a workaround for chisel codegen currently specializing and optimizing certain reduction types
    val compilerII = interval
    (latency, compilerII)
  }


  @stateful def latenciesAndCycles(block: Block[_], verbose: Boolean = true): (Map[Sym[_],Double], Set[Cycle]) = {
    val (schedule, result) = blockNestedScheduleAndResult(block)
    pipeLatencies(result, schedule, verbose = verbose)
  }

  // TODO: This is an enormous method - can we break this up?
  @stateful def pipeLatencies(
    result:   Seq[Sym[_]],
    schedule: Seq[Sym[_]],
    oos:      Map[Sym[_],Double] = Map.empty,
    verbose:  Boolean = true
  ): (Map[Sym[_],Double], Set[Cycle]) = {

    dbgs(s"----------------------------------")
    dbgs(s"Computing pipeLatencies for scope:")
    schedule.foreach{e => dbgs(s"  ${stm(e)}")}

    val scope = schedule.toSet
    val knownCycles = mutable.HashMap[Sym[_],Set[(Sym[_],Sym[_])]]()

    // TODO: FifoDeq appears as Reader and DequeueLike.  Adds "fake" cycles but shouldn't impact final answer since we take max
    val localReads  = scope.collect{case reader @ Reader(mem,_,_) => reader -> mem }
    val localWrites = scope.collect{case writer @ Writer(mem,_,_,_) => writer -> mem
                                    case reader @ DequeuerLike(mem,_,_) => reader -> mem
                                   }
    val localStatuses = scope.collect{case reader @ StatusReader(mem,_) => reader -> mem}

    val localAccums = localWrites.flatMap{case (writer,writtenMem) =>
      (localReads ++ localStatuses).flatMap{case (reader,readMem) =>
        if (readMem == writtenMem) {
          val path = writer.getNodesBetween(reader, scope)

          path.foreach{sym =>
            knownCycles += sym -> (knownCycles.getOrElse(sym, Set.empty[(Sym[_],Sym[_])]) + ((reader, writer)) )
          }

          if (verbose && path.nonEmpty) {
            dbgs("Found cycle between: ")
            dbgs(s"  ${stm(writer)}")
            dbgs(s"  ${stm(reader)}")
            path.foreach{node =>
              dbgs(s"    ${stm(node)}")
            }
          }
          else {
            dbgs(s"No cycle between: ")
            dbgs(s"  ${stm(writer)}")
            dbgs(s"  ${stm(reader)}")
          }

          if (path.nonEmpty) {
            Some((reader,writer,writtenMem))
          }
          else None
        }
        else None
        //readMem == writtenMem && writer.dependsOn(reader)
      }
    }
    val accumReads = localAccums.map(_._1)
    val accumWrites = localAccums.map(_._2)

    val paths  = mutable.HashMap[Sym[_],Double]() ++ oos
    val cycles = mutable.HashMap[Sym[_],Set[Sym[_]]]()

    accumReads.foreach{reader => cycles(reader) = Set(reader) }

    def fullDFS(cur: Sym[_]): Double = cur match {
      case Op(d) if scope.contains(cur) =>
        val deps = scope intersect cur.allDeps.toSet // Handles effect scheduling, even though there's no data to pass

        if (deps.nonEmpty) {
          val dlys = deps.map{e => paths.getOrElseAdd(e, () => fullDFS(e)) }

          // Primitives are not allowed to be loops, so the latency of nested symbols must be some function of its blocks
          // e.g. the max of all or the sum of all
          // (For now, all cases are just the max of all inputs)
          val critical = d match {
            case _ => dlys.max
          }

          val cycleSyms = deps intersect cycles.keySet
          if (cycleSyms.nonEmpty) {
            cycles(cur) = cycleSyms.flatMap(cycles) + cur
            dbgs(s"cycle deps of $cur: ${cycles(cur)}")
          }

          val inReduce = knownCycles.contains(cur)

          val delay = critical + latencyOf(cur, inReduce) // TODO + inputDelayOf(cur) -- factor in delays which are external to reduction cycles

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

      // TODO: What to do in case where a node is contained in multiple cycles?
      accumWrites.toList.zipWithIndex.foreach{case (writer,i) =>
        val cycle = cycles.getOrElse(writer, Set.empty)
        if (verbose) dbgs(s"Cycle #$i: ")
        reverseDFS(writer, cycle)
      }
    }

    //val cycleSyms = accumWrites.flatMap{writer => cycles(writer) }

    val warCycles = localAccums.map{case (reader,writer,mem) =>
      val symbols = cycles(writer)
      val cycleLengthExact = paths(writer) - paths(reader)
      val cycleLength = if (localStatuses.toList.map(_._1).contains(reader)) cycleLengthExact + 1.0 else cycleLengthExact // FIFO/Stack operations need extra cycle for status update (?)
      WARCycle(reader, writer, mem, symbols, cycleLength)
    }

    def pushMultiplexedAccesses(accessors: Set[(Sym[_],Sym[_])]) = accessors.groupBy{_._2}.map{case (mem,accesses) =>
      dbgs(s"Multiplexed accesses for memory $mem: ")
      accesses.foreach{access => dbgs(s"  ${stm(access._1)}") }

      val muxPairs = accesses.map{x =>
        // NOTE: After unrolling there should be only one mux index per access
        // unless the common parent is a Switch
        val muxes = portsOf(x._1).values.map(_.muxPort) //  muxIndexOf.getMem(x._1,x._2)
        (x, paths.getOrElse(x._1,0.0), muxes.fold(0){Math.max})
      }.toSeq

      val length = muxPairs.map(_._3).fold(0){Math.max} + 1

      // Keep accesses with the same mux index together, even if they have different delays
      // TODO[1]: This isn't quite right - should order by common parent instead?
      val groupedMuxPairs = muxPairs.groupBy(_._3)
      val orderedMuxPairs = groupedMuxPairs.values.toList.sortBy{pairs => pairs.map(_._2).max }
      var writeStage = 0.0
      orderedMuxPairs.foreach{pairs =>
        val dlys = pairs.map(_._2) :+ writeStage
        val writeDelay = dlys.max
        writeStage = writeDelay + 1
        pairs.foreach{case (x, dly, _) =>
          dbgs(s"Pushing ${stm(x._1)} to $writeDelay due to muxing")
          paths(x._1) = writeDelay
        }
      }

      AAACycle(accesses.map(_._1), mem, length)
    }

    val wawCycles = pushMultiplexedAccesses(localWrites)
    val rarCycles = pushMultiplexedAccesses(localReads)
    val allCycles: Set[Cycle] = (wawCycles ++ rarCycles ++ warCycles).toSet

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
