package spatial.lang

import argon._
import forge.tags._
import spatial.node._

import scala.collection.mutable.MutableList

@ref class LIFO[A:Bits] extends Top[LIFO[A]]
         with LocalMem1[A,LIFO]
         with Ref[MutableList[Any],LIFO[A]] {
  val A: Bits[A] = Bits[A]
  val evMem: LIFO[A] <:< LocalMem[A,LIFO] = implicitly[LIFO[A] <:< LocalMem[A,LIFO]]

  /** Returns true when this LIFO contains no elements, false otherwise. **/
  @api def isEmpty: Bit = stage(LIFOIsEmpty(this,Set.empty))

  /** Returns true when this LIFO cannot fit any more elements, false otherwise. **/
  @api def isFull: Bit = stage(LIFOIsFull(this,Set.empty))

  /** Returns true when this LIFO contains exactly one element, false otherwise. **/
  @api def isAlmostEmpty: Bit = stage(LIFOIsAlmostEmpty(this,Set.empty))

  /** Returns true when this LIFO can fit exactly one more element, false otherwise. **/
  @api def isAlmostFull: Bit = stage(LIFOIsAlmostFull(this,Set.empty))

  /** Returns the number of elements currently in this LIFO. **/
  @api def numel: I32 = stage(LIFONumel(this,Set.empty))

  /** Creates a push (write) port to this LIFO of `data`. **/
  @api def push(data: A): Void = this.push(data, true)

  /** Creates a conditional push (write) port to this LIFO of `data` enabled by `en`. **/
  @api def push(data: A, en: Bit): Void = stage(LIFOPush(this,data,Set(en)))

  /** Creates a pop (destructive read) port to this LIFO. **/
  @api def pop(): A = this.pop(true)

  /** Creates a conditional pop (destructive read) port to this LIFO enabled by `en`. **/
  @api def pop(en: Bit): A = stage(LIFOPop(this,Set(en)))

  /** Creates a non-destructive read port to this LIFO. **/
  @api def peek(): A = stage(LIFOPeek(this,Set.empty))

  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = stage(LIFOPop(this,ens))
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void = stage(LIFOPush(this,data,ens))
  @rig def __reset(ens: Set[Bit]): Void = void
}
object LIFO {
  @api def apply[A:Bits](depth: I32): LIFO[A] = stage(LIFONew(depth))
}
