package spatial.node

import core._
import forge.tags._
import spatial.lang._

@op case class LIFONew[A:Bits](depth: I32) extends MemAlloc[A,LIFO] {
  def dims = Seq(depth)
}

@op case class LIFOPush[A:Bits](mem: LIFO[A], data: Bits[A], ens: Set[Bit]) extends Enqueuer[A]
@op case class LIFOPop[A:Bits](mem: LIFO[A], ens: Set[Bit]) extends Dequeuer[A,A]
@op case class LIFOPeek[A:Bits](mem: LIFO[A], ens: Set[Bit]) extends Reader[A,A] {
  def addr: Seq[Idx] = Nil
}

@op case class LIFOIsEmpty[A:Bits](mem: LIFO[A], ens: Set[Bit]) extends StatusReader[Bit]
@op case class LIFOIsFull[A:Bits](mem: LIFO[A], ens: Set[Bit]) extends StatusReader[Bit]
@op case class LIFOIsAlmostEmpty[A:Bits](mem: LIFO[A], ens: Set[Bit]) extends StatusReader[Bit]
@op case class LIFOIsAlmostFull[A:Bits](mem: LIFO[A], ens: Set[Bit]) extends StatusReader[Bit]
@op case class LIFONumel[A:Bits](mem: LIFO[A], ens: Set[Bit]) extends StatusReader[I32]

@op case class LIFOBankedPush[A:Bits](
    mem:  LIFO[A],
    data: Seq[Sym[A]],
    enss: Seq[Set[Bit]])
  extends BankedEnqueue[A]


@op case class LIFOBankedPop[A:Bits](
    mem:  LIFO[A],
    enss: Seq[Set[Bit]]
    )(implicit val vA: Type[Vec[A]])
  extends BankedDequeue[A]


