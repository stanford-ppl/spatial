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

// Fringe-defined coalescing store. Commands are defined as <data, base, val>
// If base is >= 0, reset the base address for future commands. Otherwise, 
// auto-increment the base address.
@op case class FringeCoalStore[A:Bits,C[T]](
    dram:      DRAM[A,C],
    setupStream: StreamOut[Tup2[I64,I32]],
    cmdStream: StreamOut[Tup2[A,Bit]],
    ackStream: StreamIn[Bit],
    par: scala.Int)
  extends FringeNode[A,Void] {
  override def effects: Effects = Effects.Writes(ackStream, dram)
}

@op case class FringeDynStore[A:Bits,C[T]](
    dram:      DRAM[A,C],
    setupStream: StreamOut[I64],
    cmdStream: StreamOut[Tup2[A,Bit]],
    ackStream: StreamIn[I32],
    par: scala.Int)
  extends FringeNode[A,Void] {
  override def effects: Effects = Effects.Writes(ackStream, dram)
}

@op case class FringeStreamLoad[A:Bits,C[T]](
    dram:      DRAM[A,C],
    setupStream: StreamOut[Tup2[I64,I32]],
    dataStream: StreamIn[I32],
    par: scala.Int,
    comp: scala.Boolean)
  extends FringeNode[A,Void] {
  override def effects: Effects = Effects.Writes(dataStream, dram)
}

@op case class BVBuildNoTree(
    shift: Int,
    setupStream: StreamOut[Tup2[I32,I32]],
    cmdStream: StreamOut[I32],
    retStream: StreamIn[U32])
  extends FringeNode[I32,Void] {
  override def effects: Effects = Effects.Writes(retStream)
}

@op case class BVBuildTree(
    shift: Int,
    setupStream: StreamOut[I32],
    cmdStream: StreamOut[I32],
    vecOut: StreamIn[U32],
    // scalOut: StreamIn[Tup2[I32,Bit]])
    scalOut: StreamIn[I32])
  extends FringeNode[I32,Void] {
  override def effects: Effects = Effects.Writes(vecOut, scalOut)
}

@op case class BVBuildTreeLen(
    shift: Int,
    setupStream: StreamOut[I32],
    cmdStream: StreamOut[I32],
    scalOut: StreamIn[I32])
  extends FringeNode[I32,Void] {
  override def effects: Effects = Effects.Writes(scalOut)
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

  @rig def coalStore[A:Bits,C[T]](
    dram:      DRAM[A,C],
    setupStream: StreamOut[Tup2[I64,I32]],
    cmdStream: StreamOut[Tup2[A,Bit]],
    ackStream: StreamIn[Bit],
    par: scala.Int
  ): Void = stage(FringeCoalStore[A,C](dram,setupStream,cmdStream,ackStream, par))

  @rig def dynStore[A:Bits,C[T]](
    dram:      DRAM[A,C],
    setupStream: StreamOut[I64],
    cmdStream: StreamOut[Tup2[A,Bit]],
    ackStream: StreamIn[I32],
    par: scala.Int
  ): Void = stage(FringeDynStore[A,C](dram,setupStream,cmdStream,ackStream, par))

  @rig def streamLoad[A:Bits,C[T]](
    dram:      DRAM[A,C],
    setupStream: StreamOut[Tup2[I64,I32]],
    dataStream: StreamIn[I32],
    par: scala.Int,
    comp: scala.Boolean
  ): Void = stage(FringeStreamLoad[A,C](dram,setupStream,dataStream,par,comp))

  @rig def bvBuildNoTree(
    shift: Int,
    setupStream: StreamOut[Tup2[I32,I32]],
    cmdStream: StreamOut[I32],
    retStream: StreamIn[U32]
  ): Void = stage(BVBuildNoTree(shift,setupStream,cmdStream,retStream))

  @rig def bvBuildTree(
    shift: Int,
    setupStream: StreamOut[I32],
    cmdStream: StreamOut[I32],
    vecOut: StreamIn[U32],
    // scalOut: StreamIn[Tup2[I32,Bit]]
    scalOut: StreamIn[I32]
  ): Void = stage(BVBuildTree(shift,setupStream,cmdStream,vecOut,scalOut))

  @rig def bvBuildTreeLen(
    shift: Int,
    setupStream: StreamOut[I32],
    cmdStream: StreamOut[I32],
    scalOut: StreamIn[I32]
  ): Void = stage(BVBuildTreeLen(shift,setupStream,cmdStream,scalOut))
}
