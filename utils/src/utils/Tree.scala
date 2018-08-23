package utils

import scala.collection.mutable.ListBuffer

object Tree {
  /** Returns all ancestors of the given node x (inclusive) to optional stop (exclusive)
    * Ancestors are ordered outermost to innermost
    */
  def ancestors[T](x: T, stop: T => Boolean = {_:T => false})(parent: T => T): Seq[T] = {
    val parents = ListBuffer.empty[T]
    var current: T = x
    var continue: Boolean = !stop(current)
    while (continue) {
      parents.prepend(current)
      continue = parent(current) != current && !stop(current)
      current = parent(current)
    }
    parents
  }

  /** Returns the least common ancestor (LCA) of two nodes in some tree.
    * Also returns the paths from the least common ancestor to each node.
    */
  def LCAWithPaths[T](x: T, y: T)(parent: T => T): (T, Seq[T], Seq[T]) = {
    getLCAWithPaths(x,y)(parent).getOrElse{
      throw new Exception(s"""No LCA for $x (${parent(x)}) and $y (${parent(y)})""")
    }
  }

  def getLCAWithPaths[T](x: T, y: T)(parent: T => T): Option[(T, Seq[T], Seq[T])] = {
    val pathX = ancestors(x)(parent)
    val pathY = ancestors(y)(parent)
    val lca = pathX.zip(pathY).reverse.find{case (a,b) => a == b}.map(_._1)

    lca match {
      case Some(ctrl) =>
        // Choose last node where paths are the same
        val pathToX = pathX.drop(pathX.indexOf(ctrl))
        val pathToY = pathY.drop(pathY.indexOf(ctrl))
        Some(ctrl,pathToX,pathToY)

      case None => None
    }
  }

  def LCA[T](x: T, y: T)(parent: T => T): T = LCAWithPaths(x,y)(parent)._1

  def getLCA[T](x: T, y: T)(parent: T => T): Option[T] = getLCAWithPaths(x,y)(parent).map(_._1)

}
