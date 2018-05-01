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

  def isOuterBlk: Boolean
}

case class Controller(sym: Sym[_], id: Int) extends Ctrl {
  override def s: Option[Sym[_]] = Some(sym)
  def parent: Ctrl = if (id != -1) Controller(sym,-1) else sym.parent
  @stateful def children: Seq[Controller] = sym.children

  def isOuterBlk: Boolean = sym.isOuterControl && (id == -1 || (sym match {
    case Op(ctrl: Control[_]) => ctrl.mayBeOuterBlock(id)
    case _ => true
  }))
}

case object Host extends Ctrl {
  def id: Int = 0
  def parent: Ctrl = Host
  @stateful def children: Seq[Controller] = hwScopes.all

  def isOuterBlk: Boolean = true
}