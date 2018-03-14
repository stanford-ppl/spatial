package spatial.node

import core._
import forge.tags._
import spatial.lang._

@op case class FIFONew[A:Bits](depth: I32) extends MemAlloc[A,FIFO] {
  def dims = Seq(depth)
}


@op case class FIFOEnq[A:Bits](mem: FIFO[A], data: Bits[A], ens: Set[Bit]) extends Enqueuer[A]
@op case class FIFODeq[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends Dequeuer[A,A]
@op case class FIFOPeek[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends Reader[A,A] {
  override def addr: Seq[Idx] = Nil
}

@op case class FIFOIsEmpty[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusRead[Bit]
@op case class FIFOIsFull[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusRead[Bit]
@op case class FIFOIsAlmostEmpty[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusRead[Bit]
@op case class FIFOIsAlmostFull[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusRead[Bit]
@op case class FIFONumel[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusRead[I32]

@op case class FIFOBankedEnq[A:Bits](
    mem:  FIFO[A],
    data: Seq[Sym[A]],
    enss: Seq[Set[Bit]])
  extends BankedEnqueue[A]


@op case class FIFOBankedDeq[A:Bits](
    mem:  FIFO[A],
    enss: Seq[Set[Bit]]
    )(implicit val vA: Type[Vec[A]])
  extends BankedDequeue[A]


