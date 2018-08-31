package spatial.node

import argon._
import forge.tags._
import spatial.lang._

@op case class LineBufferNew[A:Bits](rows: I32, cols: I32) extends MemAlloc[A,LineBuffer] {
  val dims = Seq(rows, cols)
}

@op case class LineBufferEnq[A:Bits](mem: LineBuffer[A], data: Bits[A], ens: Set[Bit]) extends Enqueuer[A]
@op case class LineBufferRead[A:Bits](mem: LineBuffer[A], addr: Seq[Idx], ens: Set[Bit]) extends Reader[A,A]

@op case class LineBufferBankedEnq[A:Bits](
    mem:  LineBuffer[A],
    data: Seq[Sym[A]],
    enss: Seq[Set[Bit]])
  extends BankedEnqueue[A]


@op case class LineBufferBankedRead[A:Bits](
    mem:  LineBuffer[A],
    bank: Seq[Seq[Idx]],
    ofs: Seq[Idx],
    enss: Seq[Set[Bit]]
    )(implicit val vA: Type[Vec[A]])
  extends BankedReader[A]


