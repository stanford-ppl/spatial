package spatial.node

import core._
import spatial.lang._

/** Banked accessors */
abstract class BankedAccessor[A:Type,R:Type] extends EnPrimitive[R] {
  val tA: Type[A] = Type[A]
  def bankedRead: Option[BankedRead]
  def bankedWrite: Option[BankedWrite]
  final var ens: Set[Bit] = Set.empty

  def mem: Sym[_]
  def bank: Seq[Seq[Idx]]
  def ofs: Seq[Idx]
  var enss: Seq[Set[Bit]]

  override def mirrorEn(f: Tx, addEns: Set[Bit]): Op[R] = {
    enss = enss.map{ens => ens ++ addEns}
    this.mirror(f)
  }
  override def updateEn(f: Tx, addEns: Set[Bit]): Unit = {
    enss = enss.map{ens => ens ++ addEns}
    this.update(f)
  }
}

abstract class BankedReader[T:Type](implicit vT: Type[Vec[T]]) extends BankedAccessor[T,Vec[T]] {
  def bankedRead = Some(BankedRead(mem,bank,ofs,enss))
  def bankedWrite: Option[BankedWrite] = None
}

abstract class BankedDequeue[T:Type](implicit vT: Type[Vec[T]]) extends BankedReader[T] {
  override def effects: Effects = Effects.Writes(mem)
  def bank: Seq[Seq[Idx]] = Nil
  def ofs: Seq[Idx] = Nil
}


abstract class BankedWriter[T:Type] extends BankedAccessor[T,Void] {
  override def effects: Effects = Effects.Writes(mem)
  def data: Seq[Sym[_]]
  def bankedRead: Option[BankedRead] = None
  def bankedWrite = Some(BankedWrite(mem,data,bank,ofs,enss))
}

abstract class BankedEnqueue[T:Type] extends BankedWriter[T] {
  def bank: Seq[Seq[Idx]] = Nil
  def ofs: Seq[Idx] = Nil
}