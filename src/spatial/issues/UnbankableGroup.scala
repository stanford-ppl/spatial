package spatial.issues

import argon._
import forge.tags.stateful
import spatial.metadata.access._

case class UnbankableGroup(mem: Sym[_], reads: Set[AccessMatrix], writes: Set[AccessMatrix]) extends Issue {
  @stateful override def onUnresolved(traversal: String): Unit = {
    error(mem.ctx, s"Could not bank the following reads and writes for memory $mem")
    error(mem.ctx)
    reads.foreach{read =>
      error(read.access.ctx, s"  ${stm(read.access)} {${read.unroll.mkString(",")}}", noError = true)
      error(read.access.ctx)
    }
    writes.foreach{write =>
      error(write.access.ctx, s"  ${stm(write.access)} {${write.unroll.mkString(",")}}", noError = true)
      error(write.access.ctx)
    }
  }
}
