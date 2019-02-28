package emul.ResidualGenerator

/** ResidualGenerator elaborates the possible banks that a lane of an access may access. */

case class ResidualGenerator(A: Int, B: Seq[Int], M: Int) {
  override def toString: String = if (A == M) s"RG([${B.mkString(",")}])" else s"RG($A,[${B.mkString(",")}],$M)"

  def static: Boolean = (A == M)
  def full: Boolean = A == 1
  def resolvesTo: Option[Seq[Int]] = if (static) Some(B) else None
  def expand(max: Int): Seq[Int] = 
    if (M == 0) (0 until scala.math.ceil(max.toDouble/A.toDouble).toInt).flatMap{i => B.map { b => (i*A + b) % max } } 
  	else if (A == 0) B // This RG should never exist, but technically it expands to Seq(B)!
    else (0 until scala.math.ceil(M.toDouble/A.toDouble).toInt).flatMap{i => B.map { b => (i*A + b) % M } }
}
object ResidualGenerator{
  // Helper for creating RG with singular constant expansion
  def apply(cst: Int): ResidualGenerator = ResidualGenerator(cst+1, cst, cst+1)
  def apply(A: Int, B: Int, M: Int): ResidualGenerator = ResidualGenerator(A, Seq(B), M)
}
object RG{
  def apply(cst: Int): ResidualGenerator = ResidualGenerator(cst)
  def apply(A: Int, B: Int, M: Int): ResidualGenerator = ResidualGenerator(A,B,M)
}
