package spatial.node

import argon._
import spatial.lang._

abstract class UnrolledAccess {
  def mem:  Sym[_]
  def ens:  Seq[Set[Bit]]
}
abstract class UnrolledRead extends UnrolledAccess
abstract class UnrolledWrite extends UnrolledAccess {
  def data: Seq[Sym[_]]
}

case class BankedRead(mem: Sym[_], bank: Seq[Seq[Idx]], ofs: Seq[Idx], ens: Seq[Set[Bit]]) extends UnrolledRead
case class BankedWrite(mem: Sym[_], data: Seq[Sym[_]], bank: Seq[Seq[Idx]], ofs: Seq[Idx], ens: Seq[Set[Bit]]) extends UnrolledWrite

case class VectorRead(mem: Sym[_], addr: Seq[Seq[Idx]], ens: Seq[Set[Bit]]) extends UnrolledRead
case class VectorWrite(mem: Sym[_], data: Seq[Sym[_]], addr: Seq[Seq[Idx]], ens: Seq[Set[Bit]]) extends UnrolledWrite

abstract class UnrolledAccessor[A:Type,R:Type] extends EnPrimitive[R] {
  val A: Type[A] = Type[A]
  def mem: Sym[_]
  def bankedRead: Option[UnrolledRead]
  def bankedWrite: Option[UnrolledWrite]
  final var ens: Set[Bit] = Set.empty
  var enss: Seq[Set[Bit]]

  def width: Int = enss.length

  override def mirrorEn(f: Tx, addEns: Set[Bit]): Op[R] = {
    enss = enss.map{ens => ens ++ addEns}
    this.mirror(f)
  }
  override def updateEn(f: Tx, addEns: Set[Bit]): Unit = {
    enss = enss.map{ens => ens ++ addEns}
    this.update(f)
  }
}

/** Banked accessors */
abstract class BankedAccessor[A:Type,R:Type] extends UnrolledAccessor[A,R] {
  def bank: Seq[Seq[Idx]]
  def ofs: Seq[Idx]
}

object UnrolledAccessor {
  def unapply(x: Op[_]): Option[(Option[UnrolledWrite],Option[UnrolledRead])] = x match {
    case a: UnrolledAccessor[_,_] if a.bankedWrite.nonEmpty || a.bankedRead.nonEmpty =>
      Some((a.bankedWrite,a.bankedRead))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Option[UnrolledWrite],Option[UnrolledRead])] = x.op.flatMap(UnrolledAccessor.unapply)
}

abstract class VectorReader[A:Bits](implicit vT: Type[Vec[A]]) extends UnrolledAccessor[A,Vec[A]] {
  def addr: Seq[Seq[Idx]]
  def bankedRead = Some(VectorRead(mem,addr,enss))
  def bankedWrite: Option[UnrolledWrite] = None
}

abstract class BankedReader[A:Bits](implicit vT: Type[Vec[A]]) extends BankedAccessor[A,Vec[A]] {
  def bankedRead = Some(BankedRead(mem,bank,ofs,enss))
  def bankedWrite: Option[UnrolledWrite] = None
}

abstract class BankedDequeue[A:Bits](implicit vT: Type[Vec[A]]) extends BankedReader[A] {
  override def effects: Effects = Effects.Writes(mem)
  def bank: Seq[Seq[Idx]] = Nil
  def ofs: Seq[Idx] = Nil
}

abstract class VectorWriter[A:Bits] extends UnrolledAccessor[A,Void] {
  override def effects: Effects = Effects.Writes(mem)
  def data: Seq[Sym[_]]
  def addr: Seq[Seq[Idx]]
  def bankedRead: Option[UnrolledRead] = None
  def bankedWrite = Some(VectorWrite(mem,data,addr,enss))
}

abstract class BankedWriter[A:Type] extends BankedAccessor[A,Void] {
  override def effects: Effects = Effects.Writes(mem)
  def data: Seq[Sym[_]]
  def bankedRead: Option[UnrolledRead] = None
  def bankedWrite = Some(BankedWrite(mem,data,bank,ofs,enss))
}

abstract class BankedEnqueue[A:Type] extends BankedWriter[A] {
  def bank: Seq[Seq[Idx]] = Nil
  def ofs: Seq[Idx] = Nil
}