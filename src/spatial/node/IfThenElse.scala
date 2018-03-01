package spatial.node

import core._
import spatial.lang._

case class IfThenElse[T:Type](cond: Bit, thenBlk: Block[T], elseBlk: Block[T]) extends Op[T] {
  override def aliases: Seq[Sym[_]] = syms(thenBlk.result, elseBlk.result)
}
