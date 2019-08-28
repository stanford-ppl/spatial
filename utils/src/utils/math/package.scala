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
  def spansAllBanks(p: Seq[Int], a: Seq[Int], N: Int, B: Int, allPossible: Seq[Int]): Boolean = {
    val banksInFence = allLoops(p,a,B,Nil).map(_%N)
    allPossible.forall{b => val occurs = banksInFence.count(_==b); occurs >= 1 && occurs <= B}
  }
  /** Given (padded) dims of a memory, P for that memory, and histogram mapping bank to number of degenerates for that bank per yard,
    * figure out how many inaccessible physical addresses exist
    */
  def computeDarkVolume(paddedDims: Seq[Int], P: Seq[Int], hist: Map[Int,Int]): Int = { 
    val degenerate = hist.map(_._2).max
    hist.map(_._2).map(degenerate - _).sum * paddedDims.zip(P).map{case(x,y) => x/y}.product 
  }

}
