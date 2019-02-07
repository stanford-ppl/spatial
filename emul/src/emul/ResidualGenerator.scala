package emul.ResidualGenerator

/** ResidualGenerator elaborates the possible banks that a lane of an access may access. */

case class ResidualGenerator(A: Int, B: Int, M: Int) {
  def static: Boolean = A == M 
  def full: Boolean = A == 1
  def resolvesTo: Option[Int] = if (static) Some(B) else None
  def expand(max: Int): Seq[Int] = if (M == 0) (0 until scala.math.ceil(max.toDouble/A.toDouble).toInt).map{i => (i*A + B) % max} else (0 until scala.math.ceil(M.toDouble/A.toDouble).toInt).map{i => (i*A + B) % M}
}
