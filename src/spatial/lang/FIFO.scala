package spatial.lang

import core._
import forge.tags._
import spatial.node._

import scala.collection.mutable.Queue

@ref class FIFO[A:Bits] extends Top[FIFO[A]] with LocalMem[A,FIFO] with Ref[Queue[Any],FIFO[A]] {
  val tA: Bits[A] = Bits[A]
  val ev: FIFO[A] <:< LocalMem[A,FIFO] = implicitly[FIFO[A] <:< LocalMem[A,FIFO]]
}
object FIFO {
  @api def apply[A:Bits](depth: I32): FIFO[A] = stage(FIFONew(depth))
}
