package utils
import utils.implicits.collections._

package object math {
  def log2(x: Double): Double = Math.log10(x)/Math.log10(2)
  def isPow2(x: Int): Boolean = (x & (x-1)) == 0
  
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
      case l::rs => for(x <- l;cs <- combs(rs)) yield x::cs
    }

  def gcd(a: Int,b: Int): Int = if(b ==0) a else gcd(b, a%b)
  def coprime(x: Seq[Int]): Boolean = {
    x.size == 1 || !x.forallPairs(gcd(_,_) > 1)
  }
  def nCr(n: Int, r: Int): Int = {
    if (n > r) ({n-r+1} to n).map{i => i}.product/(1 to r).map{i => i}.product
    else 1
  }
  def divisors(x: Int): Seq[Int] = (1 to x).collect{case i if x % i == 0 => i}

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


  /** Cyclic banking helper functions */
  def log2Ceil(n: BigInt): Int = 1 max (scala.math.ceil(scala.math.log(1 max (1+n.toInt))).toInt + 2)
  def log2Ceil(n: Int): Int = log2Ceil(BigInt(n))
  def log2Ceil(n: Double): Int = log2Ceil(BigInt(n.toInt))
  def log2Up(n: Int): Int = log2Up(BigInt(n))
  def log2Up(n: BigInt): Int = if (n < 0) log2Ceil((n.abs + 1) max 1) max 1 else log2Ceil(n max 1) max 1
  def log2Up(n: Double): Int = log2Up(n.toInt)
  def nhoods(Ds: Seq[Int], Ps: Seq[Int]): Int = Ds.zip(Ps).map{case (d,s) => scala.math.ceil(d/s).toInt}.product
  def hiddenVolume(Ns: Seq[Int], Bs: Seq[Int], Ps: Seq[Int], Ds: Seq[Int]) : Int = {
    if (Ns.size == 1) Bs.head*Ps.map{_ % Ns.head}.min*nhoods(Ds, Ps)
    else Ps.zip(Ns).zipWithIndex.map{case ((s,n),i) => Bs(i)*(s % n)*Ps.patch(i,Nil,1).product}.sum * nhoods(Ds, Ps)
  }
  def volume(Ns: Seq[Int], Bs: Seq[Int], Ps: Seq[Int], Ds: Seq[Int]): Int = Ds.product + hiddenVolume(Ns, Bs, Ps, Ds)
  def numBanks(Ns: Seq[Int]): Int = Ns.product
  def ofsWidth(volume: Int, Ns: Seq[Int]): Int = log2Up(volume/Ns.product)
  def banksWidths(Ns: Seq[Int]): Seq[Int] = Ns.map(log2Up)
  def elsWidth(volume: Int): Int = log2Up(volume) + 2
}
