package spatial.node

import core.Effects
import forge.tags._
import spatial.lang._

@op case class ArgInNew[A:Bits](init: Bits[A]) extends MemAlloc[ArgIn[A]] {
  def dims = Nil
}

@op case class SetArgIn[A:Bits](mem: ArgIn[A], data: Bits[A]) extends Writer[A] {
  override def addr: Seq[Idx] = Nil
  override var ens:  Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}

@op case class ArgInRead[A:Bits](mem: ArgIn[A]) extends Reader[A,A] {
  override val isTransient: Boolean = true
  override def addr: Seq[Idx] = Nil
  override var ens:  Set[Bit] = Set.empty
  override def updateEn(f: Tx, addEns: Set[Bit]) = this.update(f)
  override def mirrorEn(f: Tx, addEns: Set[Bit]) = this.mirror(f)
}
