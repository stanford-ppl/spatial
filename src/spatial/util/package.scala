package spatial

import argon._
import forge.tags.stateful
import spatial.metadata.control._

package object util {

  def spatialConfig(implicit state: State): SpatialConfig = state.config.asInstanceOf[SpatialConfig]

  /** True if the given symbols can be safely moved out of a conditional scope. */
  @stateful def canMotionFromConditional(stms: Seq[Sym[_]]): Boolean = {
    stms.forall{s => !s.takesEnables && s.effects.isIdempotent }
  }

  /** True if these symbols can be motioned and have little cost to be moved. */
  @stateful def shouldMotionFromConditional(stms: Seq[Sym[_]], inHw: Boolean): Boolean = {
    canMotionFromConditional(stms) && (inHw || stms.length == 1)
  }

  /** Calculate delay line costs:
    * a. Determine time (in cycles) any given input or internal signal needs to be delayed
    * b. Distinguish each delay line as a separate entity
    *
    * To determine this without fully building the tree:
    * Think of the tree as composed of perfectly balanced binary tree subgraphs
    * The size and number of these required is just the binary representation of the # of leaves
    * Then just calculate the number of delays required across different trees to combine them
    * E.g.
    *   8 inputs => perfectly balanced binary tree, no delay paths
    *   9 inputs => 1 path of length 3
    *   85 inputs => 3 paths with lengths 2, 1, and 1
    **/
  def reductionTreeDelays(nLeaves: Int): List[Long] = {
    val binary = Array.tabulate(16) { i => (nLeaves & (1 << i)) > 0 }
    val partialTrees = binary.zipWithIndex.filter(_._1).map(_._2)
    var leftHeight = partialTrees.head
    var i = 1
    var dlys: List[Long] = Nil
    while (i < partialTrees.length) {
      val rightHeight = partialTrees(i)
      if (rightHeight > leftHeight) dlys ::= (rightHeight - leftHeight).toLong
      leftHeight = rightHeight + 1
      i += 1
    }
    dlys
  }

  def crossJoin[T](list: Iterable[Iterable[T]]): Iterable[Iterable[T]] =
    list match {
      case xs :: Nil => xs map (Iterable(_))
      case x :: xs => for {
        i <- x
        j <- crossJoin(xs)
      } yield Iterable(i) ++ j
    }

  def roundUpToPow2(target: Int): Int = {
    var candidate: Int = 1
    while (candidate < target) {
      candidate <<= 1
    }
    candidate
  }

  def computeShifts(parFactors: Iterable[Int]) = {
    (spatial.util.crossJoin((parFactors map { f => Range(0, f).toList }).toList) map {
      _.toList
    }).toList
  }
}
