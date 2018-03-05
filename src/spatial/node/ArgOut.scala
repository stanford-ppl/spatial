package spatial.node

import core._
import forge.tags._
import spatial.lang._

@op case class ArgOutNew[A:Bits](init: Bits[A]) extends MemAlloc[ArgOut[A]] {
  def dims = Nil
}

@op case class ArgOutWrite[A:Bits](mem: ArgOut[A], data: Bits[A]) extends Writer[A] {
  override def addr: Seq[Idx] = Nil
  override var ens:  Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}

@op case class GetArgOut[A:Bits](mem: ArgOut[A]) extends Reader[A,A] {
  override def addr: Seq[Idx] = Nil
  override var ens: Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}
