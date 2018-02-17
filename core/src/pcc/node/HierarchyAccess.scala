package pcc.node

import forge._
import pcc.core._
import pcc.data._
import pcc.lang._

/** Memory accesses **/
abstract class Access {
  def mem:  Sym[_]
  def addr: Seq[I32]
  def ens:  Seq[Bit]
}
case class Read(mem: Sym[_], addr: Seq[I32], ens: Seq[Bit]) extends Access
case class Write(mem: Sym[_], data: Sym[_], addr: Seq[I32], ens: Seq[Bit]) extends Access


/** Any access of a memory **/
abstract class Accessor[A:Type,R:Type] extends EnPrimitive[R] {
  val tA: Type[A] = typ[A]

  def localRead: Option[Read]
  def localWrite: Option[Write]
  def localAccesses: Set[Access] = (localRead ++ localWrite).toSet
}

object Accessor {
  def unapply(x: Op[_]): Option[(Option[Write],Option[Read])] = x match {
    case a: Accessor[_,_] if a.localWrite.nonEmpty || a.localRead.nonEmpty => Some((a.localWrite,a.localRead))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Option[Write],Option[Read])] = x.op.flatMap(Accessor.unapply)
}

/** Any read of a memory **/
abstract class Reader[A:Type,R:Type](
  mem: Sym[_],
  adr: Seq[I32],
  ens: Seq[Bit]
) extends Accessor[A,R] {
  def localRead = Some(Read(mem,adr,ens))
  def localWrite = None
}

object Reader {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[I32],Seq[Bit])] = x match {
    case a: Accessor[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[I32],Seq[Bit])] = x.op.flatMap(Reader.unapply)
}

/** Any dequeue-like operation from a memory **/
abstract class Dequeuer[A:Type,R:Type](
  mem: Sym[_],
  adr: Seq[I32],
  ens: Seq[Bit]
) extends Reader[A,R](mem,adr,ens)

object Dequeuer {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[I32],Seq[Bit])] = x match {
    case a: Dequeuer[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[I32],Seq[Bit])] = x.op.flatMap(Dequeuer.unapply)
}


/** Any write to a memory **/
abstract class Writer[A:Type](
  mem: Sym[_],
  dat: Sym[_],
  adr: Seq[I32],
  ens: Seq[Bit]
) extends Accessor[A,Void] {
  override def effects = Effects.Writes(mem)
  def localRead = None
  def localWrite = Some(Write(mem,dat,adr,ens))
}

object Writer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[I32],Seq[Bit])] = x match {
    case a: Accessor[_,_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[I32],Seq[Bit])] = x.op.flatMap(Writer.unapply)
}

/** Any enqueue-like operation to a memory **/
abstract class Enqueuer[A:Type](
  mem: Sym[_],
  dat: Sym[_],
  adr: Seq[I32],
  ens: Seq[Bit]
) extends Writer[A](mem,dat,adr,ens)

object Enqueuer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[I32],Seq[Bit])] = x match {
    case a: Enqueuer[_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[I32],Seq[Bit])] = x.op.flatMap(Enqueuer.unapply)
}


