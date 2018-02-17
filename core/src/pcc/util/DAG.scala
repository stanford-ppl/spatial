package pcc.util

import scala.collection.mutable.ListBuffer

object DAG {
  /**
   * Returns all ancestors of the given node x (inclusive) to optional stop (inclusive)
   * Ancestors are ordered outermost to innermost
   */
  def ancestors[T](x: T, stop: Option[T] = None)(parent: T => Option[T]): Seq[T] = {
    val parents = ListBuffer.empty[T]
    var current: Option[T] = Some(x)
    while (current.isDefined && current != stop) {
      parents.prepend(current.get)
      current = parent(current.get)
    }
    parents
  }

  /**
    * Returns the least common ancestor (LCA) of two nodes in some directed, acyclic graph.
    * If the nodes share no common parent at any point in the tree, the LCA is undefined (None).
    * Also returns the paths from the least common ancestor to each node.
    */
  def LCAWithPaths[T](x: T, y: T)(parent: T => Option[T]): (Option[T], Seq[T], Seq[T]) = {
    val pathX = ancestors(x)(parent)
    val pathY = ancestors(y)(parent)

    // Choose last node where paths are the same
    val lca = pathX.zip(pathY).filter{case (a,b) => a == b}.lastOption.map(_._1)
    val pathToX = pathX.drop(lca.map{pathX.indexOf}.getOrElse(0) + 1)
    val pathToY = pathY.drop(lca.map{pathY.indexOf}.getOrElse(0) + 1)
    (lca,pathToX,pathToY)
  }

  def LCA[T](x: T, y: T)(parent: T => Option[T]): Option[T] = LCAWithPaths(x,y)(parent)._1

}
