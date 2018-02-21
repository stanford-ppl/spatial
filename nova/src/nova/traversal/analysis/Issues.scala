package nova.traversal.analysis

import forge.tags._
import core._
import nova.data._
import spatial.lang._

case class AmbiguousMetaPipes(mem: Sym[_], mps: Map[Ctrl,Set[(Sym[_],Sym[_])]]) extends Issue {
  @stateful override def onUnresolved(traversal: String): Unit = {
    error(s"Ambiguous metapipes for readers/writers of $mem defined here:")
    error(stm(mem))
    error(mem.ctx)
    mps.foreach{case (pipe,accs) =>
      error(s"  metapipe: $pipe ")
      error(s"  accesses: ")
      accs.foreach{a =>
        error(s"    ${stm(a._1)} [${parentOf(a._1)}]")
        error(s"    ${stm(a._2)} [${parentOf(a._2)}]")
        error("")
      }
      error(stm(pipe.sym))
      error(pipe.sym.ctx)
      error("")
    }
    state.logError()
  }
}

case class UnbankableGroup(mem: Sym[_], reads: Set[AccessMatrix], writes: Set[AccessMatrix]) extends Issue {
  @stateful override def onUnresolved(traversal: String): Unit = {
    error(mem.ctx, s"Could not bank the following reads and writes for memory $mem")
    error(mem.ctx)
    reads.foreach{read =>
      error(read.access.ctx, s"  ${stm(read.access)}", noError = true)
      error(read.access.ctx)
    }
    writes.foreach{write =>
      error(write.access.ctx, s"  ${stm(write.access)}", noError = true)
      error(write.access.ctx)
    }
  }
}