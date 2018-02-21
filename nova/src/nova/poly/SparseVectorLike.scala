package nova.poly

abstract class SparseVectorLike {
  def cols: Map[Idx,Int]
  def c: Int

  def keys: Set[Idx] = cols.keySet
  def apply(x: Idx): Int = if (cols.contains(x)) cols(x) else 0
  def getOrElse(x: Idx, v: => Int): Int = cols.getOrElse(x,v)
}
