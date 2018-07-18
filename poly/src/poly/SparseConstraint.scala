package poly

case class SparseConstraint[K](cols: Map[K,Int], c: Int, mod: Int, tp: ConstraintType) extends SparseVectorLike[K] {
  def toDenseString(keys: Seq[K]): String = tp.toString + " " + keys.map{x => this(x) }.mkString(" ") + " " + mod.toString + " " + c.toString
  def toDenseVector(keys: Seq[K]): Seq[Int] = tp.toInt +: keys.map{x => this(x) } :+ mod :+ c
  override def modulus = mod

  def andDomain(implicit isl: ISL): ConstraintMatrix[K] = ConstraintMatrix[K](Set(this)).andDomain
  def isEmpty(implicit isl: ISL): Boolean = isl.isEmpty(this)
  def nonEmpty(implicit isl: ISL): Boolean = isl.nonEmpty(this)

  def ::(that: SparseConstraint[K]): ConstraintMatrix[K] = ConstraintMatrix[K](Set(this,that))
}