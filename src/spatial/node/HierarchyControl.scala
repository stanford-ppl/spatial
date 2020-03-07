package spatial.node

import argon._
import argon.node._
import spatial.lang._

abstract class FringeNode[A:Bits,R:Type] extends DSLOp[R] {
  val A: Bits[A] = Bits[A]
}
object FringeNode {
  def unapply[R](x: Sym[R]): Option[Sym[R]] = x match {
    case Op(_:FringeNode[_,_]) => Some(x)
    case _ => None
  }
}

sealed abstract class ControlBody {
  /** All blocks that are part of this stage along with their corresponding iterators. */
  def blocks: Seq[(Seq[ICTR], Block[_])]
  /** True if this body does not represent a true or future stage. */
  def isPseudoStage: Boolean
  /** True if this stage may represent an outer controller. */
  def mayBeOuterStage: Boolean
  /** True if this is an inner stage regardless of outer controller. */
  def isInnerStage: Boolean
}
case class PseudoStage(blks: (Seq[ICTR], Block[_])*) extends ControlBody {
  override def blocks: Seq[(Seq[ICTR], Block[_])] = blks.toSeq
  override def isPseudoStage: Boolean = true
  override def mayBeOuterStage: Boolean = true
  override def isInnerStage: Boolean = false
}
case class OuterStage(blks: (Seq[ICTR], Block[_])*) extends ControlBody {
  override def blocks: Seq[(Seq[ICTR], Block[_])] = blks.toSeq
  override def mayBeOuterStage: Boolean = true
  override def isPseudoStage: Boolean = false
  override def isInnerStage: Boolean = false
}
case class InnerStage(blks: (Seq[ICTR], Block[_])*) extends ControlBody {
  override def blocks: Seq[(Seq[ICTR], Block[_])] = blks.toSeq
  override def mayBeOuterStage: Boolean = false
  override def isPseudoStage: Boolean = false
  override def isInnerStage: Boolean = true
}

/** Nodes with implicit control signals/logic with internal state */
abstract class Control[R:Type] extends DSLOp[R] {
  override def inputs: Seq[Sym[_]] = super.inputs diff iters
  override def binds: Set[Sym[_]] = super.binds ++ iters.toSet
  def iters: Seq[ICTR]
  def cchains: Seq[(CounterChain, Seq[ICTR])]
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
  def iterss: Seq[Seq[ICTR]]
  def validss: Seq[Seq[Bit]]
  def cchainss: Seq[(CounterChain, Seq[Seq[ICTR]])]
  def bodiess: Seq[(Seq[Seq[ICTR]], Seq[Block[_]])]
  final override def iters: Seq[ICTR] = iterss.flatten
  final def valids: Seq[Bit] = validss.flatten
  override def inputs: Seq[Sym[_]] = super.inputs diff (iters ++ valids)
  override def binds: Set[Sym[_]] = super.binds ++ (iters ++ valids).toSet
  final override def cchains = cchainss.map{case (ctr,itrss) => ctr -> itrss.flatten }
  final override def bodies = {
    bodiess.map{case (itrss, blocks) => PseudoStage(blocks.map{blk => itrss.flatten -> blk }:_*) }
  }
}
