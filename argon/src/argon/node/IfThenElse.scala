package argon.node

import argon._
import argon.lang._
import forge.tags._

@op case class IfThenElse[T:Type](cond: Bit, thenBlk: Block[T], elseBlk: Block[T]) extends DSLOp[T] {
  override def aliases: Set[Sym[_]] = syms(thenBlk.result, elseBlk.result)
}
