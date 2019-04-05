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
  private val Bs = Seq(2, 4, 8, 16, 32, 64, 128, 256)

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
    dimGrps: Seq[Seq[Seq[Int]]]
  ): Map[Set[Set[AccessMatrix]], Seq[Seq[Banking]]] = {

    // Modify access matrices due to lockstep dephasing
    val readIterSubsts: scala.collection.immutable.Map[(Idx,Seq[Int]),Idx] = reads.map{grp => grp.map{a => 
      grp.filter(_ != a).map{b => dephasingIters(a,b,mem)}.flatten
    }.flatten}.flatten.collect{case(x,addr) if (addr.exists(_>0)) => ((x,addr) -> boundVar[I32])}.toMap
    if (readIterSubsts.nonEmpty) dbgs(s"General dephasng rules for $mem: ${readIterSubsts}")
    val writeIterSubsts: scala.collection.immutable.Map[(Idx,Seq[Int]),Idx] = writes.map{grp => grp.map{a => 
      grp.filter(_ != a).map{b => dephasingIters(a,b,mem)}.flatten
    }.flatten}.flatten.collect{case(x,addr) if (addr.exists(_>0)) => ((x,addr) -> boundVar[I32])}.toMap
    if (writeIterSubsts.nonEmpty) dbgs(s"General dephasng rules for $mem: ${writeIterSubsts}")
    val newReads = reads.map{grp => grp.map{a => 
      val keyRules: scala.collection.immutable.Map[Idx,Idx] = accessIterators(a.access, mem)
            .zipWithIndex.collect{case(iter,i) if (readIterSubsts.contains((iter,a.unroll.take(i)))) => (iter -> readIterSubsts((iter,a.unroll.take(i))))}.toMap
      if (keyRules.nonEmpty) {mem.addDephasedAccess(a.access); dbgs(s"Substituting due to dephasing: $keyRules")}
      a.randomizeKeys(keyRules)
    }.toSet}.toSet
    val newWrites = writes.map{grp => grp.map{a => 
      val keyRules: scala.collection.immutable.Map[Idx,Idx] = accessIterators(a.access, mem)
            .zipWithIndex.collect{case(iter,i) if (writeIterSubsts.contains((iter,a.unroll.take(i)))) => (iter -> writeIterSubsts((iter,a.unroll.take(i))))}.toMap
      if (keyRules.nonEmpty) {mem.addDephasedAccess(a.access); dbgs(s"Substituting due to dephasing: $keyRules")}
      a.randomizeKeys(keyRules)
    }.toSet}.toSet

    val grps = (newReads ++ newWrites).map(_.toSeq.filter(_.parent != Ctrl.Host).map(_.matrix).distinct)

    val fullStrategy = Seq.tabulate(rank){i => i}

    def findSchemes(myGrps: Set[Seq[SparseMatrix[Idx]]]): Seq[Seq[Banking]] = {
      if (myGrps.forall(_.lengthLessThan(2)) && !mem.isLineBuffer) Seq(Seq(ModBanking.Unit(rank)))
      else if (myGrps.forall(_.lengthLessThan(2)) && mem.isLineBuffer) Seq(Seq(ModBanking.Simple(1, Seq(1), 1)))
      else {
        dimGrps.flatMap{ strategy: Seq[Seq[Int]] => 
          dbgs(s"Working on strategy $strategy")
          val banking = strategy.map{dims =>
            dbgs(s"  Working on dims $dims")
            val selGrps = myGrps.flatMap{grp => 

              val grpViews = grp.map{mat => AccessView(dims, fullStrategy, mat)} 
              
              /**  Move accesses to another group if their "complement" guarantees they will never interfere **/
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
                          dbgs(s"Found nonconflicting complementary dimension: $dim")
                        }
                        conflictingRow
                      }
                      conflictingMatrix
                    }
                    if (canConflict) {
                      dbgs(s"Placing in group $grpId")
                      regrp(grpId) = regrp(grpId) ++ ArrayBuffer(current)
                      placed = true
                    }
                    else if (grpId < regrp.size - 1) {
                      dbgs(s"Cannot place in group $grpId because it has no conflicts in dim $dims")
                      grpId += 1
                    } else {
                      dbgs(s"Making new group")
                      regrp += ArrayBuffer(current)
                      placed = true
                    }
                  }
                }
              }
              dbgs(s"regrouped $grpViews\n\n-->\n\n$regrp")

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
            val stagedDims = dims.map(mem.stagedDims.map(_.toInt))
            selGrps.zipWithIndex.foreach{case (grp,i) =>
              dbgs(s"Banking accesses:")
              dbgs(s"  Group #$i:")
              grp.foreach{matrix => dbgss("    ", matrix.toString) }
            }
            // If only 1 acc left per group, Unit banking, otherwise search
            if (selGrps.forall(_.lengthLessThan(2))) ModBanking.Unit(1) else findBanking(selGrps, dims, stagedDims)
          }
          val dimsInStrategy = strategy.flatten.distinct
          val prunedGrps = myGrps.map{grp => grp.map{mat => mat.sliceDims(dimsInStrategy)}.distinct}
          if (isValidBanking(banking,prunedGrps)) {
            Some(banking)
          }
          else None
        }
      }
    }

    val unifiedScheme = findSchemes(grps)
      
    if (unifiedScheme.nonEmpty) Map(reads -> unifiedScheme)
    else {
      // Regroup based on lockstepiness
      val regroupedReads: Seq[Set[Set[AccessMatrix]]] = reads.map{grp => grp.map{a => 
          val keyRules: scala.collection.immutable.Map[Idx,Idx] = accessIterators(a.access, mem)
              .zipWithIndex.collect{case(iter,i) if (readIterSubsts.contains((iter,a.unroll.take(i)))) => (iter -> readIterSubsts((iter,a.unroll.take(i))))}.toMap
          (a,keyRules)
        }.groupBy(_._2).map{case(rules,grp) => Set(grp.map(_._1.randomizeKeys(rules)).toSet)}.toSeq}.toSeq.flatten
      dbgs(s"Attempting to regroup reads and bank by duplication with the following groups:")
      regroupedReads.zipWithIndex.foreach{case(grp,i) => dbgs(s"New group #$i"); grp.flatten.foreach{a => dbgs(s"  $a")}}
      val duplicationSchemes = regroupedReads.map{grp => 
        val grps = (grp ++ newWrites).map(_.toSeq.filter(_.parent != Ctrl.Host).map(_.matrix).distinct)
        val bankings = findSchemes(grps)
        (grp -> bankings)
      }.toMap
      dbgs(s"Found schemes $duplicationSchemes")
      duplicationSchemes
    }
  } 

  /** True if this is a valid banking strategy for the given sets of access matrices. */
  def isValidBanking(banking: Seq[ModBanking], grps: Set[Seq[SparseMatrix[Idx]]]): Boolean = {
    // TODO[2]: This may not be correct in all cases, need to verify!
    val banks = banking.map(_.nBanks).product
    grps.forall{a => a.lengthLessThan(banks+1)}
  }

  /**
    * Generates an iterator over all vectors of given rank, with values up to N
    * Prioritizes vectors which are entirely powers of 2 first
    * Next prioritizes vectors composed of either 
    *    1) Number that evenly divides N
    *    2) Number that is the result of a product of some combination of stagedDims (Not sure if this increases the likelihood of valid schemes compared to option 1 only)
    *    3) Number that is also power of 2
    *    (consider something like 96x3x3x96 sram N = 36, want to try things like alpha = 18,1,3,9)
    * Note that the total size is O(N**rank)
    */
  def Alphas(rank: Int, N: Int, stagedDims: Seq[Int]): (Iterator[Seq[Int]], Iterator[Seq[Int]]) = {
    // Prime factors of number, for shortcircuiting the brute force alphas
    def factorize(number: Int, list: List[Int] = List()): List[Int] = {
      for(n <- 2 to number if number % n == 0) {
        return factorize(number / n, list :+ n)
      }
      list
    }

    val a2 = (0 to 2*N).filter(x => isPow2(x) || x == 1 || x == 0).uniqueModN(N)
    val alikely = (
                Seq(0,1) ++ 
                Seq.tabulate(factorize(N).length){i => factorize(N).combinations(i+1).toList}.flatten.map(_.product) ++ 
                Seq.tabulate(stagedDims.length){i => stagedDims.combinations(i+1).toList}.flatten.map(_.product).filter(_ <= N) ++ 
                (0 to 2*N).filter(isPow2)
               ).uniqueModN(N)
    def Alphas2(dim: Int, prev: Seq[Int]): Iterator[Seq[Int]] = {
      if (dim < rank) {
        a2.iterator.flatMap{aD => Alphas2(dim+1, prev :+ aD) }
      }
      else a2.iterator.map{aR => prev :+ aR }
    }
    def AlphasLikely(dim: Int, prev: Seq[Int]): Iterator[Seq[Int]] = {
      if (dim < rank) {
        alikely.iterator.flatMap{aD => AlphasLikely(dim+1, prev :+ aD) }
      }
      else alikely.iterator.map{aR => prev :+ aR}
    }
    def AlphasX(dim: Int, prev: Seq[Int]): Iterator[Seq[Int]] = {
      if (dim < rank) {
        (0 to 2*N).uniqueModN(N).iterator.flatMap{aD => AlphasX(dim+1, prev :+ aD) }
      }
      else (0 to 2*N).uniqueModN(N).iterator.map{aR => prev :+ aR }.filterNot(_.forall(x => isPow2(x) || x == 1))
    }
    val pow2As   = Alphas2(1, Nil).filterNot(_.forall(_ == 0))
    val likelyAs = AlphasLikely(1, Nil).filterNot{x => x.forall(_ == 0) || x.forall(isPow2)}
    val xAs      = AlphasX(1, Nil).filterNot(_.forall(_ == 0))
    ((pow2As ++ likelyAs), xAs)
  }

  private def computeP(n: Int, b: Int, alpha: Seq[Int], stagedDims: Seq[Int]): Seq[Int] = {
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
           P_i = NB / gcd(NB,alpha_i)
               ** if alpha_i = 0, then P_i = infinity **

           ex-     alpha = 3,4    N = 6     B = 1
                        _____
               banks:  |0 4 2|0 4 2 0 4 2
                       |3_1_5|3 1 5 3 1 5
                        0 4 2 0 4 2 0 4 2
                        3 1 5 3 1 5 3 1 5
               banking pattern: 0 4 2
                                3 1 5
               P_raw = 2,3
       2. Create P_expanded: List[List[Int]], where P_i is a list containing P_raw, all divisors of P_raw, and dim_i
       2. Find list, selecting one element from each list in P, whose product == N*B and whose ranges, (0 until p*a by a), touches each bank exactly once, with 
          preference given to smallest volume after padding, and this will fence off a region that contains each bank
          exactly once.
       3. Pad the memory so that P evenly divides its respective dim (Currently stored as .padding metadata)
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
    def allLoops(maxes: Seq[Int], a: Seq[Int], B: Int, iterators: Seq[Int]): Seq[Int] = maxes match {
      case Nil => Nil
      case h::tail if tail.nonEmpty => (0 to h-1).flatMap{i => allLoops(tail, a.tail, B, iterators ++ Seq(i*a.head/B))}
      case h::tail if tail.isEmpty => (0 to h-1).map{i => i*a.head/B + iterators.sum}
    }
    def spansAllBanks(p: Seq[Int], a: Seq[Int], N: Int, B: Int, allPossible: Seq[Int]): Boolean = {
      val banksInFence = allLoops(p,a,B,Nil).map(_%N)
      allPossible.forall{b => banksInFence.count(_==b) == B}
    }
    def gcd(a: Int,b: Int): Int = if(b ==0) a else gcd(b, a%b)
    def divisors(x: Int): Seq[Int] = (1 to x).collect{case i if x % i == 0 => i}
    try {
      val P_raw = alpha.indices.map{i => if (alpha(i) == 0) 1 else n*b/gcd(n*b,alpha(i))}
      val allBanksAccessible = allLoops(P_raw.toList, alpha.toList, b, Nil).map(_%n).sorted.distinct
      val P_expanded = Seq.tabulate(alpha.size){i => divisors(P_raw(i)) ++ {if (P_raw(i) != 1) List(stagedDims(i)) else List()}}
      val options = combs(P_expanded.map(_.toList).toList).filter(_.product == allBanksAccessible.length * b).collect{case p if spansAllBanks(p,alpha,n,b,allBanksAccessible) => p}
      val PandCost = options.map{option => 
        val padding = stagedDims.zip(option).map{case(d,p) => (p - d%p) % p}
        val volume = stagedDims.zip(padding).map{case(x,y)=>x+y}.product
        (option,volume)
      }
      PandCost.minBy(_._2)._1
    }
    catch { case t:Throwable =>
      bug(s"Could not fence off a region for banking scheme N=$n, B=$b, alpha=$alpha")
      throw t
    }
  }

  protected def findBanking(grps: Set[Seq[SparseMatrix[Idx]]], dims: Seq[Int], stagedDims: Seq[Int]): ModBanking = {
    val rank = dims.length
    val Nmin: Int = grps.map(_.size).maxOrElse(1)
    val Ncap = stagedDims.product max Nmin
    val (n2,nx) = (Nmin to 8*Nmin).filter(_ <= Ncap).partition{i => isPow2(i) }
    val n2Head = if ((n2.isEmpty && nx.isEmpty) || (n2.nonEmpty && n2.head.toDouble/Nmin > MAGIC_CUTOFF_N)) Seq(Nmin) else Nil

    var banking: Option[ModBanking] = None

    /** For high-dimensional memories, there could be a huge amount of elements in As, so 
        prioritize searching through all possible Ns before all possible As.
        Attempt the following to find banking the fastest:
      *   For all Cheap Ns, For all Cheap or Likely As
      *   For all Other Ns, For all Cheap or Likely As
      *   For all Cheap Ns, For all Other As
      *   For all Other Ns, For all Other As
      */
    val Ns_1 = (n2Head ++ grps.map(_.size) ++ n2).sorted.iterator
    val Ns_2 = (nx).iterator
    val Ns_3 = (n2Head ++ grps.map(_.size) ++ n2).sorted.iterator
    val Ns_4 = (nx).iterator

    def exhaustIterators(Ns: Iterator[Int], cheapAs: Boolean): Unit = {
      var attempts = 0
      while(Ns.hasNext && banking.isEmpty) {
        val N = Ns.next()
        val allAs = Alphas(rank, N, stagedDims)
        val As = if (cheapAs) allAs._1 else allAs._2
        while (As.hasNext && banking.isEmpty) {
          val alpha = As.next()
          if (attempts < 50) dbgs(s"     Checking N=$N and alpha=$alpha")
          attempts = attempts + 1
          if (checkCyclic(N,alpha,grps)) {
            dbgs(s"     Success on N=$N, alpha=$alpha, B=1")
            val P = computeP(N,1,alpha,stagedDims)
            banking = Some(ModBanking(N,1,alpha,dims,P))
          }
          else {
            val B = Bs.find{b => checkBlockCyclic(N,b,alpha,grps) }
            banking = B.map{b =>
              dbgs(s"     Success on N=$N, alpha=$alpha, B=$b")
              val P = computeP(N, b, alpha, stagedDims)
              ModBanking(N, b, alpha, dims, P) 
            }
          }
        }
      }
    }

    exhaustIterators(Ns_1, true)
    exhaustIterators(Ns_2, true)
    exhaustIterators(Ns_3, false)
    exhaustIterators(Ns_4, false)

    banking.getOrElse(ModBanking.Unit(rank))
  }

  implicit class SeqMath(a: Seq[Int]) {
    def *(b: SparseMatrix[Idx]): SparseVector[Idx] = {
      val vec = b.keys.mapping{k => b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i(k)*a_i }.sum }
      val c = b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i.c*a_i}.sum
      SparseVector[Idx](vec,c,Map.empty)
    }
  }

  private def checkCyclic(N: Int, alpha: Seq[Int], grps: Set[Seq[SparseMatrix[Idx]]]): Boolean = grps.forall{_.forallPairs{(a0,a1) =>
    val c0 = (alpha*(a0 - a1) + (k,N)) === 0
    c0.andDomain.isEmpty
  }}

  private def checkBlockCyclic(N: Int, B: Int, alpha: Seq[Int], grps: Set[Seq[SparseMatrix[Idx]]]): Boolean = grps.forall{_.forallPairs{(a0,a1) =>
    val alphaA0 = alpha*a0
    val alphaA1 = alpha*a1
    val c0 = (-alphaA0 + (k0,B*N) + (k1,B) + B - 1) >== 0
    val c1 = (-alphaA1 + (k1,B) + B - 1) >== 0
    val c2 = (alphaA0 - (k0,B*N) - (k1,B)) >== 0
    val c3 = (alphaA1 - (k1,B)) >== 0
    ConstraintMatrix(Set(c0,c1,c2,c3)).andDomain.isEmpty
  }}
}
