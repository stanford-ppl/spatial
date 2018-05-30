package spatial.node

import argon._
import spatial.lang._

/** An operation supported for acceleration */
abstract class AccelOp[R:Type] extends Op[R] {
  /** If true, this node is only supported in host/debug mode, not hardware accel. */
  val debugOnly: Boolean = false
}

abstract class FringeNode[A:Bits,R:Type] extends AccelOp[R] {
  val A: Bits[A] = Bits[A]
}
object FringeNode {
  def unapply[R](x: Sym[R]): Option[Sym[R]] = x match {
    case Op(_:FringeNode[_,_]) => Some(x)
    case _ => None
  }
}

sealed abstract class ControlBody {
  /** The iterators over this stage. */
  def iters: Seq[I32]
  /** All blocks that are part of this stage. */
  def blocks: Seq[Block[_]]
  /** True if this body does not represent a true or future stage. */
  def isPseudoStage: Boolean
  /** True if this stage may represent an outer controller. */
  def mayBeOuterStage: Boolean
}
case class PseudoStage(iters: Seq[I32], blocks: Seq[Block[_]]) extends ControlBody {
  override def isPseudoStage: Boolean = true
  override def mayBeOuterStage: Boolean = true
}
case class FutureStage(iters: Seq[I32], blocks: Seq[Block[_]], mayBeOuterStage: Boolean) extends ControlBody {
  override def isPseudoStage: Boolean = false
}

/** Nodes with implicit control signals/logic with internal state */
abstract class Control[R:Type] extends AccelOp[R] {
  override def inputs: Seq[Sym[_]] = super.inputs diff iters
  override def binds: Set[Sym[_]] = super.binds ++ iters.toSet
  def iters: Seq[I32]
  def cchains: Seq[(CounterChain, Seq[I32])]
  def bodies: Seq[ControlBody]
}
object Control {
  def unapply[R](x: Sym[R]): Option[Sym[R]] = x match {
    case Op(_:Control[_]) => Some(x)
    case _ => None
  }
}

/** Control nodes which take explicit enable signals. */
abstract class EnControl[R:Type] extends Control[R] with Enabled[R]

/** Nodes with bodies which execute at least once. */
abstract class Pipeline[R:Type] extends EnControl[R]

/** Nodes with bodies which execute multiple times. */
abstract class Loop[R:Type] extends Pipeline[R]


/** Unrolled loops */
abstract class UnrolledLoop[R:Type] extends Loop[R] {
  def iterss: Seq[Seq[I32]]
  def validss: Seq[Seq[Bit]]
  def cchainss: Seq[(CounterChain, Seq[Seq[I32]])]
  def bodiess: Seq[(Seq[Seq[I32]], Seq[Block[_]])]
  final override def iters: Seq[I32] = iterss.flatten
  final def valids: Seq[Bit] = validss.flatten
  override def inputs: Seq[Sym[_]] = super.inputs diff (iters ++ valids)
  override def binds: Set[Sym[_]] = super.binds ++ (iters ++ valids).toSet
  final override def cchains = cchainss.map{case (ctr,itrss) => ctr -> itrss.flatten }
  final override def bodies = {
    bodiess.map{case (itrss, blocks) => PseudoStage(itrss.flatten, blocks) }
  }
}
