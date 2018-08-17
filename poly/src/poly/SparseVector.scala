package poly

/** Represents a sparse vector of integers for use in representing affine functions of the form:
  *   a_1*k_1 + ... + a_N*k_N + c
  * Where {a_1...a_N} are integer multipliers and {k_1...k_N} are symbolic values of type K,
  * c is an integer offset, and m is an optional modulus factor.
  *
  * @param cols Map of keys of type K to integer values
  * @param c Constant entry
  * @param lastIters Mapping from symbol to the innermost iterator it varies with, None if it is entirely loop invariant
  */
case class SparseVector[K](cols: Map[K,Int], c: Int, lastIters: Map[K,Option[K]])
   extends SparseVectorLike[K]
{
  import ConstraintType._

  final def m: Option[Int] = None

  def asMinConstraint(x: K): SparseConstraint[K] = SparseConstraint[K](cols.map{case (i,a) => i -> -a} ++ Map(x -> 1) , -c, GEQ_ZERO)
  def asMaxConstraint(x: K): SparseConstraint[K] = SparseConstraint[K](cols ++ Map(x -> -1), c, GEQ_ZERO)
  def asConstraintEqlZero: SparseConstraint[K] = SparseConstraint[K](cols, c, EQL_ZERO)
  def asConstraintGeqZero: SparseConstraint[K] = SparseConstraint[K](cols, c, GEQ_ZERO)

  def >==(b: Int): SparseConstraint[K] = SparseConstraint[K](cols, c - b, GEQ_ZERO)
  def ===(b: Int): SparseConstraint[K] = SparseConstraint[K](cols, c - b, EQL_ZERO)

  def map(func: Int => Int): SparseVector[K] = {
    val cols2 = cols.mapValues{v => func(v) }
    SparseVector(cols2, func(c), lastIters)
  }

  def empty(cst: Int): SparseVector[K] = {
    val cols2 = cols.mapValues{_ => 0}
    SparseVector(cols2, cst, lastIters)
  }

  def zip(that: SparseVector[K])(func: (Int,Int) => Int): SparseVector[K] = {
    val keys = this.keys ++ that.keys
    val cols2 = keys.map{k => k -> func(this(k), that(k)) }.toMap
    SparseVector(cols2, func(this.c, that.c), this.lastIters ++ that.lastIters)
  }

  def unary_-(): SparseVector[K] = this.map{x => -x}
  def +(that: SparseVector[K]): SparseVector[K] = this.zip(that){_+_}
  def -(that: SparseVector[K]): SparseVector[K] = this.zip(that){_-_}
  def +(b: (K,Int)): SparseVector[K] = SparseVector[K](this.cols + b, c, lastIters)
  def -(b: (K,Int)): SparseVector[K] = SparseVector[K](this.cols + ((b._1,-b._2)), c, lastIters)
  def +(b: Int): SparseVector[K] = SparseVector[K](this.cols, c + b, lastIters)
  def -(b: Int): SparseVector[K] = SparseVector[K](this.cols, c - b, lastIters)
}

