package spatial.node

import argon._
import forge.tags._
import spatial.lang._

@op case class HostIONew[A:Bits](init: Bits[A]) extends MemAlloc[A,HostIO] {
  def dims = Nil
}

@op case class HostIORead[A:Bits](mem: HostIO[A]) extends Reader[A,A] {
  override val isEphemeral: Boolean = true
  override def addr: Seq[Idx] = Nil
  override var ens:  Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}

@op case class HostIOWrite[A:Bits](mem: HostIO[A], data: Bits[A], ens: Set[Bit]) extends Writer[A] {
  override def addr: Seq[Idx] = Nil
}

@op case class SetHostIO[A:Bits](mem: HostIO[A], data: Bits[A]) extends Writer[A] {
  override def addr: Seq[Idx] = Nil
  override var ens:  Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}

@op case class GetHostIO[A:Bits](mem: HostIO[A]) extends Reader[A,A] {
  override def addr: Seq[Idx] = Nil
  override var ens: Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}
