package spatial.node

import argon._
import forge.tags._
import spatial.lang._

@op case class FringeDenseLoad[A:Bits,C[T]](
    dram:       DRAM[A,C],
    cmdStream:  StreamOut[BurstCmd],
    dataStream: StreamIn[A])
  extends FringeNode[A,Void] {
  override def effects: Effects = Effects.Writes(dataStream)
}

@op case class FringeDenseStore[A:Bits,C[T]](
    dram:       DRAM[A,C],
    cmdStream:  StreamOut[BurstCmd],
    dataStream: StreamOut[Tup2[A,Bit]],
    ackStream:  StreamIn[Bit])
  extends FringeNode[A,Void] {
  override def effects: Effects = Effects.Writes(ackStream, dram)
}

@op case class FringeSparseLoad[A:Bits,C[T]](
    dram:       DRAM[A,C],
    addrStream: StreamOut[I64],
    dataStream: StreamIn[A])
  extends FringeNode[A,Void] {
  override def effects: Effects = Effects.Writes(dataStream)
}

@op case class FringeSparseStore[A:Bits,C[T]](
    dram:      DRAM[A,C],
    cmdStream: StreamOut[Tup2[A,I64]],
    ackStream: StreamIn[Bit])
  extends FringeNode[A,Void] {
  override def effects: Effects = Effects.Writes(ackStream, dram)
}


object Fringe {
  @rig def denseLoad[A:Bits,C[T]](
    dram: DRAM[A,C],
    cmdStream: StreamOut[BurstCmd],
    dataStream: StreamIn[A]
  ): Void = stage(FringeDenseLoad[A,C](dram,cmdStream,dataStream))

  @rig def denseStore[A:Bits,C[T]](
    dram:       DRAM[A,C],
    cmdStream:  StreamOut[BurstCmd],
    dataStream: StreamOut[Tup2[A,Bit]],
    ackStream:  StreamIn[Bit]
  ): Void = stage(FringeDenseStore[A,C](dram,cmdStream,dataStream,ackStream))

  @rig def sparseLoad[A:Bits,C[T]](
    dram:       DRAM[A,C],
    addrStream: StreamOut[I64],
    dataStream: StreamIn[A]
  ): Void = stage(FringeSparseLoad[A,C](dram,addrStream,dataStream))

  @rig def sparseStore[A:Bits,C[T]](
    dram:      DRAM[A,C],
    cmdStream: StreamOut[Tup2[A,I64]],
    ackStream: StreamIn[Bit]
  ): Void = stage(FringeSparseStore[A,C](dram,cmdStream,ackStream))
}
