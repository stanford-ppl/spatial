package pcc
package ir

import forge._

trait DRAM[A] {
  val tA: Sym[A]
  type AI = tA.I
}

case class DRAM1[A](eid: Int)(implicit val tA: Sym[A]) extends Mem[A,DRAM1](eid) with DRAM[A] {
  override type I = Array[AI]
  override def fresh(id: Int) = DRAM1(id)
  override def stagedClass = classOf[DRAM1[A]]
  override def typeArguments: List[Sym[_]] = List(tA)
}

case class DRAM2[A](eid: Int)(implicit val tA: Sym[A]) extends Mem[A,DRAM2](eid) with DRAM[A] {
  override type I = Array[AI]
  override def fresh(id: Int) = DRAM2(id)
  override def stagedClass = classOf[DRAM2[A]]
  override def typeArguments: List[Sym[_]] = List(tA)
}



//case class DRAMAlloc[A<:Num[A],C[A]<:DRAM[A,C]](dims: List[I32]) extends
