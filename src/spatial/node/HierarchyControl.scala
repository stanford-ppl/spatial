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

/** Nodes with implicit control signals/logic with internal state */
abstract class Control[R:Type] extends AccelOp[R] {
  override def inputs: Seq[Sym[_]] = super.inputs diff iters
  override def binds: Seq[Sym[_]] = super.binds ++ iters
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


/** Unrolled loops */
abstract class UnrolledLoop[R:Type] extends Pipeline[R] {
  def iterss: Seq[Seq[I32]]
  def validss: Seq[Seq[Bit]]
  def cchainss: Seq[(CounterChain, Seq[Seq[I32]])]
  def bodiess: Seq[(Seq[Seq[I32]], Seq[Block[_]])]
  final override def iters: Seq[I32] = iterss.flatten
  override def binds: Seq[Sym[_]] = super.binds ++ validss.flatten
  final override def cchains = cchainss.map{case (ctr,itrss) => ctr -> itrss.flatten }
  final override def bodies = bodiess.map{case (itrss, blocks) => itrss.flatten -> blocks }
}
