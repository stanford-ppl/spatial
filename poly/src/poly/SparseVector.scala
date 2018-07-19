package poly

case class SparseVector[K](cols: Map[K,Int], c: Int, lastIters: Map[K,Option[K]], mod: Int) extends SparseVectorLike[K] {
  import ConstraintType._

  def asMinConstraint(x: K) = SparseConstraint[K](cols.map{case (i,a) => i -> -a} ++ Map(x -> 1) , -c, mod, GEQ_ZERO)
  def asMaxConstraint(x: K) = SparseConstraint[K](cols ++ Map(x -> -1), c, mod, GEQ_ZERO)
  def asConstraintEqlZero = SparseConstraint[K](cols, c, mod, EQL_ZERO)
  def asConstraintGeqZero = SparseConstraint[K](cols, c, mod, GEQ_ZERO)

  def >==(b: Int) = SparseConstraint[K](cols, c - b, mod, GEQ_ZERO)
  def ===(b: Int) = SparseConstraint[K](cols, c - b, mod, EQL_ZERO)

  def map(func: Int => Int): SparseVector[K] = {
    val cols2 = cols.mapValues{v => func(v) }
    SparseVector(cols2, func(c), lastIters, mod)
  }

  override def modulus = mod

  def zip(that: SparseVector[K])(func: (Int,Int) => Int): SparseVector[K] = {
    val keys = this.keys ++ that.keys
    val cols2 = keys.map{k => k -> func(this(k), that(k)) }.toMap
    val mod2 = mod + that.modulus // a mod b + c mod d = a + c mod(b+d)
    SparseVector(cols2, func(this.c, that.c), this.lastIters ++ that.lastIters, mod2)
  }

  def unary_-(): SparseVector[K] = this.map{x => -x}
  def +(that: SparseVector[K]): SparseVector[K] = this.zip(that){_+_}
  def -(that: SparseVector[K]): SparseVector[K] = this.zip(that){_-_}
  def +(b: (K,Int)): SparseVector[K] = SparseVector[K](this.cols + b, c, lastIters, mod)
  def -(b: (K,Int)): SparseVector[K] = SparseVector[K](this.cols + ((b._1,-b._2)), c, lastIters, mod)
  def +(b: Int): SparseVector[K] = SparseVector[K](this.cols, c + b, lastIters, mod)
  def -(b: Int): SparseVector[K] = SparseVector[K](this.cols, c - b, lastIters, mod)
}

