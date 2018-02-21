package nova.poly

case class SparseVector(cols: Map[Idx,Int], c: Int, lastIters: Map[Idx,Option[Idx]]) extends SparseVectorLike {
  import ConstraintType._

  def asMinConstraint(x: Idx) = SparseConstraint(cols.map{case (i,a) => i -> -a} ++ Map(x -> 1) , -c, GEQ_ZERO)
  def asMaxConstraint(x: Idx) = SparseConstraint(cols ++ Map(x -> -1), c, GEQ_ZERO)
  def asConstraintEqlZero = SparseConstraint(cols, c, EQL_ZERO)
  def asConstraintGeqZero = SparseConstraint(cols, c, GEQ_ZERO)

  def >==(b: Int) = SparseConstraint(cols, c - b, GEQ_ZERO)
  def ===(b: Int) = SparseConstraint(cols, c - b, EQL_ZERO)

  def map(func: Int => Int): SparseVector = {
    val cols2 = cols.mapValues{v => func(v) }
    SparseVector(cols2, func(c), lastIters)
  }
  def zip(that: SparseVector)(func: (Int,Int) => Int): SparseVector = {
    val keys = this.keys ++ that.keys
    val cols2 = keys.map{k => k -> func(this(k), that(k)) }.toMap
    SparseVector(cols2, func(this.c, that.c), this.lastIters ++ that.lastIters)
  }

  def unary_-(): SparseVector = this.map{x => -x}
  def +(that: SparseVector): SparseVector = this.zip(that){_+_}
  def -(that: SparseVector): SparseVector = this.zip(that){_-_}
  def +(b: (Idx,Int)): SparseVector = SparseVector(this.cols + b, c, lastIters)
  def -(b: (Idx,Int)): SparseVector = SparseVector(this.cols + ((b._1,-b._2)), c, lastIters)
  def +(b: Int): SparseVector = SparseVector(this.cols, c + b, lastIters)
  def -(b: Int): SparseVector = SparseVector(this.cols, c - b, lastIters)
}

