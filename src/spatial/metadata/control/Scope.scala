package spatial.metadata.control

import argon._
import spatial.metadata.SpatialMetadata

/** Scope hierarchy: References the exact stage and block for a statement in a controller in Accel.
  *
  * This tracks the exact stage (pseudo or otherwise) and block within that stage for each statement.
  *
  * This is most useful for determining which iterators are defined over a given scope.
  */
sealed abstract class Scope(val s: Option[Sym[_]], val stage: Int, val block: Int) extends SpatialMetadata {
  def master: Scope
}
object Scope {
  case class Node(sym: Sym[_], stg: Int, blk: Int) extends Scope(Some(sym), stg, blk) {
    def master: Scope = Scope.Node(sym, -1, -1)
    override def toString: String = s"$sym (scope: $stg, $blk)"
  }
  case object Host extends Scope(None, 0, 0) {
    def master: Scope = Scope.Host
  }
}