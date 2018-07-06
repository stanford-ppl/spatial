package spatial.metadata.control

import argon._
import spatial.node._

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