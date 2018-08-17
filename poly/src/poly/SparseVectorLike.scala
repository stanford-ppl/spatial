package poly

abstract class SparseVectorLike[K] {
  def cols: Map[K,Int]
  def c: Int

  // TODO: Modulus support
  def m: Option[Int]
  def mod: Int = m.getOrElse(0)

  def keys: Set[K] = cols.keySet
  def apply(x: K): Int = if (cols.contains(x)) cols(x) else 0
  def getOrElse(x: K, v: => Int): Int = cols.getOrElse(x,v)
}
