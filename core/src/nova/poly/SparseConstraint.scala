package nova.poly

import forge.tags._
import forge.tags.stateful
import nova.lang.I32

case class SparseConstraint(cols: Map[I32,Int], c: Int, tp: ConstraintType) extends SparseVectorLike {
  def toDenseString(keys: Seq[I32]): String = tp.toString + " " + keys.map{x => this(x) }.mkString(" ") + " " + c.toString
  def toDenseVector(keys: Seq[I32]): Seq[Int] = tp.toInt +: keys.map{x => this(x) } :+ c

  @stateful def andDomain: ConstraintMatrix = ConstraintMatrix(Set(this)).andDomain

  def isEmpty(implicit isl: ISL): Boolean = isl.isEmpty(this)
  def nonEmpty(implicit isl: ISL): Boolean = isl.nonEmpty(this)

  def ::(that: SparseConstraint): ConstraintMatrix = ConstraintMatrix(Set(this,that))
}