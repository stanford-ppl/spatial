package poly

/** A constraint for an affine function of the form:
  *   a_1*k_1 + ... + a_N*k_N + c) ?? 0
  *   Where {a_1...a_N} are integer multipliers and {k_1...k_N} are symbolic values of type K.
  *
  * Two constraint types currently supported are >= 0 and == 0.
  *
  * @param cols Symbols (k_1...k_N) and multipliers {a_1...a_N}, tracked as a map from k_i to a_i.
  * @param c The constant offset.
  * @param tp The constraint type ( >= 0 or == 0 )
  */
case class SparseConstraint[K](cols: Map[K,Int], c: Int, tp: ConstraintType)
   extends SparseVectorLike[K] {

  final def m: Option[Int] = None

  def toDenseString(keys: Seq[K]): String = s"$tp ${keys.map{x => this(x) }.mkString(" ")} $mod $c"
  def toDenseVector(keys: Seq[K]): Seq[Int] = tp.toInt +: keys.map{x => this(x) } :+ mod :+ c

  def andDomain(implicit isl: ISL): ConstraintMatrix[K] = ConstraintMatrix[K](Set(this)).andDomain
  def isEmpty(implicit isl: ISL): Boolean = isl.isEmpty(this)
  def nonEmpty(implicit isl: ISL): Boolean = isl.nonEmpty(this)

  def ::(that: SparseConstraint[K]): ConstraintMatrix[K] = ConstraintMatrix[K](Set(this,that))
}
