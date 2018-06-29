package spatial.data

import argon._
import forge.tags._
import spatial.lang._
import spatial.node._
import spatial.util._

/** IR hierarchy: References a linearized block from some parent controller. Raw scoping rules.
  *
  * A Blk defines the exact location in the IR in which a symbol is defined. This is independent
  * of the control hierarchy, which may or may not match the structure of blocks in the IR.
  */
sealed abstract class Blk(val s: Option[Sym[_]], val block: Int)
object Blk {
  case class Node(sym: Sym[_], blk: Int) extends Blk(Some(sym), blk) {
    override def toString: String = s"$sym (block: $blk)"
  }
  case object Host extends Blk(None, 0)
}

/** Scope hierarchy: References the exact stage and block for a statement in a controller in Accel.
  *
  * This tracks the exact stage (pseudo or otherwise) and block within that stage for each statement.
  *
  * This is most useful for determining which iterators are defined over a given scope.
  */
sealed abstract class Scope(val s: Option[Sym[_]], val stage: Int, val block: Int) {
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

/** Control hierarchy: References the stage for a statement in a controller in Accel.
  *
  * This keeps details about how the graph will look after unrolling, in terms of actual control
  * stages that will be implemented in hardware.
  *
  * Most useful for determining concurrency and buffer depths.
  */
sealed abstract class Ctrl(val s: Option[Sym[_]], val stage: Int) {
  def master: Ctrl
  def mayBeOuterBlock: Boolean
}
object Ctrl {
  case class Node(sym: Sym[_], stg: Int) extends Ctrl(Some(sym), stg) {
    def master: Ctrl = Node(sym, -1)
    def mayBeOuterBlock: Boolean = stage == -1 || (sym match {
      case Op(ctrl: Control[_]) => ctrl.bodies(stage).mayBeOuterStage
      case _ => true
    })

    override def toString: String = s"$sym (stage: $stg)"
  }
  case object Host extends Ctrl(None, 0) {
    def master: Ctrl = Host
    def mayBeOuterBlock: Boolean = true
  }
}