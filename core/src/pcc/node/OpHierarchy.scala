package pcc.node

import pcc.core._
import pcc.data._
import pcc.lang._

/**
  * Design principle here is to force nodes used in the Accel scope to specify
  * their node class explicitly. This goes a long way towards being able to
  * handle arbitrary graphs in a principled manner.
  * Could also handle this with trait mix-ins
  */

/** An operation supported for acceleration **/
sealed abstract class AccelOp[T:Sym] extends Op[T]

/** Memory allocation **/
abstract class Alloc[T:Sym] extends AccelOp[T]

/** Memory accesses **/
abstract class Access {
  def mem:  Sym[_]
  def addr: Option[Seq[I32]]
  def ens:  Seq[Bit]
}
case class Read(mem: Sym[_], addr: Option[Seq[I32]], ens: Seq[Bit]) extends Access
case class Write(mem: Sym[_], data: Sym[_], addr: Option[Seq[I32]], ens: Seq[Bit]) extends Access


/** Nodes with implicit control signals/logic with internal state **/
abstract class Control extends AccelOp[Void]

abstract class EnabledControl extends Control {
  def ens: Seq[Bit]
}

/** Nodes with bodies which execute at least once **/
abstract class Pipeline extends EnabledControl

/** Nodes with bodies which execute multiple times **/
abstract class Loop extends Pipeline

/** Nodes with non-zero latency, no internal state, which can be conditionally executed **/
abstract class Primitive[A:Sym] extends AccelOp[A] {
  val isStateless: Boolean = false
}
abstract class EnPrimitive[A:Sym] extends Primitive[A] {
  def ens: Seq[Bit]
}

abstract class Accessor[A:Sym,R:Sym] extends EnPrimitive[R] {
  val tA: Sym[A] = tp[A]

  def localReads: Seq[Read]
  def localWrites: Seq[Write]
  def localAccesses: Seq[Access] = localReads ++ localWrites
}

abstract class Reader[A:Sym,R:Sym](
  mem: Sym[_],
  adr: Option[Seq[I32]],
  ens: Seq[Bit]
) extends Accessor[A,R] {
  def localReads = Seq(Read(mem,adr,ens))
  def localWrites = Nil
}

object Reader {
  def unapply(x: Sym[_]): Option[Seq[Read]] = x match {
    case Op(a: Accessor[_,_]) if a.localReads.nonEmpty => Some(a.localReads)
    case _ => None
  }
}

abstract class Writer[A:Sym](
  mem: Sym[_],
  dat: Sym[_],
  adr: Option[Seq[I32]],
  en:  Seq[Bit]
) extends Accessor[A,Void] {
  override def effects = Effects.Writes(mem)
  def localReads = Nil
  def localWrites = Seq(Write(mem,dat,adr,ens))
}

object Writer {
  def unapply(x: Sym[_]): Option[Seq[Write]] = x match {
    case Op(a: Accessor[_,_]) if a.localWrites.nonEmpty => Some(a.localWrites)
    case _ => None
  }
}