package nova.poly

import forge.tags.stateful

case class SparseConstraint(cols: Map[Idx,Int], c: Int, tp: ConstraintType) extends SparseVectorLike {
  def toDenseString(keys: Seq[Idx]): String = tp.toString + " " + keys.map{x => this(x) }.mkString(" ") + " " + c.toString
  def toDenseVector(keys: Seq[Idx]): Seq[Int] = tp.toInt +: keys.map{x => this(x) } :+ c

  @stateful def andDomain: ConstraintMatrix = ConstraintMatrix(Set(this)).andDomain

  def isEmpty(implicit isl: ISL): Boolean = isl.isEmpty(this)
  def nonEmpty(implicit isl: ISL): Boolean = isl.nonEmpty(this)

  def ::(that: SparseConstraint): ConstraintMatrix = ConstraintMatrix(Set(this,that))
}