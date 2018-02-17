package pcc.poly

import pcc.lang.I32
import pcc.poly.ConstraintType._

case class SparseVector(
  cols:      Map[I32,Int],
  c:         Int,
  lastIters: Map[I32,Option[I32]]
) extends SparseVectorLike {

  def asMinConstraint(x: I32) = SparseConstraint(cols.map{case (i,a) => i -> -a} ++ Map(x -> 1) , -c, GEQ_ZERO)
  def asMaxConstraint(x: I32) = SparseConstraint(cols ++ Map(x -> -1), c, GEQ_ZERO)
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
    val cols2 = keys.map{x => x -> func(this(x), that(x)) }.toMap
    SparseVector(cols2, func(this.c, that.c), this.lastIters ++ that.lastIters)
  }

  def unary_-(): SparseVector = this.map{x => -x}
  def +(that: SparseVector): SparseVector = this.zip(that){_+_}
  def -(that: SparseVector): SparseVector = this.zip(that){_-_}
  def +(b: (I32,Int)): SparseVector = SparseVector(this.cols + b, c, lastIters)
  def -(b: (I32,Int)): SparseVector = SparseVector(this.cols + ((b._1,-b._2)), c, lastIters)
  def +(b: Int): SparseVector = SparseVector(this.cols, c + b, lastIters)
  def -(b: Int): SparseVector = SparseVector(this.cols, c - b, lastIters)
}

