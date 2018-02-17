package nova.poly

import nova.lang.I32

abstract class SparseVectorLike {
  def cols: Map[I32,Int]
  def c: Int

  def keys: Set[I32] = cols.keySet
  def apply(x: I32): Int = if (cols.contains(x)) cols(x) else 0
  def getOrElse(x: I32, v: => Int): Int = cols.getOrElse(x,v)
}
