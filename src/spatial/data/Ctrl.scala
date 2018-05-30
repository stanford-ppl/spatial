package spatial.data

import argon._
import forge.tags._
import spatial.lang._
import spatial.node._
import spatial.util._

sealed abstract class Ctrl {
  def s: Option[Sym[_]] = None
  def id: Int

  def master: Ctrl

  def isOuterBlock: Boolean
}

case class Controller(sym: Sym[_], id: Int) extends Ctrl {
  override def s: Option[Sym[_]] = Some(sym)
  def master: Ctrl = Controller(sym, -1)

  def isOuterBlock: Boolean = id == -1 || (sym match {
    case Op(ctrl: Control[_]) => ctrl.bodies(id).mayBeOuterStage
    case _ => true
  })
}

case object Host extends Ctrl {
  def id: Int = 0
  def master: Ctrl = Host

  def isOuterBlock: Boolean = true
}