package spatial.issues

import core._
import forge.tags.stateful
import spatial.data.AccessMatrix

case class UnbankableGroup(mem: Sym[_], reads: Set[AccessMatrix], writes: Set[AccessMatrix]) extends Issue {
  @stateful override def onUnresolved(traversal: String): Unit = {
    error(mem.src, s"Could not bank the following reads and writes for memory $mem")
    error(mem.src)
    reads.foreach{read =>
      error(read.access.src, s"  ${stm(read.access)}", noError = true)
      error(read.access.src)
    }
    writes.foreach{write =>
      error(write.access.src, s"  ${stm(write.access)}", noError = true)
      error(write.access.src)
    }
  }
}
