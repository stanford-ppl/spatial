package emul.ResidualGenerator

/** ResidualGenerator elaborates the possible banks that a lane of an access may access. */

case class ResidualGenerator(A: Int, B: Int, M: Int) {
  def static: Boolean = A == M 
  def full: Boolean = A == 1
  def resolvesTo: Option[Int] = if (static) Some(B) else None
  def expand(max: Int): Seq[Int] = if (M == 0) Seq.tabulate(max){i => i} else (0 until M/A).map{i => (i*A + B) % M}
}
