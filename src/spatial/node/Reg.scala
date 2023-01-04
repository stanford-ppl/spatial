package spatial.node

import argon._
import forge.tags._
import spatial.lang._

abstract class RegAlloc[A: Bits, C[T]](implicit C: Type[C[A]]) extends MemAlloc[A, C] {
  def init: Bits[A]
}

@op case class RegNew[A:Bits](init: Bits[A]) extends RegAlloc[A,Reg] { def dims = Nil }
@op case class FIFORegNew[A:Bits](init: Bits[A]) extends RegAlloc[A,FIFOReg] { def dims = Nil }
@op case class ArgInNew[A:Bits](init: Bits[A]) extends RegAlloc[A,Reg] { def dims = Nil }
@op case class ArgOutNew[A:Bits](init: Bits[A]) extends RegAlloc[A,Reg] { def dims = Nil }
@op case class HostIONew[A:Bits](init: Bits[A]) extends RegAlloc[A,Reg] { def dims = Nil }

@op case class RegWrite[A:Bits](
    mem:  Reg[A],
    data: Bits[A],
    ens:  Set[Bit])
  extends Enqueuer[A]

@op case class FIFORegEnq[A:Bits](
    mem:  FIFOReg[A],
    data: Bits[A],
    ens:  Set[Bit])
  extends Enqueuer[A]

@op case class RegRead[A:Bits](mem: Reg[A]) extends Reader[A,A] {
  override def effects: Effects = super.effects andAlso Effects.Unique
  override val isTransient = true
  // Register read never takes enables
  override var ens: Set[Bit] = Set.empty
  override def addr: Seq[Idx] = Nil
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}

@op case class FIFORegDeq[A:Bits](mem: FIFOReg[A], ens: Set[Bit]) extends Dequeuer[A,A] {
  override def effects: Effects = super.effects andAlso Effects.Unique
  override val isTransient = true
  override def addr: Seq[Idx] = Nil
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}

@op case class RegReset[A:Bits](mem: Reg[A], ens: Set[Bit]) extends Resetter[A]

@op case class GetReg[A:Bits](mem: Reg[A]) extends Reader[A,A] {
  override def addr: Seq[Idx] = Nil
  override var ens: Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}

@op case class SetReg[A:Bits](mem: Reg[A], data: Bits[A]) extends Writer[A] {
  override def addr: Seq[Idx] = Nil
  override var ens:  Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}