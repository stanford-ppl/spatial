package spatial.node

import argon._
import forge.tags._
import spatial.lang._

@op case class FIFONew[A:Bits](depth: I32) extends MemAlloc[A,FIFO] {
  def dims = Seq(depth)
  def resize(newDepth: I32): Unit = {
    this.update(new Tx {
      def apply[T](v: T): T = {
        v match {
          case x: I32 if x == depth => newDepth.asInstanceOf[T]
          case _ => v
        }
      }
    })
  }
}


@op case class FIFOEnq[A:Bits](mem: FIFO[A], data: Bits[A], ens: Set[Bit]) extends Enqueuer[A]
@op case class FIFODeq[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends Dequeuer[A,A]
@op case class FIFOPriorityDeq[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends Dequeuer[A,A]

/** Node representing the possibility to dequeue.  To be used with Blackbox interfaces */
@op case class FIFODeqInterface[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends Dequeuer[A,A] {
  override val isTransient = true
}

@op case class FIFOVecEnq[A:Bits](
  mem:  FIFO[A],
  data: Vec[A],
  adr:  Seq[Idx],
  ens:  Set[Bit]
) extends VectorEnqueuer[A] {
    override def addr: Seq[Idx] = adr
}

@op case class FIFOVecDeq[A:Bits](mem: FIFO[A], adr: Seq[Idx], ens: Set[Bit])(implicit VA: Vec[A]) extends VectorDequeuer[A] {
  override def addr: Seq[Idx] = adr
}

@op case class FIFOPeek[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends Reader[A,A] {
  override def addr: Seq[Idx] = Nil
}

@op case class FIFOIsEmpty[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusReader[Bit]
@op case class FIFOIsFull[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusReader[Bit]
@op case class FIFOIsAlmostEmpty[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusReader[Bit]
@op case class FIFOIsAlmostFull[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusReader[Bit]
@op case class FIFONumel[A:Bits](mem: FIFO[A], ens: Set[Bit]) extends StatusReader[I32]

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

@op case class FIFOBankedPriorityDeq[A:Bits](
    mem:  FIFO[A],
    enss: Seq[Set[Bit]]
    )(implicit val vA: Type[Vec[A]])
  extends BankedDequeue[A]

