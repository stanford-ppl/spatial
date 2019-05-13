package spatial.node

import argon._
import forge.tags.op
import spatial.lang._

@op case class StreamInNew[A:Bits](bus: Bus) extends MemAlloc[A,StreamIn] {
  def dims: Seq[I32] = Seq(I32(1))
}

@op case class StreamInRead[A:Bits](
    mem: StreamIn[A],
    ens: Set[Bit])
  extends Dequeuer[A,A]

@op case class StreamInBankedRead[A:Bits](
    mem:  StreamIn[A],
    enss: Seq[Set[Bit]]
    )(implicit vT: Type[Vec[A]])
  extends BankedDequeue[A]
