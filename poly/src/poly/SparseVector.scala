package poly

import emul.ResidualGenerator._

/** Represents a sparse vector of integers for use in representing affine functions of the form:
  *   a_1*k_1 + ... + a_N*k_N + c
  * Where {a_1...a_N} are integer multipliers and {k_1...k_N} are symbolic values of type K,
  * c is an integer offset, and m is an optional modulus factor.
  *
  * @param cols Map of keys of type K to integer values
  * @param c Constant entry
  * @param allIters Mapping from symbol key in cols to all iterators it varies with
  * @param lastIters Mapping from symbol to the innermost iterator it varies with, None if it is entirely loop invariant
  */
case class SparseVector[K](cols: Map[K,Int], c: Int, allIters: Map[K,Seq[K]])
   extends SparseVectorLike[K]
{
  import ConstraintType._

  final def m: Option[Int] = None

  def asMinConstraint(x: K): SparseConstraint[K] = SparseConstraint[K](cols.map{case (i,a) => i -> -a} ++ Map(x -> 1) , -c, GEQ_ZERO)
  def asMaxConstraint(x: K): SparseConstraint[K] = SparseConstraint[K](cols ++ Map(x -> -1), c-1, GEQ_ZERO)
  def asConstraintEqlZero: SparseConstraint[K] = SparseConstraint[K](cols, c, EQL_ZERO)
  def asConstraintGeqZero: SparseConstraint[K] = SparseConstraint[K](cols, c, GEQ_ZERO)

  def >==(b: Int): SparseConstraint[K] = SparseConstraint[K](cols, c - b, GEQ_ZERO)
  def ===(b: Int): SparseConstraint[K] = SparseConstraint[K](cols, c - b, EQL_ZERO)

  def map(func: Int => Int): SparseVector[K] = {
    val cols2 = cols.mapValues{v => func(v) }
    SparseVector(cols2, func(c), allIters)
  }

  def empty(cst: Int): SparseVector[K] = {
    val cols2 = cols.mapValues{_ => 0}
    SparseVector(cols2, cst, allIters)
  }

  def zip(that: SparseVector[K])(func: (Int,Int) => Int): SparseVector[K] = {
    val keys = this.keys ++ that.keys
    val cols2 = keys.map{k => k -> func(this(k), that(k)) }.toMap
    SparseVector(cols2, func(this.c, that.c), this.allIters ++ that.allIters)
  }

  def unary_-(): SparseVector[K] = this.map{x => -x}
  def +(that: SparseVector[K]): SparseVector[K] = this.zip(that){_+_}
  def -(that: SparseVector[K]): SparseVector[K] = this.zip(that){_-_}
  def +(b: (K,Int)): SparseVector[K] = SparseVector[K](this.cols + b, c, allIters)
  def -(b: (K,Int)): SparseVector[K] = SparseVector[K](this.cols + ((b._1,-b._2)), c, allIters)
  def +(b: Int): SparseVector[K] = SparseVector[K](this.cols, c + b, allIters)
  def -(b: Int): SparseVector[K] = SparseVector[K](this.cols, c - b, allIters)

  def span(N: Int, B: Int): ResidualGenerator = {
    import utils.math.{gcd, allLoops}
    import utils.implicits.collections._
    val P_raw = cols.values.map{v => val posV = ((v % N) + N) % N; if (posV == 0 && B == 1) 1 else N*B/gcd(N,posV)}
    val allBanksAccessible =
      if (cols.size > 0) allLoops(P_raw.toList.distinct, cols.values.toList, B, Nil).map{x => (x+c)%N}.distinct.map{x => ((x%N) + N) % N}.distinct.sorted
      else Seq(c%N)
    if (allBanksAccessible.size == N) ResidualGenerator(1,0,N)
    else if (allBanksAccessible.stepSizeUnderMod(N).isDefined) ResidualGenerator(allBanksAccessible.stepSizeUnderMod(N).get, 0, N)
    else ResidualGenerator(0, allBanksAccessible, N)
  }
}

