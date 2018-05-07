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
    val pathX = ancestors(x)(parent)
    val pathY = ancestors(y)(parent)

    // Choose last node where paths are the same
    val lca = pathX.zip(pathY).reverse.find{case (a,b) => a == b}.map(_._1)
                              .getOrElse{throw new Exception(s"""No common parent for $x (${parent(x)}) and $y (${parent(y)})
                                                                 |X ancestors: ${pathX.mkString(", ")}
                                                                 |Y ancestors: ${pathY.mkString(", ")}""".stripMargin) }
    val pathToX = pathX.drop(pathX.indexOf(lca))
    val pathToY = pathY.drop(pathY.indexOf(lca))
    (lca,pathToX,pathToY)
  }

  def LCA[T](x: T, y: T)(parent: T => T): T = LCAWithPaths(x,y)(parent)._1

}
