package nova.node

import nova.core._
import nova.lang._

/** An operation supported for acceleration **/
abstract class AccelOp[R:Type] extends Op[R] {
  // If true, this node is only supported in debug mode, not implementation
  val debugOnly: Boolean = false
}

/** Nodes with implicit control signals/logic with internal state **/
abstract class Control extends AccelOp[Void] {
  def iters: Seq[I32]
  def cchains: Seq[(CounterChain, Seq[I32])]
  def bodies: Seq[(Seq[I32],Seq[Block[_]])]
}
object Control {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_:Control) => Some(x)
    case _ => None
  }
}

abstract class EnabledControl extends Control {
  def ens: Seq[Bit]
}

/** Nodes with bodies which execute at least once **/
abstract class Pipeline extends EnabledControl

/** Nodes with bodies which execute multiple times **/
abstract class Loop extends Pipeline




