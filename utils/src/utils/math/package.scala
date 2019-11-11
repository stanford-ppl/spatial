package utils
import utils.implicits.collections._

package object math {
  def log2(x: Double): Double = Math.log10(x) / Math.log10(2)

  def isPow2(x: Int): Boolean = (x & (x - 1)) == 0

  // TODO: fix scaladoc here
  /* Find all combinations out of list of lists.  i.e.
    *   combs(List( List(a,b),
    *               List(x,y,z),
    *               List(m,n)
    *              )
    *         )
    *         =
    *         List( List(a,x,m), List(a,x,n), List(a,y,m), List(a,y,n), ...)
    */
  def combs[T](lol: List[List[T]]): List[List[T]] = lol match {
    case Nil => List(Nil)
    case l :: rs => for (x <- l; cs <- combs(rs)) yield x :: cs
  }

  def gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

  def coprime(x: Seq[Int]): Boolean = {
    x.size == 1 || !x.forallPairs(gcd(_, _) > 1)
  }

  def nCr(n: Int, r: Int): Int = {
    if (n > r) ({
      n - r + 1
    } to n).map { i => i }.product / (1 to r).map { i => i }.product
    else 1
  }

  def divisors(x: Int): Seq[Int] = (1 to x).collect { case i if x % i == 0 => i }

  /** Check if this is a Mersenne number */
  def isMersenne(x: Int): Boolean = {
    ((scala.math.log(x + 1) / scala.math.log(2)) % 1.0 == 0) && ((scala.math.log(x + 1) / scala.math.log(2)) <= 16) && (x > 2)
  }
  /** Returns y such that y = x*k, where y is Mersenne and k <= N */
  def withinNOfMersenne(N: Int, x: Int): Option[Int] = {
    if (!isMersenne(x) && x > 3) List.tabulate(N-2){i => x*(i+2)}.collectFirst{case i if isMersenne(i) => i}
    else None
  }

  /** Check if int can be expressed as the sum or subtraction of two powers of 2, to be used for multiplication optimizations */
  def isSumOfPow2(x: Int): Boolean = {
    (x.toBinaryString.count(_ == '1') == 2) || (x.toBinaryString.indexOf("01") == -1)
  }

  /** Check if int can be expressed as the sum or subtraction of two powers of 2, to be used for multiplication optimizations */
  def asSumOfPow2(x: Int): (Int, Int, String) = {
    import scala.math.pow
    if (x.toBinaryString.count(_ == '1') == 2) {
      val maxbit = x.toBinaryString.length-1 // Assume msb is a 1 for toBinaryString method
      val minbit = x.toBinaryString.reverse.indexOf("1")
      (pow(2,maxbit).toInt, pow(2,minbit).toInt, "add")
    } else {//if (x.toBinaryString.indexOf("01") == -1) {
      val maxbit = x.toBinaryString.length // Assume msb is a 1 for toBinaryString method
      val minbit = x.toBinaryString.reverse.indexOf("1")
      (pow(2,maxbit).toInt, pow(2,minbit).toInt, "sub")
    }
  }

  /** Given the dimensions of a hypercube (i.e. maxes), a step size per dimension (i.e. a), and a scaling factor (i.e. B),
    * determine what elements are possible via the equation: (address * a) / B.  This is used to figure out what banks
    * are visible within a certain region of a memory
    */
  def allLoops(maxes: Seq[Int], a: Seq[Int], B: Int, iterators: Seq[Int]): Seq[Int] = maxes match {
    case Nil => Nil
    case h::tail if tail.nonEmpty => (0 to h-1).flatMap{i => allLoops(tail, a.tail, B, iterators ++ Seq(i*a.head/B))}
    case h::tail if tail.isEmpty => (0 to h-1).map{i => i*a.head/B + iterators.sum}
  }
  def spansAllBanks(p: Seq[Int], a: Seq[Int], N: Int, B: Int): Boolean = {
    val banksInFence = allLoops(p,a,B,Nil).map(_%N)
    List.tabulate(N){i => i}.forall{i => banksInFence.count(_ == i) <= B}
  }
  /** Given (padded) dims of a memory, P for that memory, and histogram mapping bank to number of degenerates for that bank per yard,
    * figure out how many inaccessible physical addresses exist
    */
  def computeDarkVolume(paddedDims: Seq[Int], P: Seq[Int], hist: Map[Int,Int]): Int = { 
    val degenerate = hist.map(_._2).max
    hist.map(_._2).map(degenerate - _).sum * paddedDims.zip(P).map{case(x,y) => x/y}.product 
  }
  def bestPByVolume(options: Seq[Seq[Int]], stagedDims: Seq[Int]): Seq[Int] = {
      val PandCostandBloat = options.map{option =>
        // Extra elements per dimension so that yards cover entire memory
        val padding = stagedDims.zip(option).map{case(d,p) => (p - d%p) % p}
        val paddedDims = stagedDims.zip(padding).map{case(x,y)=>x+y}
        val volume = paddedDims.product
        (option,volume)
      }
      PandCostandBloat.minBy(_._2)._1
  }

  def computeP(n: Int, b: Int, alpha: Seq[Int], stagedDims: Seq[Int], errmsg: => Unit): Seq[Seq[Int]] = {
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
      val P_raw = alpha.indices.map{i => if (alpha(i) == 0) 1 else n*b/gcd(n*b,alpha(i))}
      val P_expanded = Seq.tabulate(alpha.size){i => divisors(P_raw(i)) ++ {if (P_raw(i) != 1 && b == 1) List(stagedDims(i)) else List()}} // Force B == 1 for stagedDim P to make life easier
      val options = combs(P_expanded.map(_.toList).toList)
//            .collect{case p if p.product == n*b => p}
            // Volume constraint
            .collect{case p if p.length == 1 && p == P_raw => p
                     case p if p.length > 1 && p.product == b*P_raw.map{pr => n - (pr % n)}.max => p
                    }
            // Census constraint
            .collect{case p if spansAllBanks(p,alpha,n,b) => p}
      options
    }
    catch { case t:Throwable =>
      errmsg
      throw t
    }
  }

  /** Cyclic banking helper functions */
  def log2Ceil(n: BigInt): Int = 1 max (scala.math.ceil(scala.math.log(1 max (1+n.toInt))/scala.math.log(2)).toInt)
  def log2Ceil(n: Int): Int = log2Ceil(BigInt(n))
  def log2Ceil(n: Double): Int = log2Ceil(BigInt(n.toInt))
  def log2Up(n: Int): Int = log2Up(BigInt(n))
  def log2Up(n: BigInt): Int = if (n < 0) log2Ceil((n.abs + 1) max 1) max 1 else log2Ceil(n max 1) max 1
  def log2Up(n: Double): Int = log2Up(n.toInt)
  def nhoods(Ds: Seq[Int], Ps: Seq[Int]): Int = Ds.zip(Ps).map{case (d,s) => scala.math.ceil(d/s).toInt}.product
  def hiddenVolume(Ns: Seq[Int], Bs: Seq[Int], Ps: Seq[Int], Ds: Seq[Int]) : Int = {
    if (Ps.size == 0) 0
    else if (Ns.size == 1) {
      val hang = Ps.map{_ % Ns.head}.min
      if (hang == 0) 0 else Bs.head*(Ns.head - hang)*nhoods(Ds, Ps)
    }
    else Ps.zip(Ns).zipWithIndex.map{case ((p,n),i) =>
      val hang = p % n
      if (hang == 0) 0 else Bs(i)*(n - hang)*Ps.patch(i,Nil,1).product
    }.sum * nhoods(Ds, Ps)
  }
  def volume(Ns: Seq[Int], Bs: Seq[Int], Ps: Seq[Int], Ds: Seq[Int]): Int = Ds.product + hiddenVolume(Ns, Bs, Ps, Ds)
  def numBanks(Ns: Seq[Int]): Int = Ns.product
  def ofsWidth(volume: Int, Ns: Seq[Int]): Int = log2Up(scala.math.ceil(volume/Ns.product))
  def banksWidths(Ns: Seq[Int]): Seq[Int] = Ns.map(log2Up)
  def elsWidth(volume: Int): Int = log2Up(volume) + 2
}
