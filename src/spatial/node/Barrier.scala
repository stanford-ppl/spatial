package spatial.node

import argon._
import argon.node.{Alloc, Primitive}
import forge.tags._
import spatial.lang._

/** An allocation of a Barrier.
  */
@op case class BarrierNew[A:Bits](init: scala.Int, depth: scala.Int) extends MemAlloc[A,Barrier]{ def dims = Nil }

abstract class BarrierOperator(mem: Barrier[_]) extends Primitive[BarrierTransaction]

/** Push tokens to a Barrier
  * @param mem The Barrier to be pushed to
  */
@op case class BarrierPush(mem: Barrier[_]) extends BarrierOperator(mem)

/** Pop tokens from a barrier
  * @param mem The Barrier to be popped from
  */
@op case class BarrierPop(mem: Barrier[_]) extends BarrierOperator(mem)
