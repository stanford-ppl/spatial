package poly

abstract class SparseVectorLike[K] {
  def cols: Map[K,Int]
  def c: Int
  def modulus: Int

  def keys: Set[K] = cols.keySet
  def apply(x: K): Int = if (cols.contains(x)) cols(x) else 0
  def getOrElse(x: K, v: => Int): Int = cols.getOrElse(x,v)
}
