package spatial.traversal
package banking

import argon._
import models.AreaEstimator
import poly.ISL
import spatial.issues.UnbankableGroup
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.node._
import spatial.util.spatialConfig
import utils.implicits.collections._
import utils.math._

import scala.collection.mutable.ArrayBuffer

class MemoryConfigurer[+C[_]](mem: Mem[_,C], strategy: BankingStrategy)(implicit state: State, isl: ISL, areamodel: AreaEstimator) {
  protected lazy val rank: Int = mem.sparseRank.length
  protected lazy val isGlobal: Boolean = mem.isArgIn || mem.isArgOut || mem.isHostIO

  lazy val bankViews: Seq[BankingView] = if (strategy.isInstanceOf[FullyBanked]) Seq(Hierarchical(rank))
                                         else if (mem.explicitBanking.isDefined && mem.explicitNs.size == 1) Seq(Flat(rank))
                                         else if (mem.explicitBanking.isDefined && mem.explicitNs.size > 1) Seq(Hierarchical(rank))
                                         else if (mem.isLineBuffer) Seq(Hierarchical(rank, Some(List(rank-1))))
                                         else if (rank > 1 && !mem.isNoHierarchicalBank && !mem.isNoFlatBank) Seq(Flat(rank), Hierarchical(rank)) 
                                         else if (mem.isNoHierarchicalBank) Seq(Flat(rank)) 
                                         else if (mem.isNoFlatBank) Seq(Hierarchical(rank)) 
                                         else Seq(Flat(rank))

  lazy val nStricts: Seq[NStrictness] = if (strategy.isInstanceOf[FullyBanked]) Seq(NRelaxed)
                                        else if (mem.explicitBanking.isDefined) Seq(UserDefinedN(mem.explicitNs))
                                        else if (mem.nConstraints.isEmpty) Seq(NBestGuess, NRelaxed)
                                        else mem.nConstraints
  lazy val aStricts: Seq[AlphaStrictness] = if (strategy.isInstanceOf[FullyBanked]) Seq(AlphaRelaxed)
                                            else if (mem.explicitBanking.isDefined) Seq(UserDefinedAlpha(mem.explicitAlphas))
                                            else if (mem.alphaConstraints.isEmpty) Seq(AlphaBestGuess, AlphaRelaxed)
                                            else mem.alphaConstraints
  lazy val dimensionDuplication: Seq[RegroupDims] = if (strategy.isInstanceOf[FullyBanked]) RegroupHelper.regroupNone
                                                    else if (mem.explicitBanking.isDefined) RegroupHelper.regroupNone
                                                    else if (mem.isNoFission) RegroupHelper.regroupNone
                                                    else if (mem.isFullFission) RegroupHelper.regroupAll(rank)
                                                    else if (mem.duplicateOnAxes.isDefined) mem.duplicateOnAxes.get.map{x: Seq[Int] => RegroupDims(x.toList)}.toList
                                                    else if (mem.isDuplicatable & !spatialConfig.enablePIR) RegroupHelper.regroupAny(rank) 
                                                    else RegroupHelper.regroupNone

  // Mapping from BankingOptions to its "duplicates."  Each "duplicate" contains a histogram (Seq[Int]), a Seq of auxilliary nodes (Seq[String]), and a 7-element Seq of its cost components (total, mem luts/ffs/brams, aux luts/ffs/brams) (Seq[Int])
  type DUPLICATE = (Seq[Banking], Seq[Int], Seq[String], Seq[Double])
  val schemesInfo = scala.collection.mutable.HashMap[Int,scala.collection.mutable.HashMap[(BankingOptions, Int), Seq[DUPLICATE]]]()
  private val latestSchemesInfo = scala.collection.mutable.HashMap[(BankingOptions,Int), Seq[DUPLICATE]]()

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
    dbgs(s"Explicit Banking: ${mem.explicitBanking}")
    dbgs(s"Force Explicit Banking: ${mem.forceExplicitBanking}")
    dbg(s"---------------------------------------------------------------------")
    val readers = mem.readers
    val writers = mem.writers
    resetData(readers, writers)

    val readMatrices = readers.flatMap{rd => rd.affineMatrices }
    val writeMatrices = writers.flatMap{wr => wr.affineMatrices}

    val instances = bank(readMatrices, writeMatrices)

    summarize(instances)
    finalize(instances)
    pirCheck(instances)
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
      List(inst.reads.iterator, inst.writes.iterator).foreach {
        _.zipWithIndex.foreach { case (grp, i) =>
          grp.foreach { a =>
            a.access.addGroupId(a.unroll, Set(i))
            a.access.addPort(dispatch, a.unroll, inst.ports(a))
            a.access.addDispatch(a.unroll, dispatch)
            dbgs(s"  Added port ${inst.ports(a)} to ${a.short}")
            dbgs(s"  Added dispatch $dispatch to ${a.short}")
          }
        }
      }
      (inst.reads.iterator.flatten ++ inst.writes.iterator.flatten).foreach{a =>
        a.access.addPort(dispatch, a.unroll, inst.ports(a))
        a.access.addDispatch(a.unroll, dispatch)
        dbgs(s"  Added port ${inst.ports(a)} to ${a.short}")
        dbgs(s"  Added dispatch $dispatch to ${a.short}")
      }

      if (inst.writes.flatten.isEmpty && mem.name.isDefined && !mem.hasInitialValues && !mem.isStreamIn) {
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
      if (mem.name.isDefined && !mem.keepUnused) {
        val msg = if (access.isReader) s"Read of memory ${mem.name.get} was unused. Read will be removed."
                  else s"Write to memory ${mem.name.get} is never used. Write will be removed."
        warn(access.ctx, msg)
        warn(access.ctx)
      }

      if (!mem.keepUnused) access.isUnusedAccess = true

      dbgs(s"  Unused access: ${stm(access)}")
    }

  }

  protected def pirCheck(instances: Seq[Instance]): Unit = {
    if (!spatialConfig.enablePIR || mem.isLockSRAM) return
    instances.zipWithIndex.foreach { case (inst, dispatch) =>
      def checkAccess(groups:Set[Set[AccessMatrix]]) = {
        // Mapping of access matrix => group id
        val groupMap = groups.zipWithIndex.flatMap { case (grp, gid) => grp.map { a => (a, gid) } }.toMap
        groups.flatten.groupBy { _.access }.foreach { case (access, ams) =>
          val gids = ams.map { a => groupMap(a) }
          if (gids.size > 1) {
            error(s"//TODO: Plasticine does not support unbanked unrolled access at the moment. ")
            error(s"mem=$mem (${mem.ctx} ${mem.name.getOrElse("")})")
            error(s"access=$access (${access.ctx})")
            error(s"AccessMatrix:")
            ams.foreach { a => 
              error(s"$a gid:${groupMap(a)}")
            }
            state.logError()
          }
        }
      }
      checkAccess(inst.reads)
      checkAccess(inst.writes)
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
        spatialConfig.enableBroadcast /* && !mem.isDualPortedRead */ && divergedIters(a, b, mem).values.forall(_.isDefined)
      }
    }
    else true
  }

  protected def groupAccesses(accesses: Set[AccessMatrix]): Set[Set[AccessMatrix]] = 
    if (spatialConfig.groupUnrolledAccess && !mem.isLockSRAM) groupAccessUnroll(accesses)
    else groupAccessesDefault(accesses)

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
  protected def groupAccessesDefault(accesses: Set[AccessMatrix]): Set[Set[AccessMatrix]] = {
    val groups = ArrayBuffer[Set[AccessMatrix]]()
    val isWrite = accesses.exists(_.access.isWriter)
    val tp = if (isWrite) "Write" else "Read"

    dbgs(s"  Grouping ${accesses.size} ${tp}s: ")

    import scala.math.Ordering.Implicits._  // Seq ordering
    val sortedAccesses = accesses.toSeq.sortBy{x => (x.access.toString, x.unroll)}

    sortedAccesses.foreach{a =>
      dbg(s"    Access: ${a.short} [${a.parent}]")
      val grpId = {
        if (mem.parent == Ctrl.Host) groups.zipWithIndex.indexWhere{case (grp,i) => 
          val samePort = grp.filter{b => requireConcurrentPortAccess(a,b)}
          if (samePort.nonEmpty && !mem.shouldIgnoreConflicts) dbgs(s"      WARNING: $mem has conflictable writers.  Do you want to add .conflictable flag to it?")
          samePort.nonEmpty && !mem.shouldIgnoreConflicts
        }
        else groups.zipWithIndex.indexWhere{case (grp, i) =>
          // Filter for accesses that require concurrent port access AND either don't overlap or are identical.
          // Should drop in data broadcasting node in this case
          val samePort = grp.filter{b => requireConcurrentPortAccess(a, b) }
          // A conflict occurs if there are accesses on the same port with overlapping addresses
          // for which we can cannot create a broadcaster read
          // (either because they are not lockstep, not reads, or because broadcasting is disabled)
          val conflicts = samePort.filter{b => !mem.isLockSRAM && overlapsAddress(a,b) && !canBroadcast(a, b) && (a.segmentAssignment == b.segmentAssignment)}
          if (conflicts.nonEmpty) dbg(s"      Group #$i conflicts: <${conflicts.size} accesses>")
          else                    dbg(s"      Group #$i conflicts: <none>")
          if (config.enLog) conflicts.foreach{b => logs(s"        ${b.short} [${b.parent}]")  }

          if (samePort.nonEmpty)  dbg(s"      Group #$i same port: <${samePort.size} accesses>")
          else                    dbg(s"      Group #$i same port: <none> ")
          if (config.enLog) samePort.foreach{b => logs(s"        ${b.short} [${b.parent}]")}

          val noConflicts = if (isWrite) {
            if (conflicts.nonEmpty && !mem.shouldIgnoreConflicts) {
              val contexts = conflicts map {_.access.ctx}
              val uids = conflicts map {_.unroll}
              warn(s"Detected potential write conflicts on ${a.access.ctx} (uid: ${a.unroll}) and ${contexts.mkString(", ")} (uid: ${uids.mkString(", ")}) to memory ${mem.ctx} (${mem.name.getOrElse("")})")
              warn(s"    Consider either:")
              warn(s"       1) Remove parallelization on all ancestor controllers")
              warn(s"       2) Declare this memory inside the innermost outer controller with parallelization > 1")
              warn(s"       3) Manually deconflicting them and add .conflictable flag to the memory. Use this if you know" +
                       "the address pattern is actually safe to bank (i.e. accesses are timed so they won't conflict or are masked so they won't all trigger at the same time")
              warn(s"    Note that banking analysis may hang or crash here...")
              true
            } else if (conflicts.nonEmpty && mem.shouldIgnoreConflicts) {
//              warn(s"Detected potential write conflicts on ${a.access.ctx} (uid: ${a.unroll}) and ${conflicts.head.access.ctx} (uid: ${conflicts.head.unroll}) to memory ${mem.ctx} (${mem.name.getOrElse("")})")
//              warn(s"    These are technically unbankable but you signed the waiver (by adding .conflictable) that says you know what you are doing")
              false
            } else conflicts.isEmpty
          } else conflicts.isEmpty
          samePort.nonEmpty && noConflicts
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

  /*
   * Given a list and a reduction lambda, 
   *  if a and b can be reduced, reduce return Some(c) else None
   * continues call reduce function on list until no two element in the list can be
   * further reduced. 
   * Notice there might be different result based on the order called on elements in list
   * */
  def partialReduce[A](list:List[A])(reduce:(A,A) => Option[A]):List[A] = {
    val queue = scala.collection.mutable.ListBuffer[A]()
    val reduced = scala.collection.mutable.Queue[A]()
    queue ++= list
    while (queue.nonEmpty) {
      val a = queue.remove(0)
      val cs = queue.flatMap { b => reduce(a,b).map { c => (b,c) } }
      if (cs.isEmpty) reduced += a
      else {
        cs.foreach { case (b,c) =>
          queue -= b
          queue +=c
        }
      }
    }
    reduced.toList
  }

  private def overlapsAddress(a: AccessMatrix, b: AccessMatrix): Boolean = {
    val substRules = divergedIters(a, b, mem)
    val keyRules: scala.collection.immutable.Map[Idx,(Idx,Int)]  = accessIterators(a.access, mem).zipWithIndex.collect{
      case(iter,i) if substRules.contains(iter) && substRules(iter).isDefined =>
        if (substRules(iter).get != 0) dbgs(s"      WARNING: ${a.access} {${a.unroll}} - ${b.access} {${b.unroll}} have totally lockstepped iterator, $iter, with offset ${substRules(iter).get}") 
        iter -> (iter, substRules(iter).get)
      case(iter,i) if substRules.contains(iter) && !substRules(iter).isDefined && a.matrix.keys.contains(iter) =>
        dbgs(s"      WARNING: ${a.access} {${a.unroll}} - ${b.access} {${b.unroll}} have totally dephased iterator, $iter")
        return true
        // (iter -> (boundVar[I32], 0))
    }.toMap
    val newa = a.substituteKeys(keyRules)
    newa.overlapsAddress(b)
  }

  protected def groupAccessUnroll(accesses: Set[AccessMatrix]): Set[Set[AccessMatrix]] = {
    val isWrite = accesses.exists(_.access.isWriter)
    val tp = if (isWrite) "Write" else "Read"

    dbgs(s"  Grouping ${accesses.size} ${tp}s: ")

    // Cache results. Potentially improve performance
    val cache = scala.collection.mutable.Map[(AccessMatrix, AccessMatrix), Boolean]()
    // Two accesses can be grouped if they are in the same port and they don't conflict
    def canGroup(a:AccessMatrix, b:AccessMatrix) = cache.getOrElseUpdate((a,b),{
      val samePort = requireConcurrentPortAccess(a, b)
      val conflict = if (samePort) {overlapsAddress(a,b) && !canBroadcast(a, b) && (a.segmentAssignment == b.segmentAssignment)} else false
      dbgs(s"   ${a.short} ${b.short} samePort:$samePort conflict:$conflict")
      var canConflict = conflict
      if (canConflict && mem.shouldIgnoreConflicts) {
//        warn(s"Detected potential conflicts on ${a.access.ctx} (uid: ${a.unroll}) and ${a.access.ctx} (uid: ${a.unroll}) to memory ${mem.ctx} (${mem.name.getOrElse("")})")
//        warn(s"    These are technically unbankable but you signed the waiver (by adding .conflictable) that says you know what you are doing")
        canConflict = false
      }
      samePort && !canConflict
    })

    if (mem.parent == Ctrl.Host) return Set(accesses)

    // Start to build groups within each access symbol. 
    import scala.math.Ordering.Implicits._  // Seq ordering
    val accessGroups = accesses.groupBy { _.access }.map { case (access, as) =>
      if (access.segmentMapping.values.exists { _ > 0 }) {
        error(s"Cannot group by unrolled access for banking on ${access} (${access.ctx}) due to dependency between iterations")
        error(s"Try turn off --bank-groupUnroll")
        state.logError()
      }
      val grps = as.toList.sortBy { _.unroll }.foldLeft(Seq[Set[AccessMatrix]]()) { case (grps, a) =>
        val gid = grps.indexWhere { grp => grp.forall { b => canGroup(a,b) } }
        if (gid == -1) grps :+ Set(a)
        else grps.zipWithIndex.map { case (grp, `gid`) => grp+a; case (grp, gid) => grp }
      }
      dbgs(s"access group $access: [${grps.map{_.size}.mkString(",")}]")
      grps
    }

    // Next try to merge groups across access symbols.
    val groups = partialReduce(accessGroups.flatten.toList) { case (grp1, grp2) =>
      if (grp1.forall { a => grp2.forall { b => canGroup(a,b) }}) Some(grp1 ++ grp2) else None
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
    val controllerLCA = lca.ancestors.collectFirst{case x if x.isInnerControl && !x.isSwitch => x} // Outermost controller that is inner controller
    (a.access == b.access && a.unroll != b.unroll) ||
      lca.isInnerPipeLoop ||
      (lca.isInnerSeqControl && lca.isFullyUnrolledLoop) ||
      (lca.isOuterPipeLoop && !isWrittenIn(lca)) ||
      (a.access.delayDefined && b.access.delayDefined && a.access.parent == b.access.parent && a.access.fullDelay == b.access.fullDelay) || 
      ((a.access.delayDefined && b.access.delayDefined && a.access.parent == b.access.parent && a.access.fullDelay != b.access.fullDelay) && (controllerLCA.isDefined && controllerLCA.get.isLoopControl)) ||
      (lca.isParallel || (a.access.parent == b.access.parent && (Seq(lca) ++ lca.ancestors).exists(_.willUnroll))) || (lca.isOuterControl && lca.isStreamControl)
  }


  protected def bank(readers: Set[AccessMatrix], writers: Set[AccessMatrix]): Seq[Instance] = {
    val rdGroups = if (mem.forceExplicitBanking) Set(readers) else groupAccesses(readers)
    val wrGroups = if (mem.forceExplicitBanking) Set(writers) else groupAccesses(writers)
    if      (readers.nonEmpty) mergeReadGroups(rdGroups, wrGroups)
    else if (writers.nonEmpty) mergeWriteGroups(wrGroups)
    else Seq(Instance.Unit(rank))
  }

  /** Returns an approximation of the cost for the given banking strategy. */
  def cost(banking: Seq[Banking], depth: Int, rdGroups: Set[Set[AccessMatrix]], wrGroups: Set[Set[AccessMatrix]]): DUPLICATE = {
    // Partition based on direct/xbar banking (TODO: Determine partial xBars here)
    val histR: Map[Int, Int] = rdGroups.flatten.groupBy{x => x.bankMuxWidth(banking.map(_.nBanks), banking.map(_.stride), banking.flatMap(_.alphas))}.map{case (width, accs) => width -> accs.size }
    val histW: Map[Int, Int] = wrGroups.flatten.groupBy{x => x.bankMuxWidth(banking.map(_.nBanks), banking.map(_.stride), banking.flatMap(_.alphas))}.map{case (width, accs) => width -> accs.size }
    val histCombined: Map[Int, (Int,Int)] = histR.map{case (width, siz) => width -> (siz, histW.getOrElse(width, 0)) } ++ histW.collect{case (width, siz) if !histR.contains(width) => width -> (0,siz) }

    // Relative scarcity of resource, roughly % of board used (TODO: Extract from target device, these numbers were just ripped from zcu)
    val lutWeight = 34260 / 100
    val ffWeight = 548160 / 100
    val bramWeight = 912 / 100

    val allDims = mem.stagedDims.map(_.toInt)
    val allB = banking.map(_.stride)
    val allN = banking.map(_.nBanks)
    val allAlpha = banking.flatMap(_.alphas)
    val allP = banking.flatMap(_.Ps)
    val histRaw = histCombined.toList.sortBy(_._1).flatMap { x => List(x._1, x._2._1, x._2._2) }

    mem.asInstanceOf[Sym[_]] match {
      case m:SRAM[_,_] => 
        val auxNodes = (rdGroups ++ wrGroups).flatten.flatMap { x => x.arithmeticNodes(allN, allB, allAlpha) }.toList
        val auxWeights = auxNodes.map{case (name,a,b) => 
          val l = areamodel.estimateArithmetic("LUTs", name, List(a.getOrElse(0), b.getOrElse(0), 32,0,1)) / lutWeight
          val f = areamodel.estimateArithmetic("FFs", name, List(a.getOrElse(0), b.getOrElse(0), 32,0,1)) / ffWeight
          val br = (areamodel.estimateArithmetic("RAMB18", name, List(a.getOrElse(0), b.getOrElse(0), 32,0,1)) + areamodel.estimateArithmetic("RAMB32", name, List(a.getOrElse(0), b.getOrElse(0), 32,0,1))) / bramWeight 
          (l,f,br)
        }
        val auxLuts = auxWeights.map(_._1).sum
        val auxFFs = auxWeights.map(_._2).sum
        val auxBrams = auxWeights.map(_._3).sum
        val luts = areamodel.estimateMem("LUTs", "SRAMNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) / lutWeight
        val ffs = areamodel.estimateMem("FFs", "SRAMNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) / ffWeight
        val bram = (areamodel.estimateMem("RAMB18", "SRAMNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) + areamodel.estimateMem("RAMB32", "SRAMNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw)) / bramWeight
        val c = luts + ffs + bram + auxLuts + auxFFs + auxBrams
        dbgs(s"          Access Hist:")
        dbgs(s"          | width | R | W |")
        histRaw.grouped(3).foreach{x => dbgs(s"          | ${x(0)} | ${x(1)} | ${x(2)} |")}
        dbgs(s"        - Duplicate costs $c (SRAM LUTs: $luts%, FFs: $ffs%, BRAMs: $bram%, Auxiliary LUTs: $auxLuts%, FFs: $auxFFs%, BRAMs: $auxBrams%)")
        (banking, histRaw, auxNodes.map{x => s"${x._1}(${x._2},${x._3})"}, Seq(c, luts, ffs, bram, auxLuts, auxFFs, auxBrams))
      case m:RegFile[_,_] =>
        val luts = areamodel.estimateMem("LUTs", "RegFileNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) / lutWeight
        val ffs = areamodel.estimateMem("FFs", "RegFileNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) / ffWeight
        val bram = (areamodel.estimateMem("RAMB18", "RegFileNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) + areamodel.estimateMem("RAMB32", "RegFileNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw)) / bramWeight
        val c = luts + ffs + bram
        dbgs(s"          Access Hist:")
        dbgs(s"          | width | R | W |")
        histRaw.grouped(3).foreach{x => dbgs(s"          | ${x(0)} | ${x(1)} | ${x(2)} |")}
        dbgs(s"        - Duplicate costs $c (LUTs: $luts%, FFs: $ffs%, BRAMs: $bram%)")
        (banking, histRaw, Seq(), Seq(c, luts, ffs, bram, 0,0,0))
      case m:LineBufferNew[_] =>
        val luts = areamodel.estimateMem("LUTs", "LineBufferNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) / lutWeight
        val ffs = areamodel.estimateMem("FFs", "LineBufferNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) / ffWeight
        val bram = (areamodel.estimateMem("RAMB18", "LineBufferNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) + areamodel.estimateMem("RAMB32", "LineBufferNew", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw)) / bramWeight
        val c = luts + ffs + bram
        dbgs(s"          Access Hist:")
        dbgs(s"          | width | R | W |")
        histRaw.grouped(3).foreach{x => dbgs(s"          | ${x(0)} | ${x(1)} | ${x(2)} |")}
        dbgs(s"        - Duplicate costs $c (LUTs: $luts%, FFs: $ffs%, BRAMs: $bram%)")
        (banking, histRaw, Seq(), Seq(c, luts, ffs, bram, 0,0,0))
      case _ => 
        val luts = areamodel.estimateMem("LUTs", "", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) / lutWeight
        val ffs = areamodel.estimateMem("FFs", "", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) / ffWeight
        val bram = (areamodel.estimateMem("RAMB18", "", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw) + areamodel.estimateMem("RAMB32", "", allDims, 32, depth, allB, allN, allAlpha, allP, histRaw)) / bramWeight
        val c = luts + ffs + bram
        dbgs(s"          Access Hist:")
        dbgs(s"          | width | R | W |")
        histRaw.grouped(3).foreach{x => dbgs(s"          | ${x(0)} | ${x(1)} | ${x(2)} |")}
        dbgs(s"        - Duplicate costs $c (LUTs: $luts%, FFs: $ffs%, BRAMs: $bram%)")
        (banking, histRaw, Seq(), Seq(c, luts, ffs, bram, 0,0,0))
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
    val writes = if (mem.keepUnused) wrGroups.flatten else reachingWrites(reads,wrGroups.flatten,isGlobal)
    val reachingWrGroups = wrGroups.map{grp => grp intersect writes }.filterNot(_.isEmpty)
    // All possible combinations of banking characteristics
    val bankingOptionsIds: List[List[Int]] = combs(List(List.tabulate(bankViews.size){i => i}, List.tabulate(nStricts.size){i => i}, List.tabulate(aStricts.size){i => i}, List.tabulate(dimensionDuplication.size){i => i}))
    val allAttemptDirectives: Seq[BankingOptions] = bankingOptionsIds
        .map{ addr => BankingOptions(bankViews(addr(0)), nStricts(addr(1)), aStricts(addr(2)), dimensionDuplication(addr(3))) }
        .sortBy{x => (x.view.P, x.N.P, x.alpha.P, x.regroup.P)}
        .filter{x => x.view.isInstanceOf[Hierarchical] || (x.view.isInstanceOf[Flat] && (x.regroup.dims.size == 0 || x.regroup.dims.size == x.view.rank)) }
    if (allAttemptDirectives.size == 0) {
      error(s"Unable to search for banking on ${mem.fullname}:")
      error(s"  ${mem.ctx})")
      error(s"  ${mem.ctx.content.getOrElse("<???>")}")
      throw new Exception(s"No banking options allowed!")
    }
    // Partition directives list based on the "good" ones and "bad" ones (i.e. likelihood of successful scheme existing), and then repack them
    val (goodDirectives, badDirectives) = allAttemptDirectives.partition{opts => !opts.undesired}
    val (goodSameDirectives, goodDiffDirectives) = goodDirectives.partition{opts => opts.N.P == opts.alpha.P}
    val attemptDirectives = goodSameDirectives ++ goodDiffDirectives ++ badDirectives
    val (metapipe, bufPorts, issue) = computeMemoryBufferPorts(mem, reads.map(_.access), writes.map(_.access))
    val depth = bufPorts.values.collect{case Some(p) => p}.maxOrElse(0) + 1
    val bankings: Map[BankingOptions, Map[Set[Set[AccessMatrix]], Seq[Seq[Banking]]]] = strategy.bankAccesses(mem, rank, rdGroups, reachingWrGroups, attemptDirectives, depth)
    dbgs(s"solution bankings are $bankings")
    val result = if (bankings.nonEmpty) {
      if (issue.isEmpty) {
        latestSchemesInfo.clear()
        ctrlTree((reads ++ writes).map(_.access)).foreach{x => dbgs(x) }
        dbgs(s"**************************************************************************************")
        dbgs(s"Analyzing costs for banking schemes found for ${mem.fullname}")
        val costs: Map[(BankingOptions,Int), Double] = bankings.flatMap{case (scheme, banking) =>
          val expanded_costs: List[List[Double]] = banking.toList.zipWithIndex.map { case ((rds, opts), inst) => // Cost for each option over all dups
            opts.zipWithIndex.map { case (opt, optId) => // Cost for each option in this dup
              dbgs(s"Scheme $scheme option $optId instance $inst:")
              dbgs(s"  - ${rds.map(_.size).sum} readers connect to duplicate #$inst (${opt})")
              val dup = cost(opt, depth, rds,  reachingWrGroups)
              latestSchemesInfo += ((scheme,optId)-> {
                latestSchemesInfo.getOrElse((scheme,optId), Seq()) ++ Seq(dup)
              })
              dup._4.head
            }.toList
          }
          val costs_per_option: List[Double] = expanded_costs.reduce[List[Double]]{case (a: List[Double],b: List[Double]) => a.zip(b).map{case (a,b) => a+b}}
          costs_per_option.zipWithIndex.map{case (c, j) => (scheme,j) -> c }.toMap
        }
        dbgs(s"***** Cost summary *****")
        bankings.foreach{case (scheme,banking) => banking.head._2.zipWithIndex.foreach{ case (b,optId) => dbgs(s"Cost: ${costs((scheme,optId))} for version $optId of $scheme")}}
        dbgs(s"**************************************************************************************")
        val winningScheme: ((BankingOptions, Int), Double) = costs.toSeq.sortBy(_._2).headOption.getOrElse(throw new Exception(s"Could not bank $mem!"))
        val winner: Map[Set[Set[AccessMatrix]], Seq[Banking]] = bankings(winningScheme._1._1).map { case (acgrp, opts) => acgrp -> opts(winningScheme._1._2) }
        Right(
          winner.flatMap { case (winningRdGrps, winningBanking) =>
            val padding = mem.stagedDims.map(_.toInt).zip(winningBanking.flatMap(_.Ps)).map { case (d, p) => (p - d % p) % p }
            val ports = computePorts(winningRdGrps, bufPorts) ++ computePorts(reachingWrGroups, bufPorts)
            val isBuffAccum = writes.cross(winningRdGrps.flatten).exists { case (wr, rd) => rd.parent == wr.parent }
            val accum = if (isBuffAccum) AccumType.Buff else AccumType.None
            val accTyp = mem.accumType | accum
            Seq(Instance(winningRdGrps, reachingWrGroups, ctrls, metapipe, winningBanking, depth, winningScheme._2, ports, padding, accTyp))
          }.toSeq
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
    val conflicting = overlapsAddress(a,b) && !canBroadcast(a, b) && (a.segmentAssignment == b.segmentAssignment)
    val trueConflict = concurrent && conflicting && !mem.isReg && !mem.isLockSRAM
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
    if (mem.isMustMerge) None
    else {
      lazy val reads = a.reads.flatten ++ b.reads.flatten
      lazy val writes = a.writes.flatten ++ b.writes.flatten
      lazy val metapipes = findAllMetaPipes(reads.map(_.access), writes.map(_.access)).keys
      val commonCtrl = a.ctrls intersect b.ctrls
      val conflicts  = getInstanceConflict(a, b)

      if (spatialConfig.enablePIR) Some("Do not merge accesses for plasticine")
      else if (commonCtrl.nonEmpty && !isGlobal && !mem.shouldIgnoreConflicts)
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
  }

  /** Should not complete merging instances if any of the following hold:
    *   1. The merge was not successful
    *   2. The merge results in a multi-ported N-buffer (if this is disabled)
    *   3. The merged instance costs more than the total cost of the two separate instances
    */
  protected def getMergeError(i1: Instance, i2: Instance, i3: Instance): Option[String] = {
    if (mem.isMustMerge) None
    else if (i1.metapipe.isDefined && i2.metapipe.isDefined && !spatialConfig.enableBufferCoalescing)
      Some("Buffer conflict")
    else if (i3.cost > (i1.cost + i2.cost) && !mem.hasDestructiveReads && !mem.isReg && !mem.isLockSRAM)
      Some(s"Too expensive to merge addressable instances: ${i3.cost} > ${i1.cost + i2.cost}")
    else if (mem.hasDestructiveReads && mem.consumers.exists{x => x match {case Op(x:OpMemReduce[_,_]) => x.accum == mem; case _ => false}})
      Some(s"Cannot merge instances when reads are destructive and mem is used as an accumulator")
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
          val unmergedSchemesInfo = latestSchemesInfo.clone()
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

                case Left(issue) => dbgs(s"Did not merge $grpId into instance $instIdx: ${issue.name}")
              }
            }
            else {
              dbgs(s"Did not merge $grpId into instance $instIdx: ${err.get}")
            }
            state.logTab -= 1
            instIdx += 1
          }
          if (!merged) {
            schemesInfo += (instances.length -> unmergedSchemesInfo)
            insts.foreach{i1 => instances += i1}
            dbgs(s"Result: Created instance #${instances.length-1}")
          }
          else {
            schemesInfo += ({instIdx-1} -> latestSchemesInfo)
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
