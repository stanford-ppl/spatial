package spatial.lang

import argon._
import forge.tags._
import spatial.node._
import spatial.metadata.memory._

import scala.collection.mutable.Queue

@ref class FIFO[A:Bits] extends Top[FIFO[A]]
         with LocalMem1[A,FIFO]
         with Ref[Queue[Any],FIFO[A]] {
  val A: Bits[A] = Bits[A]
  val evMem: FIFO[A] <:< LocalMem[A,FIFO] = implicitly[FIFO[A] <:< LocalMem[A,FIFO]]

  /** Returns true when this FIFO contains no elements, false otherwise. **/
  @api def isEmpty: Bit = stage(FIFOIsEmpty(this,Set.empty))

  /** Returns true when this FIFO cannot fit any more elements, false otherwise. **/
  @api def isFull: Bit = stage(FIFOIsFull(this,Set.empty))

  /** Returns true when this FIFO contains exactly one element, false otherwise. **/
  @api def isAlmostEmpty: Bit = stage(FIFOIsAlmostEmpty(this,Set.empty))

  /** Returns true when this FIFO can fit exactly one more element, false otherwise. **/
  @api def isAlmostFull: Bit = stage(FIFOIsAlmostFull(this,Set.empty))

  /** Returns the number of elements currently in this FIFO. **/
  @api def numel: I32 = stage(FIFONumel(this,Set.empty))

  /** Creates an enqueue (write) port to this FIFO of `data`. **/
  @api def enq(data: A): Void = this.enq(data, true)

  /** Creates a conditional enqueue (write) port to this FIFO of `data` enabled by `en`. **/
  @api def enq(data: A, en: Bit): Void = stage(FIFOEnq(this,data,Set(en)))

  /** Creates a dequeue (destructive read) port to this FIFO. **/
  @api def deq(): A = this.deq(true)

  /** Creates a conditional dequeue (destructive read) port to this FIFO enabled by `en`. **/
  @api def deq(en: Bit): A = stage(FIFODeq(this,Set(en)))

  /** Creates a non-destructive read port to this FIFO. **/
  @api def peek(): A = stage(FIFOPeek(this,Set.empty))

  /** Allow "unsafe" banking, where two enq's can technically happen simultaneously and one will be dropped.
    * Use in cases where FIFOs are used in stream controllers and have enq's in multiple places that the user
    * knows are mutually exclusive
    */
  def conflictable: FIFO[A] = { this.shouldIgnoreConflicts = true; me }
  /** Do not attempt to bank memory by duplication */
  def noduplicate: FIFO[A] = { this.isNoDuplicate = true; me }

  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = stage(FIFODeq(this,ens))
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void = stage(FIFOEnq(this,data,ens))
  @rig def __reset(ens: Set[Bit]): Void = void
}
object FIFO {
  @api def apply[A:Bits](depth: I32): FIFO[A] = stage(FIFONew(depth))
  @rig def alloc[A:Bits](depth: I32): FIFO[A] = stage(FIFONew(depth))
  @rig def deq[A](fifo: FIFO[A], ens: Set[Bit] = Set.empty): A = {
    implicit val tA: Bits[A] = fifo.A
    stage(FIFODeq(fifo, ens))
  }
  @rig def enq[A](fifo: FIFO[A], data: Bits[A], ens: Set[Bit] = Set.empty): Void = {
    implicit val tA: Bits[A] = fifo.A
    stage(FIFOEnq(fifo,data,ens))
  }

}
