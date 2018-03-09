package spatial.node

import core._
import spatial.lang._

/** An operation supported for acceleration */
abstract class AccelOp[R:Type] extends Op[R] {
  /** If true, this node is only supported in host/debug mode, not hardware accel. */
  val debugOnly: Boolean = false
}

/** Nodes with implicit control signals/logic with internal state */
abstract class Control[R:Type] extends AccelOp[R] {
  def iters: Seq[I32]
  def cchains: Seq[(CounterChain, Seq[I32])]
  def bodies: Seq[(Seq[I32],Seq[Block[_]])]
}
object Control {
  def unapply[R](x: Sym[R]): Option[Sym[R]] = x match {
    case Op(_:Control[_]) => Some(x)
    case _ => None
  }
}

/** Control nodes which take explicit enable signals. */
abstract class EnControl[R:Type] extends Control[R] with Enabled[R]

/** Black box nodes which represent transfers between memories. */
abstract class MemTransfer extends EnControl[Void] {
  def cchains = Nil
  def bodies = Nil
}

/** Nodes with bodies which execute at least once. */
abstract class Pipeline[R:Type] extends EnControl[R]

/** Nodes with bodies which execute multiple times. */
abstract class Loop[R:Type] extends Pipeline[R]

