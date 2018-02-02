package pcc.node

import forge._
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
sealed abstract class AccelOp[T:Sym] extends Op[T] {
  // If true, this node is only supported in debug mode, not implementation
  val debugOnly: Boolean = false
}

/** Memory allocation **/
abstract class Alloc[T:Sym] extends AccelOp[T]
abstract class Memory[T:Sym] extends Alloc[T]

/** Memory accesses **/
abstract class Access {
  def mem:  Sym[_]
  def addr: Seq[I32]
  def ens:  Seq[Bit]
}
case class Read(mem: Sym[_], addr: Seq[I32], ens: Seq[Bit]) extends Access
case class Write(mem: Sym[_], data: Sym[_], addr: Seq[I32], ens: Seq[Bit]) extends Access


/** Nodes with implicit control signals/logic with internal state **/
abstract class Control extends AccelOp[Void] {
  def cchains: Iterable[CounterChain] = getCChains(inputs)
  def iters: Seq[I32]
}

abstract class EnabledControl extends Control {
  def ens: Seq[Bit]
}

/** Nodes with bodies which execute at least once **/
abstract class Pipeline extends EnabledControl

/** Nodes with bodies which execute multiple times **/
abstract class Loop extends Pipeline {
  def iters: Seq[I32]
  def bodies: Seq[(Seq[I32],Seq[Block[_]])]
}

/** Nodes with non-zero latency, no internal state, which can be conditionally executed **/
abstract class Primitive[A:Sym] extends AccelOp[A] {
  val isStateless: Boolean = false
}

abstract class EnPrimitive[A:Sym] extends Primitive[A] {
  def ens: Seq[Bit]
}

abstract class Accessor[A:Sym,R:Sym] extends EnPrimitive[R] {
  val tA: Sym[A] = typ[A]

  def localRead: Option[Read]
  def localWrite: Option[Write]
  def localAccesses: Set[Access] = (localRead ++ localWrite).toSet
}

abstract class Reader[A:Sym,R:Sym](
  mem: Sym[_],
  adr: Seq[I32],
  ens: Seq[Bit]
) extends Accessor[A,R] {
  def localRead = Some(Read(mem,adr,ens))
  def localWrite = None
}

abstract class Writer[A:Sym](
  mem: Sym[_],
  dat: Sym[_],
  adr: Seq[I32],
  en:  Seq[Bit]
) extends Accessor[A,Void] {
  override def effects = Effects.Writes(mem)
  def localRead = None
  def localWrite = Some(Write(mem,dat,adr,ens))
}

object Accessor {
  def unapply(x: Op[_]): Option[(Option[Write],Option[Read])] = x match {
    case a: Accessor[_,_] if a.localWrite.nonEmpty || a.localRead.nonEmpty => Some((a.localWrite,a.localRead))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Option[Write],Option[Read])] = x.op.flatMap(Accessor.unapply)
}

object Writer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[I32],Seq[Bit])] = x match {
    case a: Accessor[_,_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[I32],Seq[Bit])] = x.op.flatMap(Writer.unapply)
}
object Reader {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[I32],Seq[Bit])] = x match {
    case a: Accessor[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[I32],Seq[Bit])] = x.op.flatMap(Reader.unapply)
}


object Alloc {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: Alloc[_]) => Some(x)
    case _ => None
  }
}
object Memory {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: Memory[_]) => Some(x)
    case _ => None
  }
}
object Primitive {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_:Primitive[_]) => Some(x)
    case _ => None
  }
}
object Stateless {
  @stateful def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(p:Primitive[_]) if p.isStateless => Some(x)
    case Expect(c) => Some(x)
    case _ => None
  }
}
object Control {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_:Control) => Some(x)
    case _ => None
  }
}
