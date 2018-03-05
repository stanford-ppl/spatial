package spatial.node

import core._
import forge.tags._
import spatial.lang._

@op case class RegNew[T:Bits](init: Bits[T]) extends MemAlloc[Reg[T]] {
  def dims = Nil
}

@op case class RegWrite[T:Bits](
    mem:  Reg[T],
    data: Bits[T],
    ens:  Set[Bit])
  extends Enqueuer[T]


@op case class RegRead[T:Bits](mem: Reg[T]) extends Reader[T,T] {
  override val isTransient = true
  // Register read never takes enables
  override var ens: Set[Bit] = Set.empty
  override def addr: Seq[Idx] = Nil
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}

@op case class RegReset[T:Bits](mem: Reg[T], ens: Set[Bit]) extends Resetter[T]
