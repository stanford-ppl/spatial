package spatial.node

import argon._
import spatial.lang._

abstract class BankedAccess {
  def mem:  Sym[_]
  def bank: Seq[Seq[Idx]]
  def ofs:  Seq[Idx]
  def ens:  Seq[Set[Bit]]
}
case class BankedRead(mem: Sym[_], bank: Seq[Seq[Idx]], ofs: Seq[Idx], ens: Seq[Set[Bit]]) extends BankedAccess
case class BankedWrite(mem: Sym[_], data: Seq[Sym[_]], bank: Seq[Seq[Idx]], ofs: Seq[Idx], ens: Seq[Set[Bit]]) extends BankedAccess

/** Banked accessors */
abstract class BankedAccessor[A:Type,R:Type] extends EnPrimitive[R] {
  val A: Type[A] = Type[A]
  def bankedRead: Option[BankedRead]
  def bankedWrite: Option[BankedWrite]
  final var ens: Set[Bit] = Set.empty

  def mem: Sym[_]
  def bank: Seq[Seq[Idx]]
  def ofs: Seq[Idx]
  var enss: Seq[Set[Bit]]
  def width: Int = bank.length

  override def mirrorEn(f: Tx, addEns: Set[Bit]): Op[R] = {
    enss = enss.map{ens => ens ++ addEns}
    this.mirror(f)
  }
  override def updateEn(f: Tx, addEns: Set[Bit]): Unit = {
    enss = enss.map{ens => ens ++ addEns}
    this.update(f)
  }
}

object BankedAccessor {
  def unapply(x: Op[_]): Option[(Option[BankedWrite],Option[BankedRead])] = x match {
    case a: BankedAccessor[_,_] if a.bankedWrite.nonEmpty || a.bankedRead.nonEmpty =>
      Some((a.bankedWrite,a.bankedRead))
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Option[BankedWrite],Option[BankedRead])] = x.op.flatMap(BankedAccessor.unapply)
}

abstract class BankedReader[A:Bits](implicit vT: Type[Vec[A]]) extends BankedAccessor[A,Vec[A]] {
  def bankedRead = Some(BankedRead(mem,bank,ofs,enss))
  def bankedWrite: Option[BankedWrite] = None
}

object BankedReader {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Seq[Idx]],Seq[Idx],Seq[Set[Bit]])] = x match {
    case a: BankedAccessor[_,_] => a.bankedRead.map{rd => (rd.mem,rd.bank,rd.ofs,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Seq[Idx]],Seq[Idx],Seq[Set[Bit]])] = x.op.flatMap(BankedReader.unapply)
}

abstract class BankedDequeue[A:Bits](implicit vT: Type[Vec[A]]) extends BankedReader[A] {
  override def effects: Effects = Effects.Writes(mem)
  def bank: Seq[Seq[Idx]] = Nil
  def ofs: Seq[Idx] = Nil
}

abstract class BankedWriter[A:Type] extends BankedAccessor[A,Void] {
  override def effects: Effects = Effects.Writes(mem)
  def data: Seq[Sym[_]]
  def bankedRead: Option[BankedRead] = None
  def bankedWrite = Some(BankedWrite(mem,data,bank,ofs,enss))
}

object BankedWriter {
  def unapply(x: Op[_]): Option[(Sym[_],Seq[Sym[_]],Seq[Seq[Idx]],Seq[Idx],Seq[Set[Bit]])] = x match {
    case a: BankedAccessor[_,_] => a.bankedWrite.map{rd => (rd.mem,rd.data,rd.bank,rd.ofs,rd.ens) }
    case _ => None
  }
  def unapply(x: Sym[_]): Option[(Sym[_],Seq[Sym[_]],Seq[Seq[Idx]],Seq[Idx],Seq[Set[Bit]])] = x.op.flatMap(BankedWriter.unapply)
}

abstract class BankedEnqueue[A:Type] extends BankedWriter[A] {
  def bank: Seq[Seq[Idx]] = Nil
  def ofs: Seq[Idx] = Nil
}