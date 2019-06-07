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

abstract class VecAccess {
  def mem:  Sym[_]
  def addr: Seq[Vec[Idx]]
  def ens:  Set[Bit]
}
case class VecRead(mem: Sym[_], addr: Seq[Vec[Idx]], ens: Set[Bit]) extends VecAccess
case class VecWrite(mem: Sym[_], data: Vec[Sym[_]], addr: Seq[Vec[Idx]], ens: Set[Bit]) extends VecAccess

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


/** Any vector access of a memory */
abstract class VecAccessor[A:Bits,R:Type] extends EnPrimitive[R] {
  val A: Bits[A] = Bits[A]
  def mem:  Sym[_]
  def addr: Seq[Vec[Idx]]
  def dataOpt: Option[Vec[Sym[_]]] = localVecWrite.map(_.data)
  def localVecRead: Option[VecRead]
  def localVecWrite: Option[VecWrite]
  def localVecAccesses: Set[VecAccess] = (localVecRead ++ localVecWrite).toSet
}

object VecAccessor {
  def unapply(x: Op[_]): Option[(Option[VecWrite],Option[VecRead])] = x match {
    case a: VecAccessor[_,_] if a.localVecWrite.nonEmpty || a.localVecRead.nonEmpty =>
      Some((a.localVecWrite,a.localVecRead))
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

/** Any vector read of a memory */
abstract class VecReader[A:Bits](implicit vT: Type[Vec[A]]) extends VecAccessor[A,Vec[A]] {
  def localVecRead = Some(VecRead(mem,addr,ens))
  def localVecWrite: Option[VecWrite] = None
}

object VecReader {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Vec[Idx]],Set[Bit])] = x match {
    case a: VecAccessor[_,_] => a.localVecRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Vec[Idx]],Set[Bit])] = x.op.flatMap(VecReader.unapply)
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

abstract class VectorDequeuer[A:Bits](implicit VA: Vec[A]) extends Dequeuer[A,Vec[A]]

object Dequeuer {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Dequeuer[_,_] => a.localRead.map{rd => (rd.mem,rd.addr,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Dequeuer.unapply)
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

/** Any vector Vecwrite to a memory */
abstract class VecWriter[A:Bits] extends VecAccessor[A,Void] {
  override def effects: Effects = Effects.Writes(mem)

  def data: Vec[Sym[_]]
  def localVecRead: Option[VecRead] = None
  def localVecWrite = Some(VecWrite(mem,data,addr,ens))
}

object VecWriter {
  def unapply(x: Op[_]): Option[(Sym[_],Vec[Sym[_]],Seq[Vec[Idx]],Set[Bit])] = x match {
    case a: VecAccessor[_,_] => a.localVecWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Vec[Sym[_]],Seq[Vec[Idx]],Set[Bit])] = x.op.flatMap(VecWriter.unapply)
}

/** Any enqueue-like operation to a memory */
abstract class EnqueuerLike[A:Bits] extends Writer[A]

/** An address-less enqueue operation. */
abstract class Enqueuer[A:Bits] extends EnqueuerLike[A] {
  def addr: Seq[Idx] = Nil
}

abstract class VectorEnqueuer[A:Bits] extends Enqueuer[A]

object Enqueuer {
  def unapply(x: Op[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x match {
    case a: Enqueuer[_] => a.localWrite.map{wr => (wr.mem,wr.data,wr.addr,wr.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Sym[_],Seq[Idx],Set[Bit])] = x.op.flatMap(Enqueuer.unapply)
}







