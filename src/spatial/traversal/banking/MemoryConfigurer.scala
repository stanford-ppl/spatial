package spatial.traversal
package banking

import argon._
import poly.ISL
import utils.implicits.collections._
import utils.math._
import utils.tags.instrument

import utils.math.isPow2
import spatial.issues.UnbankableGroup
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.retiming._
import spatial.metadata.memory._
import spatial.util.spatialConfig
import poly.{SparseMatrix, SparseVector}

import scala.collection.mutable.ArrayBuffer

import spatial.metadata.types._

class MemoryConfigurer[+C[_]](mem: Mem[_,C], strategy: BankingStrategy)(implicit state: State, isl: ISL) {
  protected lazy val rank: Int = mem.sparseRank.length
  protected lazy val isGlobal: Boolean = mem.isArgIn || mem.isArgOut

  // TODO: This may need to be tweaked based on the fix for issue #23
  final lazy val FLAT_BANKS = Seq(List.tabulate(rank){i => i})
  final lazy val NEST_BANKS = List.tabulate(rank){i => Seq(i)}
  lazy val bankViews: Seq[BankingView] = if (mem.isLineBuffer) Seq(Hierarchical(rank, Some(List(rank-1))))
                                         else if (rank > 1 && !mem.isNoHierarchicalBank && !mem.isNoFlatBank && !mem.isNoBank) Seq(Flat(rank), Hierarchical(rank)) 
                                         else if (mem.isNoHierarchicalBank) Seq(Flat(rank)) 
                                         else if (mem.isNoFlatBank) Seq(Hierarchical(rank)) 
                                         else if (mem.isNoBank) Seq() 
                                         else Seq(Flat(rank))

  lazy val nStricts: Seq[NStrictness] = Seq(NPowersOf2, NBestGuess, NRelaxed)
  lazy val aStricts: Seq[AlphaStrictness] = Seq(AlphaPowersOf2, AlphaBestGuess, AlphaRelaxed)
  lazy val dimensionDuplication: Seq[RegroupDims] = if (mem.isDuplicatable & !mem.isNoDuplicate & !spatialConfig.enablePIR) RegroupHelper.regroupAny(rank) else RegroupHelper.regroupNone


  def configure(): Unit = {
    dbg(s"---------------------------------------------------------------------")
    dbg(s"INFERRING...")
    dbg(s"Name: ${mem.fullname}")
    dbg(s"Type: ${mem.tp}")
    dbg(s"Src:  ${mem.ctx}")
    dbg(s"Src:  ${mem.ctx.content.getOrElse("<???>")}")
    dbg(s"Symbol:     ${stm(mem)}")
    dbgs(s"Effort:    ${mem.bankingEffort}")
    dbgs(s"BankingViews:   ${bankViews}")
    dbgs(s"NStrictness:   ${nStricts}")
    dbgs(s"AlphaStrictness:   ${aStricts}")
    dbgs(s"DimensionDuplication: ${dimensionDuplication}")
    dbg(s"---------------------------------------------------------------------")
    val readers = mem.readers
    val writers = mem.writers
    resetData(readers, writers)

    val readMatrices = readers.flatMap{rd => rd.affineMatrices }
    val writeMatrices = writers.flatMap{wr => wr.affineMatrices}

    val instances = bank(readMatrices, writeMatrices)

    summarize(instances)
    finalize(instances)
  }

  protected def resetData(readers: Set[Sym[_]], writers: Set[Sym[_]]): Unit = {
    (readers.iterator ++ writers.iterator).foreach{a =>
      metadata.clear[Dispatch](a)
      metadata.clear[Ports](a)
    }
    metadata.clear[Duplicates](mem)
  }

  protected def summarize(instances: Seq[Instance]): Unit = {
    dbg(s"---------------------------------------------------------------------")
    dbg(s"SUMMARY: ")
    dbg(s"Name: ${mem.fullname}")
    dbg(s"Type: ${mem.tp}")
    dbg(s"Src:  ${mem.ctx}")
    dbg(s"Src:  ${mem.ctx.content.getOrElse("<???>")}")
    dbg(s"Symbol:     ${stm(mem)}")
    dbg(s"---------------------------------------------------------------------")
    dbg(s"Instances: ${instances.length}")
    instances.zipWithIndex.foreach{case (inst,i) =>
      dbg(s"Instance #$i")
      dbgss("  ", inst)
      dbg("\n\n")
    }
    dbg(s"---------------------------------------------------------------------")
    dbg("\n\n\n")
  }

  /** Complete memory analysis by adding banking and buffering metadata to the memory and
    * all associated accesses.
    */
  protected def finalize(instances: Seq[Instance]): Unit = {
    val duplicates = instances.map{_.toMemory}
    mem.duplicates = duplicates

    instances.zipWithIndex.foreach{case (inst, dispatch) =>
      (inst.reads.iterator.flatten ++ inst.writes.iterator.flatten).foreach{a =>
        a.access.addPort(dispatch, a.unroll, inst.ports(a))
        a.access.addDispatch(a.unroll, dispatch)
        dbgs(s"  Added port ${inst.ports(a)} to ${a.short}")
        dbgs(s"  Added dispatch $dispatch to ${a.short}")
      }

      if (inst.writes.flatten.isEmpty && mem.name.isDefined && !mem.hasInitialValues) {
        inst.reads.iterator.flatten.foreach{read =>
          warn(read.access.ctx, s"Memory ${mem.name.get} appears to be read here before ever being written.")
          warn(read.access.ctx)
          warn(mem.ctx, s"For memory ${mem.name.get} originally defined here.", noWarning = true)
          warn(mem.ctx)
        }
      }
    }

    val used = instances.flatMap(_.accesses).toSet
    val unused = mem.accesses diff used
    unused.foreach{access =>
      if (mem.name.isDefined) {
        val msg = if (access.isReader) s"Read of memory ${mem.name.get} was unused. Read will be removed."
                  else s"Write to memory ${mem.name.get} is never used. Write will be removed."
        warn(access.ctx, msg)
        warn(access.ctx)
      }

      access.isUnusedAccess = true

      dbgs(s"  Unused access: ${stm(access)}")
    }

    if (spatialConfig.enablePIR) {
      instances.zipWithIndex.foreach { case (inst, dispatch) =>
        def checkAccess(groups:Set[Set[AccessMatrix]]) = {
          // Mapping of access matrix => group id
          val groupMap = groups.zipWithIndex.flatMap { case (grp, gid) => grp.map { mat => (mat, gid) } }.toMap
          groups.flatten.groupBy { _.access }.foreach { case (access, mats) =>
            val gids = mats.map { mat => groupMap(mat) }
            val castgroup = mats.map { mat => inst.ports(mat).castgroup}
            if (gids.size > 1 && castgroup.size > 1) {
              error(s"//TODO: Plasticine does not support unbanked unrolled access at the moment. ")
              error(s"mem=$mem (${mem.ctx} ${mem.name.getOrElse("")})")
              error(s"access=$access (${access.ctx})")
              error(s"AccessMatrix:")
              mats.foreach { mat => 
                error(s"$mat castgroup:${inst.ports(mat).castgroup}")
              }
              state.logError()
            }
          }
        }
        checkAccess(inst.reads)
        checkAccess(inst.writes)
      }
      // TODO: after lid is added. check access with same outer loop uid but different inner loop
      // uid touch the same instance
      //used.foreach { access =>
        //access.dispatches { case (uid, dispatches) =>
        //}
      //}
    }

  }

  /** True if a and b always occur at the exact same time, or if are interface arg reads.
    * This is trivially true if a and b are the same unrolled access.
    *
    * This method is used to enable broadcast reads.
    */
  def canBroadcast(a: AccessMatrix, b: AccessMatrix): Boolean = {
    // TODO[3]: What about accesses of the same form across different loops?
    // Should we rely on loop fusion for this? Are there cases where that wouldn't work?
    if (mem.isArgIn || a == b) return true // isGlobal isn't correct if we are using an ArgOut to mediate stream control across different unrolled bodies
    val isWrite = a.access.isWriter || b.access.isWriter
    if (isWrite || a.access != b.access || a.matrix != b.matrix) return false

    val iters = accessIterators(a.access, mem)
    // The index of iterators which will differ between a and b
    // If this list is empty, a and b are identical (so they are trivially lockstep)
    val differ = a.unroll.indices.filter{i => a.unroll(i) != b.unroll(i) }
    val itersDiffer = differ.map{i => iters(i) }
    if (itersDiffer.nonEmpty) {
      val outermost = itersDiffer.head
      if (outermost.isInnerControl) true  // Unrolling takes care of this broadcast within inner ctrl
      else {
        // Need more specialized logic for broadcasting across controllers
        spatialConfig.enableBroadcast && outermost.parent.s.get.isLockstepAcross(itersDiffer, Some(a.access), Some(outermost.parent))
      }
    }
    else true
  }

  /** Group accesses on this memory.
    *
    * For some access a to this memory and some existing group S:
    * let B = {all b in S | Parallel(a,b) }
    *   where Parallel(a,b) is true if a and b may occur simultaneously (parallel or pipeline parallel)
    *
    * Access a is grouped with S if:
    *   [Control] B is non-empty
    *   [Space]   for all b in B: a and b do not conflict (never overlap or can be broadcast)
    * If no such groups exist, a is placed in a new group S' = {a}
    */
  protected def groupAccesses(accesses: Set[AccessMatrix]): Set[Set[AccessMatrix]] = {
    val groups = ArrayBuffer[Set[AccessMatrix]]()
    val isWrite = accesses.exists(_.access.isWriter)
    val tp = if (isWrite) "Write" else "Read"

    dbgs(s"  Grouping ${accesses.size} ${tp}s: ")

    import scala.math.Ordering.Implicits._  // Seq ordering
    val sortedAccesses = accesses.toSeq.sortBy{x => (x.access.toString, x.unroll)}

    sortedAccesses.foreach{a =>
      dbg(s"    Access: ${a.short} [${a.parent}]")
      val grpId = {
        if (a.parent == Ctrl.Host) { if (groups.isEmpty) -1 else 0 }
        else groups.zipWithIndex.indexWhere{case (grp, i) =>
          // Filter for accesses that require concurrent port access AND either don't overlap or are identical.
          // Should drop in data broadcasting node in this case
          val samePort = grp.filter{b => requireConcurrentPortAccess(a, b) }
          // A conflict occurs if there are accesses on the same port with overlapping addresses
          // for which we can cannot create a broadcaster read
          // (either because they are not lockstep, not reads, or because broadcasting is disabled)
          val conflicts = samePort.filter{b => a.overlapsAddress(b) && !canBroadcast(a, b) && (a.segmentAssignments == b.segmentAssignments)}
          samePort.foreach{b => val conflictable = dephasingIters(a,b,mem); if (conflictable.nonEmpty) dbgs(s"      WARNING: Group contains iters ${conflictable.map(_._1)} that dephase due to non-lockstep controllers")}
          if (conflicts.nonEmpty) dbg(s"      Group #$i conflicts: <${conflicts.size} accesses>")
          else                    dbg(s"      Group #$i conflicts: <none>")
          if (config.enLog) conflicts.foreach{b => logs(s"        ${b.short} [${b.parent}]")  }

          if (samePort.nonEmpty)  dbg(s"      Group #$i same port: <${samePort.size} accesses>")
          else                    dbg(s"      Group #$i same port: <none> ")
          if (config.enLog) samePort.foreach{b => logs(s"        ${b.short} [${b.parent}]")}

          samePort.nonEmpty && conflicts.isEmpty
        }
      }
      if (grpId != -1) { groups(grpId) = groups(grpId) + a } else { groups += Set(a) }
    }

    if (config.enDbg) {
      if (groups.isEmpty) dbg(s"\n  <No $tp Groups>") else dbg(s"  ${groups.length} $tp Groups:")
      groups.zipWithIndex.foreach { case (grp, i) =>
        dbg(s"  Group #$i")
        grp.foreach{matrix => dbgss("    ", matrix) }
      }
    }
    groups.toSet
  }

  /** True if the memory is written in the given controller. */
  def isWrittenIn(ctrl: Ctrl): Boolean = {
    mem.writers.exists{write => write.ancestors.contains(ctrl) }
  }

  /** True if the lca of the two accesses is an outer controller and each of 
      the accesses have different unroll addresses for the iterators that 
      compose this lca
    */
  def willUnrollTogether(a: AccessMatrix, b: AccessMatrix): Boolean = {
    val lca = LCA(a.access, b.access)
    val lcaIters = lca.scope.iters
    val aIters = accessIterators(a.access, mem)
    val bIters = accessIterators(b.access, mem)
    val isPeek = a.access.isPeek || b.access.isPeek
    isPeek || lca.isInnerControl || lcaIters.forall{iter => if (aIters.contains(iter) && bIters.contains(iter)) {a.unroll(aIters.indexOf(iter)) == b.unroll(bIters.indexOf(iter))} else true}
  }

  /** True if accesses a and b may occur concurrently and to the same buffer port.
    * This is true when any of the following hold:
    *   0. a and b are two different unrolled parts of the same node
    *   1. a and b are in the same inner pipeline
    *   2. a and b are in the same fully unrolled inner sequential
    *   3. a and b are in a Parallel controller
    *   4. a and b are in a pipelined controller but NOT buffered w.r.t each other (no writes in LCA(a,b))
    *   5. TODO[2]: If a and b are in parallel stages in a controller's child dataflow graph
    */
  def requireConcurrentPortAccess(a: AccessMatrix, b: AccessMatrix): Boolean = {
    val lca = LCA(a.access, b.access)
    (a.access == b.access && a.unroll != b.unroll) ||
      lca.isInnerPipeLoop ||
      (lca.isInnerSeqControl && lca.isFullyUnrolledLoop) ||
      ((lca.isOuterPipeLoop || lca.isOuterStreamLoop) && !isWrittenIn(lca)) ||
      (a.access.delayDefined && b.access.delayDefined && a.access.parent == b.access.parent && a.access.fullDelay == b.access.fullDelay) ||
      lca.isParallel
  }


  protected def bank(readers: Set[AccessMatrix], writers: Set[AccessMatrix]): Seq[Instance] = {
    val rdGroups = groupAccesses(readers)
    val wrGroups = groupAccesses(writers)
    if      (readers.nonEmpty) mergeReadGroups(rdGroups, wrGroups)
    else if (writers.nonEmpty) mergeWriteGroups(wrGroups)
    else Seq(Instance.Unit(rank))
  }

  /** Returns an approximation of the cost for the given banking strategy. */
  def cost(banking: Seq[Banking], depth: Int, rdGroups: Set[Set[AccessMatrix]], wrGroups: Set[Set[AccessMatrix]]): Long = {
    // Set up penalty model TODO: Make it more accurate
    val mulCost = 6
    val divCost = 20
    val modCost = 20
    val muxCost = 6
    val volumePenalty = 1

    if (banking.nonEmpty) {
      // Get powerOf2 composition
      val padding          = mem.stagedDims.map(_.toInt).zip(banking.flatMap(_.Ps)).map{case(d,p) => (p - d%p) % p}
      val w                = mem.stagedDims.map(_.toInt).zip(padding).map{case (a:Int, b:Int) => a + b}
      val D                = w.size
      val numBanks         = banking.map    (_.nBanks).product
      val Ns_not_pow2      = banking.map    (_.nBanks).map{x => if (isPow2(x)) 0 else 1}.sum
      val alphas_not_pow2  = banking.flatMap(_.alphas).map{x => if (isPow2(x)) 0 else 1}.sum
      val Ps               = banking.flatMap(_.Ps)
      val Pss_not_pow2     = Ps.map{x => if (isPow2(x)) 0 else 1}.sum
      val dimMultipliers   = Seq.tabulate(D){t => (w.slice(t+1,D).zip(Ps.slice(t+1,D)).map{case (x,y) => math.ceil(x/y).toInt}.product)}
      val mults_not_pow2   = dimMultipliers.map{x => if (isPow2(x)) 0 else 1}.sum

      // Partition based on direct/xbar banking
      val (direct, xbar) = (rdGroups ++ wrGroups).flatten.partition(_.isDirectlyBanked(banking.map(_.nBanks), banking.map(_.stride), banking.flatMap(_.alphas)))

      // Compute penalty from offset calculation
      val ofsDivPenalty = (direct.size + xbar.size) * Pss_not_pow2 * divCost //spatialConfig.target.latencyModel.model("FixDiv")("b" -> 32))
      val ofsMulPenalty = (direct.size + xbar.size) * mults_not_pow2 * mulCost //spatialConfig.target.latencyModel.model("FixMul")("b" -> 32))

      // Compute penalty from bank calculation
      val bankMulPenalty = xbar.size * alphas_not_pow2 * mulCost //spatialConfig.target.latencyModel.model("FixMul")("b" -> 32))
      val bankModPenalty = xbar.size * Ns_not_pow2 * modCost //spatialConfig.target.latencyModel.model("FixMod")("b" -> 32))

      // Compute penalty from muxes for bank resolution
      val wmuxPenalty = if (!mem.isReg && !mem.isRegFile && !mem.isStreamOut && !mem.isStreamIn && xbar.filter(_.access.isWriter).size > 0) depth * muxCost * numBanks * numBanks * xbar.filter(_.access.isWriter).size else 0
      val rmuxPenalty = if (!mem.isReg && !mem.isRegFile && !mem.isStreamOut && !mem.isStreamIn && xbar.filter(_.access.isReader).size > 0) depth * muxCost * numBanks * numBanks * xbar.filter(_.access.isReader).size else 0

      // Compute penalty from volume
      val sizePenalty = depth * w.product * volumePenalty
      
      // dbgs(s"BANKING COST FOR $mem UNDER $banking:")
      // dbgs(s"  depth            = ${depth}")
      // dbgs(s"  volume           = ${w.product}")
      // dbgs(s"  numBanks         = ${numBanks}")
      // dbgs(s"    `- # not pow 2 = ${Ns_not_pow2}")
      // dbgs(s"  alphas           = ${banking.map(_.alphas)}")
      // dbgs(s"    `- # not pow 2 = ${alphas_not_pow2}")
      // dbgs(s"  Ps               = ${banking.map(_.Ps)}")
      // dbgs(s"    `- # not pow 2 = ${Pss_not_pow2}")
      // dbgs(s"  dim multipliers  = ${dimMultipliers}")
      // dbgs(s"    `- # not pow 2 = ${mults_not_pow2}")
      // dbgs(s"  Directly banked accesses: ${direct.map(_.access)}")
      // dbgs(s"  XBar banked accesses:     ${xbar.map(_.access)}")
      // dbgs(s"")    
      // dbgs(s"  ofsDivPenalty  = ${ofsDivPenalty}")
      // dbgs(s"  ofsMulPenalty  = ${ofsMulPenalty}")
      // dbgs(s"  bankMulPenalty  = ${bankMulPenalty}")
      // dbgs(s"  bankModPenalty  = ${bankModPenalty}")
      // dbgs(s"  wmuxPenalty  = ${wmuxPenalty}")
      // dbgs(s"  rmuxPenalty  = ${rmuxPenalty}")
      // dbgs(s"  sizePenalty  = ${sizePenalty}")
      // dbgs(s"")

      val totalCost = (ofsDivPenalty + ofsMulPenalty + bankMulPenalty + bankModPenalty + wmuxPenalty + rmuxPenalty + sizePenalty).toLong

      // dbgs(s"TOTAL COST: $totalCost")

      totalCost
    } else {
      val w                = mem.stagedDims.map(_.toInt)
      val sizePenalty    = depth * w.product * volumePenalty

      val numWriters = wrGroups.flatten.size

      // Assume direct banking for W, and crossbar for readers
      val rmuxPenalty = if (!mem.isReg && !mem.isRegFile && !mem.isStreamOut && !mem.isStreamIn) depth * muxCost * numWriters * numWriters else 0

      // dbg(s"BANKING COST FOR $mem UNDER DUPLICATION:")
      // dbg(s"  depth            = ${depth}")
      // dbg(s"  volume           = ${w.product}")
      // dbgs(s"  rmuxPenalty  = ${rmuxPenalty}")
      // dbgs(s"")

      val totalCost =  rdGroups.flatten.size * (sizePenalty + rmuxPenalty).toLong

      // dbgs(s"TOTAL COST: $sizePenalty")

      totalCost

    }
  }

  /** Computes the Ports for all accesses in all groups
    *
    *            |--------------|--------------|
    *            |   Buffer 0   |   Buffer 1   |
    *            |--------------|--------------|
    * bufferPort         0              1           The buffer port (None for access outside pipeline)
    *                 |x x x|        |x x x|
    *
    *                /       \      /       \
    *
    *              |x x x|x x|     |x x x|x x x|
    * muxPort         0    1          0     1       The ID for the given time multiplexed vector
    *
    *              |( ) O|O O|    |(   )|( ) O|
    * muxOfs        0   2 0 1        0    0  2      Start offset into the time multiplexed vector
    *
    *
    * broadcast    | A  B |
    *
    *
    */
  protected def computePorts(groups: Set[Set[AccessMatrix]], bufPorts: Map[Sym[_],Option[Int]]): Map[AccessMatrix,Port] = {
    var ports: Map[AccessMatrix,Port] = Map.empty

    val bufferPorts: Map[Option[Int],Set[Set[AccessMatrix]]] = groups.flatMap{grp =>
      grp.groupBy{m => bufPorts(m.access) }.toSeq // These should generally always be all the same?
    }.groupBy(_._1).mapValues(_.map(_._2))

    bufferPorts.foreach{case (bufferPort, grps) =>
      var muxPortMap: Map[AccessMatrix,Int] = Map.empty

      // Each group is banked together, so all accesses in a group must be connected to the same muxPort
      grps.zipWithIndex.foreach{case (grp,muxPort) =>
        grp.foreach{m => muxPortMap += m -> muxPort }
      }

      val muxPorts: Map[Int,Set[AccessMatrix]] = grps.flatten.groupBy{m => muxPortMap(m) }

      muxPorts.foreach{case (muxPort, matrices) =>
        var muxOfs: Int = 0

        var nGroups: Int = -1
        var broadcastGroups: Map[Int,Set[AccessMatrix]] = Map.empty
        def checkBroadcast(m1: AccessMatrix): (Int,Int,Int) = {
          val group = broadcastGroups.find{case (_,mats) =>
            mats.exists{m2 => canBroadcast(m1, m2) }
          }
          val (groupID, muxOffset) = group match {
            case Some((bID,mats)) =>
              broadcastGroups += bID -> (mats + m1)
              (bID, ports(mats.head).muxOfs)
            case None =>
              nGroups += 1
              broadcastGroups += nGroups -> Set(m1)
              (nGroups, muxOfs)
          }
          // Broadcast ID is the number of accesses in that group - 1
          (groupID, broadcastGroups(groupID).size - 1, muxOffset)
        }

        // Assign mux offsets per access
        matrices.groupBy(_.access).values.foreach{mats =>
          // And assign mux offsets in unroll order
          import scala.math.Ordering.Implicits._
          mats.toSeq.sortBy(_.unroll).foreach{m =>
            val (castgroup, broadcast, muxOffset) = checkBroadcast(m)

            val port = Port(
              bufferPort = bufferPort,
              muxPort    = muxPort,
              muxOfs     = muxOffset,
              castgroup  = Seq(castgroup),
              broadcast  = Seq(broadcast)
            )
            ports += m -> port
            if (broadcast == 0) muxOfs += 1
          }
        }
      }
    }

    ports
  }

  /** Returns the memory instance required to support the given read and write sets.
    * Includes banking, buffering depth, and the mapping of accesses to buffer ports.
    * Also calculates whether the associated memory should be considered a "buffer accumulator";
    * this occurs if at least one read and write occur in the same controller within a buffer.
    */
  protected def bankGroups(rdGroups: Set[Set[AccessMatrix]], wrGroups: Set[Set[AccessMatrix]]): Either[Issue,Seq[Instance]] = {
    val reads = rdGroups.flatten
    val ctrls = reads.map(_.parent)
    val writes = reachingWrites(reads,wrGroups.flatten,isGlobal)
    val reachingWrGroups = wrGroups.map{grp => grp intersect writes }.filterNot(_.isEmpty)
    val bankingOptionsIds: List[List[Int]] = combs(List(List.tabulate(bankViews.size){i => i}, List.tabulate(nStricts.size){i => i}, List.tabulate(aStricts.size){i => i}, List.tabulate(dimensionDuplication.size){i => i}))
    val attemptDirectives: Seq[BankingOptions] = bankingOptionsIds
        .map{ addr => BankingOptions(bankViews(addr(0)), nStricts(addr(1)), aStricts(addr(2)), dimensionDuplication(addr(3))) }
        .sortBy{x => (x.view.P, x.N.P, x.alpha.P, x.regroup.P)}
        .filter{x => (x.view.isInstanceOf[Hierarchical] || (x.view.isInstanceOf[Flat] && (x.regroup.dims.size == 0 || x.regroup.dims.size == x.view.rank)))}
    val (metapipe, bufPorts, issue) = computeMemoryBufferPorts(mem, reads.map(_.access), writes.map(_.access))
    val depth = bufPorts.values.collect{case Some(p) => p}.maxOrElse(0) + 1
    val bankings: Map[BankingOptions, Map[Set[Set[AccessMatrix]], Seq[Banking]]] = strategy.bankAccesses(mem, rank, rdGroups, reachingWrGroups, attemptDirectives, depth)
    val result = if (bankings.nonEmpty) {
      if (issue.isEmpty) {
        ctrlTree((reads ++ writes).map(_.access)).foreach{x => dbgs(x) }
        val costs: Map[BankingOptions, Long] = bankings.map{case (scheme, banking) => 
          val c = banking.toList.map{case (rds, b) => cost(b,depth,rds,reachingWrGroups)}.sum
          dbgs(s"Scheme $scheme:")
          banking.foreach{x => dbgs(s"  - ${x._1.map(_.size)} readers -> ${x._2}")}
          scheme -> c
        }
        bankings.foreach{case (scheme,banking) => dbgs(s"Cost: ${costs(scheme)} for $scheme")}
        val winningScheme = costs.toSeq.sortBy(_._2).headOption.getOrElse(throw new Exception(s"Could not bank $mem!"))
        val winner = bankings(winningScheme._1)
        Right(
          winner.map{case (winningRdGrps, winningBanking) => 
            val padding = mem.stagedDims.map(_.toInt).zip(winningBanking.flatMap(_.Ps)).map{case(d,p) => (p - d%p) % p}
            val ports = computePorts(winningRdGrps,bufPorts) ++ computePorts(reachingWrGroups,bufPorts)
            val isBuffAccum = writes.cross(winningRdGrps.flatten).exists{case (wr,rd) => rd.parent == wr.parent }
            val accum = if (isBuffAccum) AccumType.Buff else AccumType.None
            val accTyp = mem.accumType | accum
            Seq(Instance(winningRdGrps,reachingWrGroups,ctrls,metapipe,winningBanking,depth,winningScheme._2,ports,padding,winningBanking.head.darkVolume,accTyp))
          }.flatten.toSeq
        )
      }
      else Left(issue.get)
    }
    else Left(UnbankableGroup(mem, reads, writes))

    dbgs(s"  Reads:")
    rdGroups.zipWithIndex.foreach{case (grp, i) => grp.foreach{m => dbgss(s"    grp $i: ", m) }}
    dbgs(s"  Writes:")
    reachingWrGroups.zipWithIndex.foreach{case (grp, i) => grp.foreach{m => dbgss(s"    grp $i: ", m) }}
    dbgs(s"  Result: $result")
    result
  }

  // TODO: Some code duplication here with groupAccesses
  protected def accessesConflict(a: AccessMatrix, b: AccessMatrix): Boolean = {
    val concurrent  = requireConcurrentPortAccess(a, b) || !willUnrollTogether(a,b)
    val conflicting = a.overlapsAddress(b) && !canBroadcast(a, b) && (a.segmentAssignments == b.segmentAssignments)
    val trueConflict = concurrent && conflicting
    if (trueConflict) dbgs(s"${a.short}, ${b.short}: Concurrent: $concurrent, Conflicting: $conflicting")
    trueConflict
  }

  protected def getGroupConflict(grpA: Set[AccessMatrix], grpB: Set[AccessMatrix]): Option[(AccessMatrix,AccessMatrix)] = {
    grpA.cross(grpB).find{case (a,b) => accessesConflict(a,b) }
  }

  protected def getInstanceConflict(a: Instance, b: Instance): Option[(AccessMatrix,AccessMatrix)] = {
    a.reads.cross(b.reads).mapFind{case (gA, gB) => getGroupConflict(gA, gB) } orElse
    a.writes.cross(b.writes).mapFind{case (gA, gB) => getGroupConflict(gA, gB) }
  }

  /** Should not attempt to merge instances if any of the following conditions hold:
    *   1. The two instances have a common LCA controller (means they require separate banking)
    *   2. The two instances result in hierarchical buffers (for now)
    *   3. Either instance is an accumulator and there is at least one pipelined ancestor controller.
    */
  protected def getMergeAttemptError(a: Instance, b: Instance): Option[String] = {
    lazy val reads = a.reads.flatten ++ b.reads.flatten
    lazy val writes = a.writes.flatten ++ b.writes.flatten
    lazy val metapipes = findAllMetaPipes(reads.map(_.access), writes.map(_.access)).keys
    val commonCtrl = a.ctrls intersect b.ctrls
    val conflicts  = getInstanceConflict(a, b)

    if (commonCtrl.nonEmpty && !isGlobal)
      Some(s"Control conflict: Common control (${commonCtrl.mkString(",")})")
    else if (conflicts.nonEmpty)
      Some(s"Instances conflict: ${conflicts.get._1.short} / ${conflicts.get._2.short}")
    else if (metapipes.size > 1)
      Some("Ambiguous metapipes")
    else if (metapipes.nonEmpty && (a.accType | b.accType) >= AccumType.Reduce && !mem.shouldCoalesce)
      Some(s"Accumulator conflict (A Type: ${a.accType}, B Type: ${b.accType})")
    else
      None
  }

  /** Should not complete merging instances if any of the following hold:
    *   1. The merge was not successful
    *   2. The merge results in a multi-ported N-buffer (if this is disabled)
    *   3. The merged instance costs more than the total cost of the two separate instances
    */
  protected def getMergeError(i1: Instance, i2: Instance, i3: Instance): Option[String] = {
    if (i1.metapipe.isDefined && i2.metapipe.isDefined && !spatialConfig.enableBufferCoalescing)
      Some("Buffer conflict")
    else if (i3.cost > (i1.cost + i2.cost))
      Some(s"Too expensive: ${i3.cost} > ${i1.cost + i2.cost}")
    else
      None
  }

  /** Greedily banks and merges groups of readers into memory instances. */
  protected def mergeReadGroups(rdGroups: Set[Set[AccessMatrix]], wrGroups: Set[Set[AccessMatrix]]): Seq[Instance] = {
    dbgs("\n\n")
    dbgs(s"Merging memory instance groups:")
    val instances = ArrayBuffer[Instance]()

    rdGroups.zipWithIndex.foreach{case (grp,grpId) =>
      dbgs(s"Group #$grpId: ")
      state.logTab += 1
      // TODO: Should actually attempt to merge any duplicate banking scheme with any duplicate banknig scheme
      //       rather than computing cost of a single duplicate's banking scheme options independently.  Maybe
      //       some "expensive" schemes for two duplicates can merge into one duplicate with overall less cost
      bankGroups(Set(grp),wrGroups) match {
        case Right(insts) =>
          var instIdx = 0
          var merged = false
          while (instIdx < instances.length && !merged && insts.length == 1) {
            dbgs(s"Attempting to merge group #$grpId with instance #$instIdx: ")
            state.logTab += 1
            val i2 = instances(instIdx)

            val err = getMergeAttemptError(insts.head, i2)
            if (err.isEmpty) {
              bankGroups(insts.head.reads ++ i2.reads, wrGroups) match {
                case Right(i3) =>
                  if (i3.size == 1) {
                    val err = getMergeError(insts.head, i2, i3.head)
                    if (err.isEmpty) {
                      instances(instIdx) = i3.head
                      merged = true
                    }
                    else dbgs(s"Did not merge $grpId into instance $instIdx: ${err.get}")
                  }

                case Left(issue) =>
                  dbgs(s"Did not merge $grpId into instance $instIdx: ${issue.name}")
              }
            }
            else dbgs(s"Did not merge $grpId into instance $instIdx: ${err.get}")
            state.logTab -= 1
            instIdx += 1
          }
          if (!merged) {
            insts.foreach{i1 => instances += i1}
            dbgs(s"Result: Created instance #${instances.length-1}")
          }
          else {
            dbgs(s"Result: Merged $grpId into instance ${instIdx-1}")
          }

        case Left(issue) => raiseIssue(issue)
      }
      state.logTab -= 1
    }
    instances
  }

  /** Greedily banks and merges groups of writers into memory instances.
    * Only used if the memory has no readers.
    */
  protected def mergeWriteGroups(wrGroups: Set[Set[AccessMatrix]]): Seq[Instance] = {
    // Assumes that all writers reach some unknown reader external to Accel.
    bankGroups(Set.empty, wrGroups) match {
      case Right(instances) => instances
      case Left(issue)     => raiseIssue(issue); Nil
    }
  }
}
