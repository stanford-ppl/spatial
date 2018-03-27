package spatial.node

import forge.tags._
import spatial.lang._

@op case class FringeDenseLoad[A:Bits,C[T]](
    dram:       DRAM[A,C],
    cmdStream:  StreamOut[BurstCmd],
    dataStream: StreamIn[A])
  extends FringeNode[A,Void]

@op case class FringeDenseStore[A:Bits,C[T]](
    dram:       DRAM[A,C],
    cmdStream:  StreamOut[BurstCmd],
    dataStream: StreamOut[Tup2[A,Bit]],
    ackStream:  StreamIn[Bit])
  extends FringeNode[A,Void]

@op case class FringeSparseLoad[A:Bits,C[T]](
    dram:       DRAM[A,C],
    addrStream: StreamOut[I64],
    dataStream: StreamIn[A])
  extends FringeNode[A,Void]

@op case class FringeSparseStore[A:Bits,C[T]](
    dram:      DRAM[A,C],
    cmdStream: StreamOut[Tup2[A,I64]],
    ackStream: StreamIn[Bit])
  extends FringeNode[A,Void]

