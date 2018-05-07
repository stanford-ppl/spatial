package spatial.data

import argon._
import forge.tags._
import spatial.lang._
import spatial.node._
import spatial.util._

sealed abstract class Ctrl {
  def s: Option[Sym[_]] = None
  def id: Int

  def parent: Ctrl
  @stateful def children: Seq[Controller]

  def isOuterBlock: Boolean
}

case class Controller(sym: Sym[_], id: Int) extends Ctrl {
  override def s: Option[Sym[_]] = Some(sym)
  def parent: Ctrl = if (id != -1) Controller(sym,-1) else sym.parent
  @stateful def children: Seq[Controller] = {
    if (id == -1) sym match {
      case Op(ctrl: Control[_]) => ctrl.bodies.indices.map{i => Controller(sym,i) }
      case _ => Seq(Controller(sym, 0))
    }
    else sym.children.filter(_.parent == this)
  }

  def isOuterBlock: Boolean = id == -1 || (sym match {
    case Op(ctrl: Control[_]) => ctrl.mayBeOuterBlock(id)
    case _ => true
  })
}

case object Host extends Ctrl {
  def id: Int = 0
  def parent: Ctrl = Host
  @stateful def children: Seq[Controller] = hwScopes.all

  def isOuterBlock: Boolean = true
}