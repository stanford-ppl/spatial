package spatial.traversal.banking

import argon._
import utils.implicits.collections._
import utils.math._
import poly.{ConstraintMatrix, ISL, SparseMatrix, SparseVector}

import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._

import spatial.metadata.types._
import spatial.util.IntLike._
import scala.collection.mutable.ArrayBuffer


case class AccessView(val activeDims: Seq[Int], val fullDims: Seq[Int], val access: SparseMatrix[Idx]){
  def activeAccess: SparseMatrix[Idx] = access.sliceDims(activeDims)
  def complementAccess: SparseMatrix[Idx] = access.sliceDims(fullDims.diff(activeDims))
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
  private val sparseMatrixMapping = scala.collection.mutable.HashMap[SparseMatrix[Idx], Set[AccessMatrix]]()
  // Mapping te keep track of which read SparseMatrix slices rewritten as low rank corresponds to which original full-rank read SparseMatrix
  private val lowRankMapping = scala.collection.mutable.HashMap[SparseMatrix[Idx], Set[SparseMatrix[Idx]]]()
  // Helper for replacing sparse matrix with its original access matrix
  private def reverseAM(a: SparseMatrix[Idx]): Set[AccessMatrix] = lowRankMapping(a).map(sparseMatrixMapping).flatten.map(accMatrixMapping)
  // Cache for skipping ahead to correct banking solution for patterns/axes that have already been solved
  private val solutionCache = scala.collection.mutable.HashMap[(Set[Set[SparseMatrix[Idx]]], NStrictness, AlphaStrictness, Seq[Int]), Option[ModBanking]]()
  // Map for tracking which kinds of schemes already have a solution, used depending on what the banking effort is set to
  private val schemesFoundCount = scala.collection.mutable.HashMap[(BankingView,RegroupDims), Int]()
  /** Returns a Map from Seq(banking schemes) to the readers for these schemes.  
    * Generally, it will contain Map(Seq(flat_scheme, nested_scheme) -> all readers) but in 
    * the case of dephased accesses that cannot be banked together, there will be multiple 
    * entries in the map who each point to a different partition of readers
    */
  override def bankAccesses(
    mem:    Sym[_],
    rank:   Int,
    reads:  Set[Set[AccessMatrix]],
    writes: Set[Set[AccessMatrix]],
    attemptDirectives: Seq[BankingOptions],
    depth: Int
  ): Map[BankingOptions, Map[Set[Set[AccessMatrix]], Seq[Banking]]] = {

    // Generate substitution rules for each iter for each uid.  Given iter and its uid, generate a rule to replace it with a new iter and an offset to include in the access patterns c column
    def generateSubstRules(accs: Set[Set[AccessMatrix]]): scala.collection.immutable.Map[(Idx,Seq[Int]),(Idx,Int)] = {
      val toRewrite: Map[(Idx, Seq[Int]), Option[Int]] = if (mem.forceExplicitBanking) Map() else accs.flatten.map{a => dephasingIters(a,Seq.fill(a.unroll.size)(0),mem)}.flatten.toMap
      toRewrite.map{
        case((i,addr),ofs) if (ofs.isDefined) => ((i,addr) -> (i,ofs.get))
        case((i,addr),ofs) if (!ofs.isDefined) => ((i,addr) -> (boundVar[I32],0))
      }.toMap
    }
    def rewriteAccesses(accs: Set[Set[AccessMatrix]], rules: Map[(Idx,Seq[Int]),(Idx,Int)]): Set[Set[AccessMatrix]] = {
      accs.map{grp => grp.map{a => 
        val aIters = accessIterators(a.access, mem)
        val keyRules: scala.collection.immutable.Map[Idx,(Idx,Int)] = aIters.zipWithIndex.collect{case(iter,i) if (rules.contains((iter,getDephasedUID(aIters,a.unroll,i)))) => (iter -> rules((iter,getDephasedUID(aIters,a.unroll,i))))}.toMap
        if (keyRules.nonEmpty) {mem.addDephasedAccess(a.access); dbgs(s"Substituting due to dephasing: $keyRules")}
        val newa = a.substituteKeys(keyRules)
        accMatrixMapping += (newa -> a)
        newa
      }.toSet}.toSet
    }
    def repackageGroup(grp: Seq[SparseMatrix[Idx]], dims: List[Int], isRd: Boolean): ArrayBuffer[Seq[SparseMatrix[Idx]]] = {
      val fullStrategy = Seq.tabulate(rank){i => i}
      // For hierarchical views, regroup accesses based on whether their "complements" are non-interfering
      val grpViews = grp.map{mat => 
        val t = AccessView(dims, fullStrategy, mat)
        if (isRd) lowRankMapping += (t.activeAccess -> {lowRankMapping.getOrElse(t.activeAccess, Set()) ++ Set(mat)}) 
        t
      } 
      val regrp = ArrayBuffer[ArrayBuffer[AccessView]]()
      grpViews.zipWithIndex.foreach{case (current,i) => 
        if (regrp.isEmpty) regrp += ArrayBuffer(current)
        else {
          // Find first group where current access may interfere with ANY of the complementary dimensions
          var grpId = 0
          var placed = false
          while (grpId < regrp.size & !placed) {
            val canConflict = regrp(grpId).exists{other => 
              val diff = current.complementAccess - other.complementAccess
              val conflictingMatrix = diff.rows.zipWithIndex.forall{case (row, dim) => 
                val patternForDim = (Seq(1)*SparseMatrix[Idx](Seq(row)) === 0)
                val conflictingRow = !patternForDim.andDomain.isEmpty
                // dbgs(s"Row $dim: \n  ISL problem:\n${patternForDim.andDomain}")
                if (!conflictingRow) {
                  // dbgs(s"Found nonconflicting complementary dimension: $dim")
                }
                conflictingRow
              }
              conflictingMatrix
            }
            if (canConflict) {
              // dbgs(s"Placing in group $grpId")
              regrp(grpId) = regrp(grpId) ++ ArrayBuffer(current)
              placed = true
            }
            else if (grpId < regrp.size - 1) {
              // dbgs(s"Cannot place in group $grpId because it has no conflicts in dim $dims")
              grpId += 1
            } else {
              // dbgs(s"Making new group")
              regrp += ArrayBuffer(current)
              placed = true
            }
          }
        }
      }
      // dbgs(s"regrouped $grpViews\n\n-->\n\n$regrp")

      regrp.map{newgrp => 
        var firstInstances: Set[SparseMatrix[Idx]] = Set.empty

        /** True if the sliced matrix has any of the following:
          *   - If the access is identical to another within this group
          *   - If it is the first time we are seeing this "sliced matrix" within this group
          */
        def isUniqueSliceInGroup(i: Int): Boolean = {
          // Current (sliced matrix, complement matrix) tuple
          val current = newgrp(i)

          // Others in group (Sequence of (sliced matrix, complement matrix) tuples)
          val pairsExceptCurrent = newgrp.patch(i,Nil,1)

          val totalCollision = pairsExceptCurrent.exists{other => other.access == current.access }
          val firstTime          = !firstInstances.contains(current.activeAccess)

          totalCollision || firstTime
        }

        Seq(newgrp.zipWithIndex.collect{case (current,i) if isUniqueSliceInGroup(i) =>
          firstInstances += current.activeAccess
          current.activeAccess
        }:_*)
      }
    }

    def findSchemes(myReads: Set[Seq[SparseMatrix[Idx]]], myWrites: Set[Seq[SparseMatrix[Idx]]], hostReads: Set[AccessMatrix]): Map[BankingOptions, Map[Set[Set[AccessMatrix]], Seq[Banking]]] = {
      val effort = mem.bankingEffort
      schemesFoundCount.clear()
      def markFound(scheme: BankingOptions): Unit = {
        val count = schemesFoundCount.getOrElse((scheme.view, scheme.regroup), 0)
        schemesFoundCount += (((scheme.view, scheme.regroup) -> {count + 1}))
        dbgs(s"incrementing ${scheme.view}, ${scheme.regroup} to ${count + 1} ")
      }
      def wantScheme(scheme: BankingOptions): Boolean = {
        if (effort == 0 && (schemesFoundCount.map(_._2).sum > 0)) false
        else if (effort == 1 && (
          schemesFoundCount.filter{x => x._1._1 == scheme.view && x._1._2 == scheme.regroup}.values.sum > 0 ||
          !(scheme.regroup.dims.size == 0 || scheme.regroup.dims.size == scheme.view.rank)
        )) false
        else if (effort == 2 && (
          schemesFoundCount.filter{x => x._1._1 == scheme.view && x._1._2 == scheme.regroup}.values.sum > 0
        )) false
        else true
      }

      val myGrps = myReads ++ myWrites
      myGrps.foreach{x => x.foreach{y => lowRankMapping += (y -> Set(y))}}
      if (mem.isSingleton) {
        if (myWrites.exists(_.size > 1) && !mem.shouldIgnoreConflicts) error(ctx, s"Cannot bank ${mem.ctx} (${mem.name.getOrElse("")})")
        Map(attemptDirectives.head -> Map((myReads.map{x => x.flatMap(reverseAM).toSet} ++ Set(hostReads)) -> Seq(ModBanking.Unit(rank, Seq.tabulate(mem.stagedDims.size){i => i}))))
      }
      else if (myGrps.forall(_.lengthLessThan(2)) && !mem.isLineBuffer && !mem.explicitBanking.isDefined) Map(attemptDirectives.head -> Map((myReads.map{x => x.flatMap(reverseAM).toSet} ++ Set(hostReads)) -> Seq(ModBanking.Unit(rank, Seq.tabulate(mem.stagedDims.size){i => i}))))
      else if (myGrps.forall(_.lengthLessThan(2)) && mem.isLineBuffer && !mem.explicitBanking.isDefined) {
        val autoFullBank: Seq[ModBanking] = Seq(ModBanking.Simple(mem.stagedDims(0).toInt + (depth-1)*mem.stride, Seq(0), mem.stride, 0))
        Map(attemptDirectives.head -> Map((myReads.map{x => x.flatMap(reverseAM).toSet} ++ Set(hostReads)) -> (autoFullBank ++ Seq(ModBanking.Simple(1, Seq(1), 1, 0)))))
      }
      // else if (mem.explicitBanking.isDefined) {
      //   if (mem.forceExplicitBanking) {
      //     Seq()
      //   }
      // }
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
            val autoFullBank: Seq[ModBanking] = if (view.complementView.nonEmpty) view.complementView.toSeq.flatMap{axis => Seq(ModBanking.Simple(mem.stagedDims(axis).toInt + (depth-1)*mem.stride, Seq(0), mem.stride, 0))} else Seq()
            val rawBanking: Seq[Map[Set[Set[AccessMatrix]], Option[ModBanking]]] = view.expand().map{axes => 
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
              val axisBankingScheme: Option[ModBanking] = {
                if (solutionCache.contains((selGrps, nStricts, aStricts, axes))) dbgs(s"Cache hit on ${selGrps.flatten.size} accesses, $nStricts, $aStricts, axes $axes!  Good job! (scheme ${solutionCache.get((selGrps, nStricts, aStricts, axes))})")
                solutionCache.getOrElseUpdate((selGrps, nStricts, aStricts, axes), {
                  if (selGrps.forall(_.toSeq.lengthLessThan(2)) && view.isInstanceOf[Hierarchical]) Some(ModBanking.Unit(1, axes))
                  else if (selGrps.forall(_.toSeq.lengthLessThan(2)) && view.isInstanceOf[Flat]) Some(ModBanking.Unit(rank, axes))
                  else {
                    findBanking(selGrps, nStricts, aStricts, axes, mem.stagedDims.map(_.toInt), mem)
                  }                  
                })}
              if (axes.forall(regroup.dims.contains)) selRdGrps.flatMap{x => x.map{a => (Set(reverseAM(a)) -> axisBankingScheme)}}.toMap else Map((selRdGrps.map{x => x.flatMap(reverseAM(_)).toSet ++ hostReads}) -> axisBankingScheme)
            }
            if (rawBanking.forall{m => m.toSeq.map(_._2).forall{b => b.isDefined}}) {
              val bankingIds: List[List[Int]] = combs(rawBanking.toList.map{b => List.tabulate(b.size){i => i}})
              val banking: Map[Set[Set[AccessMatrix]], Seq[ModBanking]] = bankingIds
                .map{addr => addr.zipWithIndex.map{case (i,j) => rawBanking(j).toList(i)}}
                .map{dup => 
                  // When repackaging rawBanking, make sure to only keep read groups whose accesses can be found in read groups of ALL other dimensions
                  val accs: Seq[Set[Set[AccessMatrix]]] = dup.map(_._1.filter(_.nonEmpty)).toSeq
                  val inViewAccs: Set[Set[AccessMatrix]] = accs.zipWithIndex.map{ case (dimGrp:Set[Set[AccessMatrix]],i:Int) => 
                    val others: Seq[Set[Set[AccessMatrix]]] = accs.patch(i, Nil, 1) 
                    dimGrp.map{ grp:Set[AccessMatrix] => if (others.isEmpty) grp else others.map{allDimGrps => allDimGrps.flatten}.reduce(_.intersect(_)).intersect(grp)}//.map{otherDimGrp => otherDimGrp.intersect(grp)}}} //if grp.forall{ac => others.forall{dg => dg.flatten.contains(ac)}} => grp}
                  }.reduce{_++_}.filter(_.nonEmpty)
                  (inViewAccs -> (autoFullBank ++ dup.map(_._2.get)))
                }.toMap
              val dimsInStrategy = view.expand().flatten.distinct
              if (banking.forall{case (accs, banks) => 
                          val prunedGrps = (accs.map(_.map(_.matrix)) ++ myWrites).map{grp => grp.map{mat => mat.sliceDims(dimsInStrategy)}.toSeq.distinct}  
                          isValidBanking(banks, prunedGrps)
                        }) {
                // dbgs(s"Dim-based (raw) banking $rawBanking")
                // dbgs(s"Duplicate-based (assembled) banking $banking")
                dbgs(s"Banking scheme ${banking.map(_._2)} accepted!")
                markFound(scheme)
                Some((scheme -> banking))
              }
              else {
                dbgs(s"Computed banking for $scheme is invalid!")
                None
              }
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
    val hostReads = scala.collection.mutable.Set[AccessMatrix]()
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
    findSchemes(newReads, newWrites, hostReads.toSet)
  }

  /** True if this is a valid banking strategy for the given sets of access matrices. */
  def isValidBanking(banking: Seq[ModBanking], grps: Set[Seq[SparseMatrix[Idx]]]): Boolean = {
    // TODO[2]: This may not be correct in all cases, need to verify!
    val banks = banking.map(_.nBanks).product
    grps.forall{a => a.toList.lengthLessThan(banks+1)}
  }

  private def computeP(n: Int, b: Int, alpha: Seq[Int], stagedDims: Seq[Int], mem: Sym[_]): (Seq[Int], Int) = {
    /* Offset correction not mentioned in Wang et. al., FPGA '14
       0. Equations in paper must be wrong.  Example 
           ex-     alpha = 1,2    N = 4     B = 1
                        
               banks:   0 2 0 2    ofs (bank0):   0 * 0 *
                        1 3 1 3                   * * * *
                        2 0 2 0                   * 2 * 2 
                        1 3 1 3                   * * * *

          These offsets conflict!  They went wrong by assuming NB periodicity of 1 in leading dimension

       1. Proposed correction: Add field P: Seq[Int] to ModBanking.  Divide memory into "offset chunks" by finding the 
          periodicity of the banking pattern and fencing off a portion that contains each bank exactly once
           P_i = NB / gcd(N,alpha_i)
               ** if alpha_i = 0, then P_i = infinity **


           ex1-     alpha = 3,4    N = 6     B = 1
                        _____
               banks:  |0 4 2|0 4 2 0 4 2
                       |3_1_5|3 1 5 3 1 5
                        0 4 2 0 4 2 0 4 2
                        3 1 5 3 1 5 3 1 5
               banking pattern: 0 4 2
                                3 1 5
               P_raw = 2,3
        
           ex2-     alpha = 3     N = 9     B = 4
                       _______________________
               banks: |0_0_1_2_3_3_4_5_6_6_7_8|0 0 1 2 3 3 4 5 6 6 7 8
              
               banking pattern: 0 0* 1 2 3 3* 4 5 6 6* 7 8
               P_raw = 12
               * need to know that each field contains two 0s, 3s, and 6s

       2. Create P_expanded: List[List[Int]], where P_i is a list containing P_raw, all divisors of P_raw, and dim_i
       2. Find list, selecting one element from each list in P, whose product == N*B and whose ranges, (0 until p*a by a), touches each bank exactly once (at least once for B > 1), with 
          preference given to smallest volume after padding, and this will fence off a region that contains each bank
          exactly once.
            NOTE: If B > 1, then we are just going to assume that all banks have as many elements per yard as the most populous bank in that yard (some addresses end up being untouchable,
                  but the addressing logic would be insane otherwise).  Distinguish the degenerate elements simply by taking pure address % # max degenerates (works because of magic, 
                  but may be wrong at some point)
       3. Pad the memory so that P evenly divides its respective dim (Currently stored as .padding metadata)
       --------------------------
            # Address resolution steps in metadata/memory/BankingData.scala:
       4. Compute offset chunk
          ofsdim_i = floor(x_i/P_i)
       5. Flatten these ofsdims (w_* is stagedDim_* + pad_*)
          ofschunk = ... + (ofsdim_0 * ceil(w_1 / P_1)) + ofsdim_1
       6. If B != 1, do extra math to compute index within the block
          intrablockdim_i = x_i mod B
       7. Flatten intrablockdims
          intrablockofs = ... + intrablockdim_0 * B + intrablockdim_1
       8. Combine ofschunk and intrablockofs
          ofs = ofschunk * exp(B,D) + intrablockofs

    */
    try {
      val P_raw = alpha.indices.map{i => if (alpha(i) == 0) 1 else n*b/gcd(n,alpha(i))}
      val allBanksAccessible = allLoops(P_raw.toList, alpha.toList, b, Nil).map(_%n).sorted
      val hist = allBanksAccessible.distinct.map{x => (x -> allBanksAccessible.count(_ == x))}
      val P_expanded = Seq.tabulate(alpha.size){i => divisors(P_raw(i)) ++ {if (P_raw(i) != 1 && b == 1) List(stagedDims(i)) else List()}} // Force B == 1 for stagedDim P to make life easier
      val options = combs(P_expanded.map(_.toList).toList)
            .filter{x => if (b == 1) x.product == allBanksAccessible.distinct.length else x.product >= allBanksAccessible.distinct.length} // pre-filter yards that don't have enough entries to touch each bank
            .collect{case p if spansAllBanks(p,alpha,n,b,allBanksAccessible.distinct) => p}
      val PandCostandBloat = options.map{option => 
        // Extra elements per dimension so that yards cover entire memory
        val padding = stagedDims.zip(option).map{case(d,p) => (p - d%p) % p}
        val paddedDims = stagedDims.zip(padding).map{case(x,y)=>x+y}
        // Number of inaccessible address (caused by B > 1).  TODO: Does it matter which dimension we add these to?
        val degenerate = hist.map(_._2).max
        val darkVolume = computeDarkVolume(paddedDims, option, hist.toMap)
        val volume = paddedDims.product + darkVolume
        (option,volume,darkVolume)
      }
      (PandCostandBloat.minBy(_._2)._1, PandCostandBloat.minBy(_._2)._3)
    }
    catch { case t:Throwable =>
      bug(s"Could not fence off a region for banking scheme N=$n, B=$b, alpha=$alpha (memory $mem ${mem.ctx})")
      throw t
    }
  }

  protected def findBanking(grps: Set[Set[SparseMatrix[Idx]]], nStricts: NStrictness, aStricts: AlphaStrictness, axes: Seq[Int], stagedDims: Seq[Int], mem: Sym[_]): Option[ModBanking] = {
    val filteredStagedDims = axes.map(mem.stagedDims.map(_.toInt))
    val Nmin: Int = grps.map(_.size).maxOrElse(1)
    val Ncap = filteredStagedDims.product max Nmin
    val Ns = nStricts.expand(Nmin, Ncap, filteredStagedDims.toList, grps.map(_.size).toList, axes).iterator

    val rank = axes.length
    var banking: Option[ModBanking] = None

    var attempts = 0
    while(Ns.hasNext && banking.isEmpty) {
      val N = Ns.next()
      val As = aStricts.expand(rank, N, stagedDims, axes)
      if (mem.forceExplicitBanking) {
        val alpha = As.next()
        val t = computeP(N,1,alpha,stagedDims,mem)
        val P = t._1
        val darkVolume = t._2
        // TODO: Extraction of B here may be wrong, but people should really be careful if they explicitly set B > 1
        banking = Some(ModBanking(N, axes.map(mem.explicitBs).head,alpha,axes,P,darkVolume))
      }
      while (As.hasNext && banking.isEmpty) {
        val alpha = As.next()
        if (attempts < 50) dbgs(s"     Checking N=$N and alpha=$alpha")
        attempts = attempts + 1
        if (!mem.onlyBlockCyclic && (!mem.explicitBanking.isDefined || (mem.explicitBanking.isDefined && mem.explicitBs(axes.head) == 1)) && checkCyclic(N,alpha,grps)) {
          dbgs(s"     Success on N=$N, alpha=$alpha, B=1")
          val t = computeP(N,1,alpha,stagedDims,mem)
          val P = t._1
          val darkVolume = t._2
          banking = Some(ModBanking(N,1,alpha,axes,P,darkVolume))
        }
        else if (!mem.noBlockCyclic) {
          val B = if (mem.explicitBanking.isDefined) Some(mem.explicitBs(axes.head)) else mem.blockCyclicBs.find{b => checkBlockCyclic(N,b,alpha,grps) }
          banking = B.map{b =>
            dbgs(s"     Success on N=$N, alpha=$alpha, B=$b")
            val t = computeP(N, b, alpha, stagedDims,mem)
            val P = t._1
            val darkVolume = t._2
            ModBanking(N, b, alpha, axes, P, darkVolume) 
          }
        }
      }
    }

    banking
  }

  implicit class SeqMath(a: Seq[Int]) {
    def *(b: SparseMatrix[Idx]): SparseVector[Idx] = {
      val vec = b.keys.mapping{k => b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i(k)*a_i }.sum }
      val c = b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i.c*a_i}.sum
      SparseVector[Idx](vec,c,Map.empty)
    }
  }

  private def checkCyclic(N: Int, alpha: Seq[Int], grps: Set[Set[SparseMatrix[Idx]]]): Boolean = grps.forall{_.forallPairs{(a0,a1) =>
    val c0 = (alpha*(a0 - a1) + (k,N)) === 0
    c0.andDomain.isEmpty
  }}

  private def checkBlockCyclic(N: Int, B: Int, alpha: Seq[Int], grps: Set[Set[SparseMatrix[Idx]]]): Boolean = grps.forall{_.forallPairs{(a0,a1) =>
    val alphaA0 = alpha*a0
    val alphaA1 = alpha*a1
    val c0 = (-alphaA0 + (k0,B*N) + (k1,B) + B - 1) >== 0
    val c1 = (-alphaA1 + (k1,B) + B - 1) >== 0
    val c2 = (alphaA0 - (k0,B*N) - (k1,B)) >== 0
    val c3 = (alphaA1 - (k1,B)) >== 0
    ConstraintMatrix(Set(c0,c1,c2,c3)).andDomain.isEmpty
  }}
}
