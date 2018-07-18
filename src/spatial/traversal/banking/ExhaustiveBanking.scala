package spatial.traversal.banking

import argon._
import utils.implicits.collections._
import utils.math.isPow2
import poly.{ConstraintMatrix, ISL, SparseMatrix, SparseVector}

import spatial.lang._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._

import spatial.metadata.types._
import spatial.util.IntLike._

case class ExhaustiveBanking()(implicit IR: State, isl: ISL) extends BankingStrategy {
  // TODO[4]: What should the cutoff be for starting with powers of 2 versus exact accesses?
  private val MAGIC_CUTOFF_N = 1.4
  private val maxAttempts = 1000
  private val k = boundVar[I32]
  private val k0 = boundVar[I32]
  private val k1 = boundVar[I32]
  private val Bs = Seq(2, 4, 8, 16, 32, 64, 128, 256)

  override def bankAccesses(
    mem:    Sym[_],
    rank:   Int,
    reads:  Set[Set[AccessMatrix]],
    writes: Set[Set[AccessMatrix]],
    dimGrps: Seq[Seq[Seq[Int]]]
  ): Seq[Seq[Banking]] = {

    val grps = (reads ++ writes).map(_.toSeq.filter(_.parent != Ctrl.Host).map(_.matrix).distinct)
    val fullStrategy = Seq.tabulate(rank){i => i}
    if (grps.forall(_.lengthLessThan(2))) Seq(Seq(ModBanking.Unit(rank)))
    else {
      dimGrps.flatMap{ strategy: Seq[Seq[Int]] => 
        val banking = strategy.map{dims =>
          val selGrps = grps.map{grp => 
            val sliceCompPairs = grp.map{mat => (mat.sliceDims(dims), mat.sliceDims(fullStrategy.diff(dims)))} 
            var firstInstances: Seq[SparseMatrix[Idx]] = Seq()
            Seq(sliceCompPairs.indices.collect{case i if (
                          sliceCompPairs.patch(i,Nil,1).filter(_._1 == sliceCompPairs(i)._1).isEmpty ||  // If this slice is unique
                          sliceCompPairs.patch(i,Nil,1).filter(_._1 == sliceCompPairs(i)._1).exists(_._2 == sliceCompPairs(i)._2) || // Or if it is not unique but its compliment collides
                          !firstInstances.contains(sliceCompPairs(i)._1) // Or if it is the first time we are seeing this slice
                        ) =>  // then add it to the group
                          firstInstances = firstInstances ++ Seq(sliceCompPairs(i)._1)
                          sliceCompPairs(i)._1
            }:_*)
          }
          val stagedDims = dims.map(mem.stagedDims.map(_.toInt))
          findBanking(selGrps, dims, stagedDims)
        }
        if (isValidBanking(banking,grps)) {
          if (!mem.getPadding.isDefined) mem.padding = mem.stagedDims.map(_.toInt).zip(banking.head.Ps).map{case(d,p) => (p - d%p) % p}
          Some(banking)
        } else None
      }
    }
  }

  /** True if this is a valid banking strategy for the given sets of access matrices. */
  def isValidBanking(banking: Seq[ModBanking], grps: Set[Seq[SparseMatrix[Idx]]]): Boolean = {
    // TODO[2]: This may not be correct in all cases, need to verify!
    val banks = banking.map(_.nBanks).product
    grps.forall{_.lengthLessThan(banks+1)}
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
  def Alphas(rank: Int, N: Int, stagedDims: Seq[Int]): Iterator[Seq[Int]] = {
    // Prime factors of number, for shortcircuiting the brute force alphas
    def factorize(number: Int, list: List[Int] = List()): List[Int] = {
      for(n <- 2 to number if (number % n == 0)) {
        return factorize(number / n, list :+ n)
      }
      list
    }

    val a2 = (0 to 2*N).filter(x => isPow2(x) || x == 1 || x == 0).uniqueModN(N)
    val alikely = (
                Seq(0,1) ++ 
                Seq.tabulate(factorize(N).length){i => factorize(N).combinations(i+1).toList}.flatten.map(_.product) ++ 
                Seq.tabulate(stagedDims.length){i => stagedDims.combinations(i+1).toList}.flatten.map(_.product).filter(_ <= N) ++ 
                (0 to 2*N).filter(isPow2(_))
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
    val pow2As = Alphas2(1, Nil).filterNot(_.forall(_ == 0))
    val likelyAs = AlphasLikely(1, Nil).filterNot{x => x.forall(_ == 0) || x.forall(isPow2(_))}
    val xAs = AlphasX(1, Nil).filterNot(_.forall(_ == 0))
    pow2As ++ likelyAs ++ xAs
  }

  private def computeP(n: Int, b: Int, alpha: Seq[Int], stagedDims: Seq[Int]): Seq[Int] = {
    /* Offset correction not mentioned in p199-wang
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
    def combs(lol: List[List[Int]]): List[List[Int]] = lol match {
        case Nil => List(Nil)
        case l::rs => for(x <- l;cs <- combs(rs)) yield x::cs
      }
    def allLoops(maxes: Seq[Int], steps: Seq[Int], iterators: Seq[Int]): Seq[Int] = maxes match {
      case Nil => Nil
      case h::tail if tail.nonEmpty => (0 to h-1).map{i => allLoops(tail, steps.tail, iterators ++ Seq(i*steps.head))}.flatten
      case h::tail if tail.isEmpty => (0 to h-1).map{i => i*steps.head + iterators.sum}
    }
    def spansAllBanks(p: Seq[Int], a: Seq[Int], N: Int): Boolean = {
      allLoops(p,a,Nil).map(_%N).sorted.distinct.length == N
    }
    def gcd(a: Int,b: Int): Int = if(b ==0) a else gcd(b, a%b)
    def divisors(x: Int): Seq[Int] = (1 to x).collect{case i if (x % i == 0) => i}
    try {
      val P_raw = alpha.indices.map{i => if (alpha(i) == 0) 1 else n*b/gcd(n*b,alpha(i))}
      val P_expanded = Seq.tabulate(alpha.size){i => divisors(P_raw(i)) ++ List(stagedDims(i))}
      val options = combs(P_expanded.map(_.toList).toList).filter(_.product == n*b).collect{case p if (spansAllBanks(p,alpha,n*b)) => p}
      val PandCost = options.map{option => 
        val padding = stagedDims.zip(option).map{case(d,p) => (p - d%p) % p}
        val volume = stagedDims.zip(padding).map{case(x,y)=>x+y}.product
        (option,volume)
      }
      PandCost.sortBy(_._2).head._1
    }
    catch { case t:Throwable =>
      bug(s"Could not fence off a region for banking scheme N=$n, B=$b, alpha=$alpha")
      throw t
    }
  }


  protected def findBanking(grps: Set[Seq[SparseMatrix[Idx]]], dims: Seq[Int], stagedDims: Seq[Int]): ModBanking = {
    val rank = dims.length
    val Nmin: Int = grps.map(_.size).maxOrElse(1)
    val (n2,nx) = (Nmin to 8*Nmin).partition{i => isPow2(i) }
    val n2Head = if (n2.head.toDouble/Nmin > MAGIC_CUTOFF_N) Seq(Nmin) else Nil
    val Ns = (n2Head ++ n2 ++ nx).iterator

    var banking: Option[ModBanking] = None
    var attempts = 0

    while(Ns.hasNext && banking.isEmpty) {
      val N = Ns.next()
      val As = Alphas(rank, N, stagedDims)
      while (As.hasNext && banking.isEmpty && attempts < maxAttempts) {
        val alpha = As.next()
        if (attempts < 200) dbgs(s"     Checking N=$N and alpha=$alpha")
        else if (attempts == 200) dbgs(s"    ...")
          else if (attempts == maxAttempts-2) dbgs(s"    Could not find banking scheme after $attempts attempts!  Giving up...")
        attempts = attempts + 1
        if (checkCyclic(N,alpha,grps)) {
          dbgs(s"     Success on N=$N, alpha=$alpha, B=1")
          val P = computeP(N,1,alpha,stagedDims)
          banking = Some(ModBanking(N,1,alpha,dims,P))
        } else {
          val B = Bs.find{b => val x = checkBlockCyclic(N,b,alpha,grps); if (x) dbgs(s"     Success on N=$N, alpha=$alpha, B=$b"); x}
          banking = B.map{b => 
            val P = computeP(N, b, alpha, stagedDims)
            ModBanking(N, b, alpha, dims, P) 
          }
        }
      }
    }

    banking.getOrElse(ModBanking.Unit(rank))
  }

  implicit class SeqMath(a: Seq[Int]) {
    def *(b: SparseMatrix[Idx]): SparseVector[Idx] = {
      val vec = b.keys.mapping{k => b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i(k)*a_i }.sum }
      val c = b.rows.zip(a).iterator.map{case (row_i,a_i) => row_i.c*a_i}.sum
      // Combined modulus is sum of moduli: a0*dim0 mod m0 + a1*dim1 mod m1 = a0*dim0 + a1*dim1 + k(m0+m1)
      val mod = b.rows.map(_.modulus).sum
      SparseVector[Idx](vec,c,Map.empty,mod)
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
