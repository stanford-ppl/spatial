package spatial.node

import argon._
import argon.node._
import forge.tags._
import spatial.lang._

/** Memory accesses */
abstract class Access {
  def mem:  Sym[_]
  def addr: Seq[Idx]
  def ens:  Set[Bit]
}

case class Read(mem: Sym[_], addr: Seq[Idx], ens: Set[Bit]) extends Access
case class Write(mem: Sym[_], data: Sym[_], addr: Seq[Idx], ens: Set[Bit]) extends Access
case class RMW(mem: Sym[_], data: Sym[_], addr: Seq[Idx], op: String, order: String, ens: Set[Bit]) extends Access

/** Status read of a memory */
abstract class StatusReader[R:Bits] extends EnPrimitive[R] {
  override val R: Bits[R] = Bits[R]
  def mem: Sym[_]
}
object StatusReader {
  def unapply(x: Op[_]): Option[(Sym[_],Set[Bit])] = x match {
    case a: StatusReader[_] => Some((a.mem,a.ens))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Set[Bit])] = x.op.flatMap(StatusReader.unapply)
}

/** Reset of a memory */
abstract class Resetter[A:Type] extends EnPrimitive[Void] {
  override def effects: Effects = Effects.Writes(mem)
  val tA: Type[A] = Type[A]
  def mem: Sym[_]
}
object Resetter {
  def unapply(x: Op[_]): Option[(Sym[_],Set[Bit])] = x match {
    case a: Resetter[_] => Some((a.mem,a.ens))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Set[Bit])] = x.op.flatMap(Resetter.unapply)
}

/** Any access of a memory */
abstract class Accessor[A:Bits,R:Type] extends EnPrimitive[R] {
  val A: Bits[A] = Bits[A]
  def mem:  Sym[_]
  def addr: Seq[Idx]
  def dataOpt: Option[Sym[_]] = localWrite.map(_.data)
  def localRead: Option[Read]
  def localWrite: Option[Write]
  def localAccesses: Set[Access] = (localRead ++ localWrite).toSet
}

object Accessor {
  def unapply(x: Op[_]): Option[(Option[Write],Option[Read])] = x match {
    case a: Accessor[_,_] if a.localWrite.nonEmpty || a.localRead.nonEmpty =>
      Some((a.localWrite,a.localRead))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Option[Write],Option[Read])] = x.op.flatMap(Accessor.unapply)
}


/** Any read of a memory */
abstract class Reader[A:Bits,R:Bits] extends Accessor[A,R] {
  def localRead = Some(Read(mem,addr,ens))
  def localWrite: Option[Write] = None
}

object Reader {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Accessor[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Reader.unapply)
}


/** Any dequeue-like operation from a memory */
abstract class DequeuerLike[A:Bits,R:Bits] extends Reader[A,R] {
  override def effects: Effects = Effects.Writes(mem)
}

object DequeuerLike {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: DequeuerLike[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(DequeuerLike.unapply)
}

/** An address-less dequeue operation. */
abstract class Dequeuer[A:Bits,R:Bits] extends DequeuerLike[A,R] {
  def addr: Seq[Idx] = Nil
}

object Dequeuer {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Dequeuer[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Dequeuer.unapply)
}

abstract class VectorDequeuer[A:Bits](implicit VA: Vec[A]) extends Dequeuer[A,Vec[A]]

object VectorDequeuer {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: VectorDequeuer[_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(VectorDequeuer.unapply)
}



/** Any write to a memory */
abstract class Writer[A:Bits] extends Accessor[A,Void] {
  override def effects: Effects = Effects.Writes(mem)

  def data: Sym[_]
  def localRead: Option[Read] = None
  def localWrite = Some(Write(mem,data,addr,ens))
}

object Writer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Accessor[_,_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Writer.unapply)
}

/** Any enqueue-like operation to a memory */
abstract class EnqueuerLike[A:Bits] extends Writer[A]

/** An address-less enqueue operation. */
abstract class Enqueuer[A:Bits] extends EnqueuerLike[A] {
  def addr: Seq[Idx] = Nil
}

object Enqueuer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Enqueuer[_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Enqueuer.unapply)
}

abstract class VectorEnqueuer[A:Bits] extends Enqueuer[A]

object VectorEnqueuer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: VectorEnqueuer[_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(VectorEnqueuer.unapply)
}

/** Any access of a memory */
abstract class TokenAccessor[A:Bits,R:Type] extends Accessor[A,R] {
  def op: String
  def order: String
  def localRMW: Option[RMW]
  def token: Option[Token]
  override def localAccesses: Set[Access] = (localRead ++ localWrite ++ localRMW).toSet
}

object TokenAccessor {
  def unapply(x: Op[_]): Option[(Option[Write],Option[Read],Option[RMW])] = x match {
    case a: TokenAccessor[_,_] if a.localWrite.nonEmpty || a.localRead.nonEmpty || a.localRMW.nonEmpty =>
      Some((a.localWrite,a.localRead,a.localRMW))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Option[Write],Option[Read],Option[RMW])] = x.op.flatMap(TokenAccessor.unapply)
}

/** Any read of a memory */
abstract class TokenReader[A:Bits,R:Bits] extends TokenAccessor[A,R] {
  def localRead = Some(Read(mem,addr,ens))
  def localWrite: Option[Write] = None
  def localRMW: Option[RMW] = None
  def op = ""
  def order = ""
}

object TokenReader {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: TokenAccessor[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(TokenReader.unapply)
}

/** Any write to a memory that returns a token */
abstract class TokenWriter[A:Bits] extends TokenAccessor[A,Token] {
  override def effects: Effects = Effects.Writes(mem)

  def data: Sym[_]
  def localRead: Option[Read] = None
  def localWrite = Some(Write(mem,data,addr,ens))
  def localRMW: Option[RMW] = None
  def op = ""
  def order = ""
  def token: Option[Token] = None
}

object TokenWriter {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: TokenAccessor[_,_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(TokenWriter.unapply)
}

/** Any read of a memory */
abstract class RMWDoer[A:Bits,R:Bits] extends TokenAccessor[A,R] {
  override def effects: Effects = Effects.Writes(mem)
  def data: Sym[_]
  def localRead: Option[Read] = None
  def localWrite: Option[Write] = None
  def localRMW = Some(RMW(mem,data,addr,op,order,ens))
  override def dataOpt: Option[Sym[_]] = localRMW.map(_.data)
}

object RMWDoer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[Idx],String,String,Set[Bit])] = x match {
    case a: TokenAccessor[_,_] => a.localRMW.map{rd => (rd.mem,rd.data,rd.addr,rd.op,rd.order,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[Idx],String,String,Set[Bit])] = x.op.flatMap(RMWDoer.unapply)
}





