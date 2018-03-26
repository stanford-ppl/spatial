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
}
object LIFO {
  @api def apply[A:Bits](depth: I32): LIFO[A] = stage(LIFONew(depth))
}
