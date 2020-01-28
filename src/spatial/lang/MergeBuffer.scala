package spatial.lang

import argon._
import forge.tags._
import spatial.node._
import spatial.metadata.memory._

import scala.collection.mutable.Queue

@ref class MergeBuffer[A:Bits] extends Top[MergeBuffer[A]]
         with LocalMem1[A,MergeBuffer]
         with Ref[Queue[Any],MergeBuffer[A]] {
  val A: Bits[A] = Bits[A]
  val evMem: MergeBuffer[A] <:< LocalMem[A,MergeBuffer] = implicitly[MergeBuffer[A] <:< LocalMem[A,MergeBuffer]]

  @api def enq(way: Int, data: A): Void = stage(MergeBufferEnq(this, way, data, Set(true)))
  @api def bound(way: Int, bound: I32): Void = stage(MergeBufferBound(this, way, bound, Set(true)))
  @api def init(init: Bit): Void = stage(MergeBufferInit(this, init, Set(true)))

  @api def deq(): A = stage(MergeBufferDeq(this, Set(true)))

  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = stage(MergeBufferDeq(this,ens))
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void = void
  @rig def __reset(ens: Set[Bit]): Void = void
}

object MergeBuffer {
  @api def apply[A:Bits](ways: I32, par: I32): MergeBuffer[A] = {
    val x = stage(MergeBufferNew(ways, par))
    x.isWriteBuffer = true
    x.isMustMerge = true
    x
  }
}
