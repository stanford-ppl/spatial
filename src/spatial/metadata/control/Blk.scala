package spatial.metadata.control

import argon._
import spatial.metadata.SpatialMetadata

/** IR hierarchy: References a linearized block from some parent controller. Raw scoping rules.
  *
  * A Blk defines the exact location in the IR in which a symbol is defined. This is independent
  * of the control hierarchy, which may or may not match the structure of blocks in the IR.
  */
sealed abstract class Blk(val s: Option[Sym[_]], val block: Int) extends SpatialMetadata 
object Blk {
  case class Node(sym: Sym[_], blk: Int) extends Blk(Some(sym), blk) {
    override def toString: String = s"$sym (block: $blk)"
  }
  case object Host extends Blk(None, 0)
}
