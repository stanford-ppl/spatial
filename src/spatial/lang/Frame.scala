package spatial.lang

import argon._
import forge.tags._
import spatial.node._
import spatial.lang.types._
import spatial.metadata.memory._

import utils.implicits.collections._

/** A Frame is a special off-chip memory type that is a stream-only memory.  It interfaces with the Accel via an AXI4-Stream
  * interface, and can only be read/written once.  It is designed to be used with the Frame interface in the Rogue
  * API, simulating streaming devices in a system..
  *
  * @param `bits$A`
  * @param evMem
  * @tparam A
  * @tparam C
  */
abstract class Frame[A:Bits,C[T]](implicit val evMem: C[A] <:< Frame[A,C]) extends Top[C[A]] with RemoteMem[A,C] {
  val A: Bits[A] = Bits[A]

  protected def M1: Type[Frame1[A]] = implicitly[Type[Frame1[A]]]
  def rank: Seq[Int]

  /** Returns the total capacity (in elements) of this Frame. */
  @api def size: I32 = stage(MemDim(this,rank(0)))

  /** Returns the 64-bit address of this Frame */
  @api def address: I64 = stage(FrameAddress(me))

  @api override def neql(that: C[A]): Bit = {
    error(this.ctx, "Native comparison of Frames is unsupported. Use getMem to extract data.")
    error(this.ctx)
    super.neql(that)
  }
  @api override def eql(that: C[A]): Bit = {
    error(this.ctx, "Native comparision of Frames is unsupported. Use getMem to extract data.")
    error(this.ctx)
    super.eql(that)
  }
}
object Frame {
  /** Allocates a 1-dimensional [[Frame1]] with capacity of `length` elements of type A. */
  @api def apply[A:Bits](length: scala.Int, stream: StreamIn[A]): Frame1[A] = {
    stage(FrameHostNew[A,Frame1](Seq(length),zero[A],stream))
  }
  @api def apply[A:Bits](length: scala.Int, stream: StreamOut[A]): Frame1[A] = {
    stage(FrameHostNew[A,Frame1](Seq(length),zero[A],stream))
  }
}

/** A 1-dimensional [[Frame]] with elements of type A. */
@ref class Frame1[A:Bits] extends Frame[A,Frame1] with Ref[Array[Any],Frame1[A]] with Mem1[A,Frame1] {
  def rank: Seq[Int] = Seq(0)
  @api def length: I32 = size

  /** Creates a dense, burst transfer from the on-chip `local` to this Frame's region of main memory. */
  @api def store[Local[T]<:LocalMem1[T,Local]](local: Local[A])(implicit tp: Type[Local[A]]): Void = {
    stage(FrameTransmit(this,local,isLoad = false))
  }

}

