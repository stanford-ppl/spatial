package spatial.traversal.banking

import argon._
import utils.implicits.collections._
import utils.math._
import poly.{ConstraintMatrix, ISL, SparseMatrix, SparseVector}
import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.spatialConfig
import spatial.metadata.types._

import scala.collection.JavaConverters._
import scala.collection.mutable
//import spatial.util.IntLike._
import scala.collection.mutable.ArrayBuffer


case class AccessView(activeDims: Seq[Int], fullDims: Seq[Int], access: SparseMatrix[Idx]){
  def activeAccess: SparseMatrix[Idx] = access.sliceDims(activeDims)
  def complementAccess: SparseMatrix[Idx] = access.sliceDims(fullDims.diff(activeDims))
  def priorComplementAccess: SparseMatrix[Idx] = access.sliceDims(fullDims.take(activeDims.max))
  def postComplementAccess: SparseMatrix[Idx] = access.sliceDims(fullDims.takeRight(fullDims.max - activeDims.max))
}

case class ExhaustiveBanking()(implicit IR: State, isl: ISL) extends BankingStrategy {
  // TODO[4]: What should the cutoff be for starting with powers of 2 versus exact accesses?

  private val MAGIC_CUTOFF_N = 1.4
  private val k = boundVar[I32]
  private val k0 = boundVar[I32]
  private val k1 = boundVar[I32]
  //private val Bs = Seq(2, 4, 8, 16, 32, 64, 128, 256) // Now set in metadata

  // Mapping to keep track of which AccessMatrix is rewritten as which
  private val accMatrixMapping = scala.collection.mutable.HashMap[AccessMatrix, AccessMatrix]()
  // Mapping te keep track of which rewritten AccessMatrix corresponds to which SparseMatrix for proper re-bundling after computing banking
  private val sparseMatrixMapping = scala.collection.mutable.HashMap[SparseMatrix[Idx], SingleAccessGroup]()
  // Mapping te keep track of which read SparseMatrix slices rewritten as low rank corresponds to which original full-rank read SparseMatrix
  private val lowRankMapping = scala.collection.mutable.HashMap[SparseMatrix[Idx], Set[SparseMatrix[Idx]]]()
  // Helper for replacing sparse matrix with its original access matrix
  private def reverseAM(a: SparseMatrix[Idx]): SingleAccessGroup = lowRankMapping(a).map(sparseMatrixMapping).flatten.map(accMatrixMapping)
  // Cache for skipping ahead to correct banking solution for patterns/axes that have already been solved
  private val solutionCache = scala.collection.mutable.HashMap[(Set[Set[SparseMatrix[Idx]]], NStrictness, AlphaStrictness, Seq[Int]), Option[FullBanking]]()
  // Map for tracking which kinds of schemes already have a solution, used depending on what the banking effort is set to.  Tracks BankingView and RegroupDims
  private val schemesFoundCount = scala.collection.mutable.HashMap[(BankingView,RegroupDims), Int]()

  /** Returns a Map from Seq(banking schemes) to the readers for these schemes.  
    * Generally, it will contain Map(Seq(flat_scheme, nested_scheme) -> all readers) but in 
    * the case of dephased accesses that cannot be banked together, there will be multiple 
    * entries in the map who each point to a different partition of readers
    */
  override def bankAccesses(
    mem:    Sym[_],
    rank:   Int,
    reads:  AccessGroups,
    writes: AccessGroups,
    attemptDirectives: Seq[BankingOptions],
    depth: Int
  ): Map[BankingOptions, Map[AccessGroups, FullBankingChoices]] = {
    // Generate substitution rules for each iter for each uid.  Given iter and its uid, generate a rule to replace it with a new iter and an offset to include in the access patterns c column
    def generateSubstRules(accs: AccessGroups): scala.collection.immutable.Map[(Idx,Seq[Int]),(Idx,Int)] = {
      val toRewrite: Map[(Idx, Seq[Int]), Option[Int]] = if (mem.forceExplicitBanking) Map() else accs.flatten.flatMap { a => dephasingIters(a, Seq.fill(a.unroll.size)(0), mem) }.toMap
      toRewrite.map {
        case ((i, addr), ofs) if ofs.isDefined => (i, addr) -> (i, ofs.get)
        case ((i, addr), ofs) if ofs.isEmpty => (i, addr) -> (boundVar[I32], 0)
      }.toMap
    }
    def rewriteAccesses(accs: AccessGroups, rules: Map[(Idx,Seq[Int]),(Idx,Int)]): AccessGroups = {
      accs.map { grp =>
        grp.map { a =>
          val aIters = accessIterators(a.access, mem)
          val keyRules: scala.collection.immutable.Map[Idx, (Idx, Int)] = aIters.zipWithIndex.collect {
            // TODO: Figure out why this getDephasedUID is necessary?  Its logic is hard to follow but it seems like it may be doing-
            //       some kind of lookup for a uid fork point up to that point and no deeper, but I'm not sure the synch logic
            //       needs this anymore.  BankLockstep breaks if you just do a.unroll though...
            case (iter, i) if rules.contains((iter, getDephasedUID(aIters, a.unroll, i))) =>
              iter -> rules((iter, getDephasedUID(aIters, a.unroll, i)))
            // case (iter, i) if rules.contains((iter, a.unroll)) =>
            //   iter -> rules((iter, a.unroll))
          }.toMap
          if (keyRules.nonEmpty) {
            mem.addDephasedAccess(a.access)
            dbgs(s"Substituting due to dephasing: $keyRules")
          }
          val newa = a.substituteKeys(keyRules)
          accMatrixMapping += (newa -> a)
          newa
        }
      }
    }
    def repackageGroup(grp: Seq[SparseMatrix[Idx]], dims: List[Int], isRd: Boolean): Seq[Seq[SparseMatrix[Idx]]] = {
      val fullStrategy = Seq.tabulate(rank){i => i}
      // For hierarchical views, regroup accesses based on whether their "complements" are non-interfering AND their projection is not unique
      val grpViews: Seq[AccessView] = grp.map{mat =>
        val t = AccessView(dims, fullStrategy, mat)
        if (isRd) lowRankMapping += (t.activeAccess -> {lowRankMapping.getOrElse(t.activeAccess, Set()) ++ Set(mat)}) 
        t
      }
      dbgs(s"")
      val projectionsToBank = ArrayBuffer(ArrayBuffer[AccessView]())
      grpViews.foreach { current: AccessView =>
        var grpId = 0
        var placed = false
        while (!placed & grpId < projectionsToBank.size) {
          val prevAccs = projectionsToBank(grpId)
          // Check if this access is "already handled" by a different dimension.
          //  "Already handled" means that EITHER the prior- or post- complement access has a non-conflicting dimension,
          //                                   OR no dimensions conflict and dims == first dim.
          //     We need to do the XOR of these to make sure we don't account for non-conflicting twice and end up with too few
          //     banks, such as in the case of a diagonal lockstep access pattern.  If no dim conflicts, then no projection will
          //     take care of it so check for the case of no dims conflicting and we are on the first dim
          val alreadyHandled: scala.Boolean = prevAccs.exists { other =>
            val priordiff = current.priorComplementAccess - other.priorComplementAccess
            val nonconflictingPriorDim = priordiff.rows.exists { row =>
              val patternForDim = Seq(1) * SparseMatrix[Idx](Seq(row)) === 0
              val emptyPolytope = patternForDim.andDomain.isEmpty
              emptyPolytope
            }
            val postdiff = current.postComplementAccess - other.postComplementAccess
            val nonconflictingPostDim = postdiff.rows.exists { row =>
              val patternForDim = Seq(1) * SparseMatrix[Idx](Seq(row)) === 0
              val emptyPolytope = patternForDim.andDomain.isEmpty
              emptyPolytope
            }
            nonconflictingPriorDim ^ nonconflictingPostDim
          }
          val noDimsActuallyHandled: scala.Boolean = prevAccs.exists { other =>
            val alldiff = current.access - other.access
            val nonconflictingAllDim = alldiff.rows.forall { row =>
              val patternForDim = Seq(1) * SparseMatrix[Idx](Seq(row)) === 0
              val emptyPolytope = patternForDim.andDomain.isEmpty
              emptyPolytope
            }
            nonconflictingAllDim && dims == List(0)
          }
          if (!alreadyHandled || noDimsActuallyHandled) {projectionsToBank(grpId) += current; placed = true}
          else if (grpId == projectionsToBank.size-1) {projectionsToBank += ArrayBuffer(current); placed = true}
          else grpId = grpId + 1
        }
      }
      projectionsToBank.map(_.map(_.activeAccess).toSeq)
    }

//      // TODO: Figure out what this is doing.  It seems unnecessary
//      regrp.map{newgrp =>
//        var firstInstances: Set[SparseMatrix[Idx]] = Set.empty
//
//        /** True if the sliced matrix has any of the following:
//          *   - If the access is identical to another within this group
//          *   - If it is the first time we are seeing this "sliced matrix" within this group
//          */
//        def isUniqueSliceInGroup(i: Int): Boolean = {
//          // Current (sliced matrix, complement matrix) tuple
//          val current = newgrp(i)
//
//          // Others in group (Sequence of (sliced matrix, complement matrix) tuples)
//          val pairsExceptCurrent = newgrp.patch(i,Nil,1)
//
//          val totalCollision = pairsExceptCurrent.exists{other => other.access == current.access }
//          val firstTime          = !firstInstances.contains(current.activeAccess)
//
//          totalCollision || firstTime
//        }
//
//        Seq(newgrp.zipWithIndex.collect{case (current,i) if isUniqueSliceInGroup(i) =>
//          firstInstances += current.activeAccess
//          current.activeAccess
//        }:_*)
//      }
//    }

    def findSchemes(myReads: Set[Seq[SparseMatrix[Idx]]], myWrites: Set[Seq[SparseMatrix[Idx]]], hostReads: SingleAccessGroup): Map[BankingOptions, Map[AccessGroups, FullBankingChoices]] = {
      val effort = mem.bankingEffort
      schemesFoundCount.clear()
      def markFound(scheme: BankingOptions): Unit = {
        val count = schemesFoundCount.getOrElse((scheme.view, scheme.regroup), 0)
        schemesFoundCount += ((scheme.view, scheme.regroup) -> {count + 1})
        dbgs(s"incrementing ${scheme.view}, ${scheme.regroup} to ${count + 1} ")
      }
      def wantScheme(scheme: BankingOptions): Boolean = {
        if (effort == 0 && (schemesFoundCount.map(_._2).sum > 0)) false
        else if (effort == 1 && (
          schemesFoundCount.filter{x => x._1._1 == scheme.view && x._1._2 == scheme.regroup}.values.sum > 0 ||
          !(scheme.regroup.dims.isEmpty || scheme.regroup.dims.size == scheme.view.rank)
        )) false
        else if (effort == 2 && (
          schemesFoundCount.filter{x => x._1._1 == scheme.view && x._1._2 == scheme.regroup}.values.sum > 1
        )) false
        else true
      }

      val myGrps = myReads ++ myWrites
      myGrps.foreach{x => x.foreach{y => lowRankMapping += (y -> Set(y))}}
      if (mem.isSingleton) {
        if (myWrites.exists(_.size > 1) && !mem.shouldIgnoreConflicts) error(ctx, s"Cannot bank ${mem.ctx} (${mem.name.getOrElse("")})")
        Map(attemptDirectives.head -> Map((myReads.map{x => x.flatMap(reverseAM).toSet} ++ Set(hostReads)) -> Seq(Seq(ModBanking.Unit(rank, Seq.tabulate(mem.stagedDims.size){i => i})))))
      }
      else if (myGrps.forall(_.lengthLessThan(2)) && !mem.isLineBuffer && mem.explicitBanking.isEmpty) Map(attemptDirectives.head -> Map((myReads.map{ x => x.flatMap(reverseAM).toSet} ++ Set(hostReads)) -> Seq(Seq(ModBanking.Unit(rank, Seq.tabulate(mem.stagedDims.size){ i => i})))))
      else if (myGrps.forall(_.lengthLessThan(2)) && mem.isLineBuffer && mem.explicitBanking.isEmpty) {
        val autoFullBank: FullBanking = Seq(ModBanking.Simple(mem.stagedDims(0).toInt + (depth-1)*mem.stride, Seq(0), mem.stride))
        Map(attemptDirectives.head -> Map((myReads.map{x => x.flatMap(reverseAM).toSet} ++ Set(hostReads)) -> Seq(autoFullBank ++ Seq(ModBanking.Simple(1, Seq(1), 1)))))
      }
      else if (mem.explicitBanking.isDefined) {
        Map(attemptDirectives.head -> Map((myReads.map{x => x.flatMap(reverseAM).toSet} ++ Set(hostReads)) -> Seq(mem.explicitScheme)))
      }
      else {
        attemptDirectives.flatMap{case scheme@BankingOptions(view, nStricts, aStricts, regroup) =>
          if (wantScheme(scheme)) {
            dbgs(s"Finding scheme for $scheme")
            /* Example of what "rawBanking" could look like if we duplicate for dim0 and actually bank for dim1 on something that may look like:
              *   Foreach(N by 1 par 2, M by 1){ (i,j) => s(i,j) = ...}
              *   Foreach(N by 1, M by 1 par 2){ (i,j) => ... = s(i,j)}
              *   rawBanking = Seq(
              *                   Map( Set(Seq(wr0)) -> ModBanking0, Set(Seq(wr1)) -> ModBanking0 ),
              *                   Map( Set(Seq(rd0,rd1)) -> ModBanking1 )
              *                )
              *
              *   To convert to "banking," we want to take one entry from each Map and call it a new duplicate
              */
            val autoFullBank: FullBanking = if (view.complementView.nonEmpty) view.complementView.flatMap{ axis => Seq(ModBanking.Simple(mem.stagedDims(axis).toInt + (depth-1)*mem.stride, Seq(0), mem.stride))} else Seq()
            // For hierarchical, rawBanking is a Seq that has an entry per dimension.  It is a Seq of size 1 for flat banking
            var firstSearch = true // For dual ported memories that are hierarchically banked, we can only halve N for ONE of the dimensions
            val rawBanking: Seq[Map[AccessGroups, Option[PartialBankingChoices]]] = view.expand().map{axes =>
              lowRankMapping.clear()
              myReads.foreach{x => x.foreach{y => lowRankMapping += (y -> Set(y))}}
              val selWrGrps: Set[Set[SparseMatrix[Idx]]] = if (axes.size < rank) myWrites.flatMap{grp => repackageGroup(grp, axes, false)}.map(_.toSet) else myWrites.map(_.toSet)
              val selRdGrps: Set[Set[SparseMatrix[Idx]]] = if (axes.size < rank) {myReads.flatMap{grp => repackageGroup(grp, axes, true)}.map(_.toSet)} else {myReads.map(_.toSet)}
              val selGrps: Set[Set[SparseMatrix[Idx]]] = selWrGrps ++ {if (axes.forall(regroup.dims.contains)) Set() else selRdGrps}
              selGrps.zipWithIndex.foreach{case (grp,i) =>
                dbgs(s"Banking group #$i has (${grp.size} accesses)")
                grp.foreach{matrix => dbgss("    ", matrix.toString) }
              }
              // If only 1 acc left per group, Unit banking, otherwise search
              val axisBankingScheme: Option[PartialBankingChoices] = {
                if (solutionCache.contains((selGrps, nStricts, aStricts, axes))) dbgs(s"Cache hit on ${selGrps.flatten.size} accesses, $nStricts, $aStricts, axes $axes!  Good job! (scheme ${solutionCache.get((selGrps, nStricts, aStricts, axes))})")
                solutionCache.getOrElseUpdate((selGrps, nStricts, aStricts, axes), {
                  if (selGrps.forall(_.toSeq.lengthLessThan(2)) && view.isInstanceOf[Hierarchical]) Some(Seq(ModBanking.Unit(1, axes)))
                  else if (selGrps.forall(_.toSeq.lengthLessThan(2)) && view.isInstanceOf[Flat]) Some(Seq(ModBanking.Unit(rank, axes)))
                  else {
                    val x = findBanking(selGrps, nStricts, aStricts, axes, mem.stagedDims.map(_.toInt), mem, firstSearch)
                    firstSearch = false
                    x
                  }
                })}
              if (axes.forall(regroup.dims.contains)) selRdGrps.flatMap{x => x.map{a => Set(reverseAM(a)) -> axisBankingScheme }}.toMap else Map(selRdGrps.map{ x => x.flatMap(reverseAM(_)) ++ hostReads} -> axisBankingScheme)
            }
            if (rawBanking.forall{m => m.toSeq.map(_._2).forall{b => b.isDefined}}) {
              val bankingIds: List[List[Int]] = combs(rawBanking.toList.map{b => List.tabulate(b.size){i => i}})
              // Banking is a mapping from access groups to a Seq of possible banking schemes, must repackage rawBanking to produce it.
              val bankingRepackaged: Map[AccessGroups, FullBankingChoices] = bankingIds
                .map{addr: List[Int] => addr.zipWithIndex.map{case (i,j) => rawBanking(j).toList(i)}}
                .map{dup =>
                  // When repackaging rawBanking, make sure to only keep read groups whose accesses can be found in read groups of ALL other dimensions
                  val accs: Seq[AccessGroups] = dup.map(_._1.filter(_.nonEmpty))
                  val inViewAccs: AccessGroups = accs.zipWithIndex.map{ case (dimGrp:AccessGroups,i:Int) =>
                    val others: Seq[AccessGroups] = accs.patch(i, Nil, 1)
                    dimGrp.map{ grp:SingleAccessGroup => if (others.isEmpty) grp else others.map{allDimGrps => allDimGrps.flatten}.reduce(_.intersect(_)).intersect(grp)}//.map{otherDimGrp => otherDimGrp.intersect(grp)}}} //if grp.forall{ac => others.forall{dg => dg.flatten.contains(ac)}} => grp}
                  }.reduce{_++_}.filter(_.nonEmpty)
                  inViewAccs -> dup.map(_._2.get).distinct
                }.toMap
              // All-to-all combinations for hierarchical
              val banking = bankingRepackaged.map { case (grps, schms) => grps -> combs(autoFullBank.map(List(_)).toList ++ schms.map(_.toList).toList) }
              val dimsInStrategy = view.expand().flatten.distinct
              val validBanking: Map[AccessGroups, FullBankingChoices] = banking.flatMap { case (accs, fullOpts) =>
                val prunedGrps = (accs.map(_.map(_.matrix)) ++ myWrites).map { grp => grp.map { mat => mat.sliceDims(dimsInStrategy) }.toSeq.distinct }
                // TODO: Should we actually reject based on group sizes at this point?  Presumably we only landed on ones that are valid
                //       prunedGrps also doesn't maintain projection filtering the same way selGrps did up above so it rejects valid schemes now for Gibbs
                val(validOpts, rejectedOpts) = fullOpts.distinct.partition { case opt => true } //isValidBanking(opt, prunedGrps) }
                validOpts.foreach{opt =>
                  dbgs(s"Banking scheme $opt accepted!")
                  markFound(scheme)
                }
                rejectedOpts.foreach { opt =>
                  dbgs(s"Banking scheme $opt rejected because it was deemed invalid!")
                }
                if (validOpts.nonEmpty) { Some(accs -> validOpts) }
                else { None }
              }
              if (validBanking.nonEmpty) { Some(scheme -> validBanking) }
              else None
            } else {
              dbgs(s"Could not find valid solution for $scheme!")
              None
            }
          } else {
            dbgs(s"Because $effort effort level, skipping search for scheme $scheme")
            None
          }
        }.toMap
      }
    }

    accMatrixMapping.clear()
    sparseMatrixMapping.clear()

    // Step 1: Modify access matrices due to lockstep dephasing and compute new "actual" grps
    val readIterSubsts = generateSubstRules(reads)
    if (readIterSubsts.nonEmpty) dbgs(s"General read dephasing rules for $mem: ${readIterSubsts.mkString("\n  - ")}")
    val writeIterSubsts = generateSubstRules(writes)
    if (writeIterSubsts.nonEmpty) dbgs(s"General write dephasing rules for $mem: ${writeIterSubsts.mkString("\n  - ")}")
    val hostReads: mutable.Set[AccessMatrix] = scala.collection.mutable.Set[AccessMatrix]()
    val newReads = rewriteAccesses(reads, readIterSubsts).map{accs => 
      val mats = accs.toSeq.flatMap{x =>
                                      sparseMatrixMapping += (x.matrix -> {sparseMatrixMapping.getOrElse(x.matrix, Set()) ++ Set(x)})
                                      if (x.parent != Ctrl.Host) Some(x.matrix)
                                      else {
                                        hostReads += x
                                        None
                                      }
                                    }
      if (mem.isSingleton) mats else mats.distinct
    }
    val newWrites = rewriteAccesses(writes, writeIterSubsts).map{accs => 
      val mats = accs.toSeq.flatMap{x => 
                                      // sparseMatrixMapping += (x.matrix -> {sparseMatrixMapping.getOrElse(x.matrix, Set()) ++ Set(x)})
                                      if (x.parent != Ctrl.Host) Some(x.matrix)
                                      else None
                                    }
      if (mem.isSingleton) mats else mats.distinct
    }

    // Step 2: Find schemes for these grps
    findSchemes(newReads, newWrites, hostReads.toSet.asInstanceOf[SingleAccessGroup])
  }

  /** True if this is a valid banking strategy for the given sets of access matrices. */
  def isValidBanking(banking: FullBanking, grps: Set[Seq[SparseMatrix[Idx]]]): Boolean = {
    // TODO[2]: This may not be correct in all cases, need to verify!
    val banks = banking.map(_.nBanks).product
    grps.forall{a => a.toList.lengthLessThan(banks+1)}
  }

  protected def findBanking(grps: Set[Set[SparseMatrix[Idx]]], nStricts: NStrictness, aStricts: AlphaStrictness, axes: Seq[Int], stagedDims: Seq[Int], mem: Sym[_], firstSearch: Boolean): Option[PartialBankingChoices] = {
    val filteredStagedDims = axes.map(mem.stagedDims.map(_.toInt))
    val Nmin_base: Int = grps.map(_.size).maxOrElse(1)
    val dualPortHalve = mem.isDualPortedRead && firstSearch
    val Nmin =  if (dualPortHalve) scala.math.ceil(Nmin_base.toFloat / 2.0).toInt else Nmin_base
    val Ncap = filteredStagedDims.product max Nmin
    val possibleNs = nStricts.expand(Nmin, Ncap, filteredStagedDims.toList, grps.map(_.size).toList, axes)
//    dbgs(s"possible Ns: ${nStricts.expand(Nmin, Ncap, filteredStagedDims.toList, grps.map(_.size).toList, axes)}")
    val Ns = possibleNs.iterator
    val numChecks = grps.map{x => nCr(x.size, 2)}.sum
    val rank = axes.length
    var banking: Option[PartialBankingChoices] = None

    var attempts = 0

    // Experiment hack
    var validSchemesFound = 0
    val searchTimeout = spatialConfig.bankingTimeout
    val validSchemesWanted = spatialConfig.numSchemesPerRegion
    while(Ns.hasNext && validSchemesFound < validSchemesWanted && attempts < searchTimeout) {
      val N = Ns.next()
      val As = aStricts.expand(rank, N, stagedDims, axes)
      val numAs = aStricts.expand(rank, N, stagedDims, axes).toList.size
//      dbgs(s"possible as ${aStricts.expand(rank, N, stagedDims, axes).toList}")
      if (mem.forceExplicitBanking) {
        val alpha = As.next()
        val allP = computeP(N,1,alpha,stagedDims,bug(s"Could not fence off a region for banking scheme N=$N, B=1, alpha=$alpha (memory $mem ${mem.ctx})"))
        val forEachP = allP.map{P => ModBanking(N, axes.map(mem.explicitBs).head,alpha,axes,P)}
        // TODO: Extraction of B here may be wrong, but people should really be careful if they explicitly set B > 1
          banking = Some(banking.getOrElse(Seq()) ++ forEachP)
      }
      dbgs(s" Solution space: $numAs * ${possibleNs.size} * ${mem.blockCyclicBs.size} (B), check complexity: $numChecks")
      while (As.hasNext && validSchemesFound < validSchemesWanted && attempts < searchTimeout) {
        val alpha = As.next()
        if (attempts < 50) dbgs(s"     Checking N=$N and alpha=$alpha")
        attempts = attempts + 1
        if (!mem.onlyBlockCyclic && (mem.explicitBanking.isEmpty || (mem.explicitBanking.isDefined && mem.explicitBs(axes.head) == 1)) && checkCyclic(mem,N,alpha,grps,dualPortHalve)) {
          dbgs(s"     Success on N=$N, alpha=$alpha, B=1")
          val allP = computeP(N,1,alpha,stagedDims,bug(s"Could not fence off a region for banking scheme N=$N, B=1, alpha=$alpha (memory $mem ${mem.ctx})"))
          val forEachP = allP.map{P => ModBanking(N,1,alpha,axes,P,numAs*possibleNs.size,numChecks)}
          banking = Some(banking.getOrElse(Seq[ModBanking]()) ++ forEachP)
          validSchemesFound = validSchemesFound + 1
        }
        else if (!mem.noBlockCyclic) {
          val B = if (mem.explicitBanking.isDefined) Some(mem.explicitBs(axes.head)) else mem.blockCyclicBs.find{b => checkBlockCyclic(mem,N,b,alpha,grps,dualPortHalve) }
          val numBs = B.size
          val scheme: Option[Seq[ModBanking]] = B.collectFirst{case b if coprime(Seq(b) ++ alpha) =>
            dbgs(s"     Success on N=$N, alpha=$alpha, B=$b")
            val allP = computeP(N, b, alpha, stagedDims,bug(s"Could not fence off a region for banking scheme N=$N, B=$b, alpha=$alpha (memory $mem ${mem.ctx})"))
            val forEachP = allP.map{P => ModBanking(N, b, alpha, axes, P,numAs*possibleNs.size*numBs,numChecks)}
            validSchemesFound = validSchemesFound + 1
            forEachP
          }
          if (scheme.isDefined) banking = Some(banking.getOrElse(Seq[ModBanking]()) ++ scheme.get)
        }
      }
    }
    if (attempts >= searchTimeout - 2) dbgs(s"Timeout while searching!")
    dbgs(s"       $mem: Found $validSchemesFound solutions after ${attempts * numChecks} (= $attempts * $numChecks) attempts to find solution for $nStricts $aStricts $axes")
    banking
  }

  implicit class SeqMath(a: Seq[Int]) {
    def *(b: SparseMatrix[Idx]): SparseVector[Idx] = {
      val vec = b.keys.mapping{k => b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i(k)*a_i }.sum }
      val c = b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i.c*a_i}.sum
      SparseVector[Idx](vec,c,Map.empty)
    }
  }

  private def checkCyclic(mem: Sym[_], N: Int, alpha: Seq[Int], grps: Set[Set[SparseMatrix[Idx]]], dualPortHalve: Boolean): Boolean = {
    grps.forall{grp =>
      val dp = grp.nonEmpty && grp.head.isReader && dualPortHalve // TODO: mem.isDualPortedWrite (requires simple changes here, complicated changes in chisel backend)
      if (dp) {
        grp.forallTriplets { (a0, a1, a2) =>
          val c01 = ((alpha * (a0 - a1) + (k, N)) === 0).andDomain.isEmpty
          val c02 = ((alpha * (a0 - a2) + (k, N)) === 0).andDomain.isEmpty
//          val c12 = ((alpha * (a1 - a2) + (k, N)) === 0).andDomain.isEmpty // Apparently c12 is unnecessary
          c01 || c02 //|| c12
        }
      } else {
        grp.forallPairs{(a0,a1) =>
          val c0 = (alpha*(a0 - a1) + (k,N)) === 0
          c0.andDomain.isEmpty
        }
      }
    }
  }

  private def checkBlockCyclic(mem: Sym[_], N: Int, B: Int, alpha: Seq[Int], grps: Set[Set[SparseMatrix[Idx]]], dualPortHalve: Boolean): Boolean = {
    def checkPair(alphaA0: SparseVector[Idx], alphaA1: SparseVector[Idx]): Boolean = {
      val c0 = (-alphaA0 + (k0, B * N) + (k1, B) + B - 1) >== 0
      val c1 = (-alphaA1 + (k1, B) + B - 1) >== 0
      val c2 = (alphaA0 - (k0, B * N) - (k1, B)) >== 0
      val c3 = (alphaA1 - (k1, B)) >== 0
      ConstraintMatrix(Set(c0, c1, c2, c3)).andDomain.isEmpty
    }
    grps.forall{grp =>
      val dp = grp.nonEmpty && grp.head.isReader && dualPortHalve // TODO: mem.isDualPortedWrite (requires simple changes here, complicated changes in chisel backend)
      if (dp) {
        grp.forallTriplets { (a0, a1, a2) =>
          val alphaA0 = alpha * a0
          val alphaA1 = alpha * a1
          val alphaA2 = alpha * a2
          checkPair(alpha * a0, alpha * a1) || checkPair(alpha * a0, alpha * a2) // || checkPair(alpha * a1, alpha * a2)
        }
      } else {
        grp.forallPairs { (a0, a1) =>
          checkPair(alpha * a0, alpha * a1)
        }
      }
    }
  }
}

