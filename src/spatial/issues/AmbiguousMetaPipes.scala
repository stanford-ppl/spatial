package spatial.issues

import core._
import forge.tags._
import spatial.data._

case class AmbiguousMetaPipes(mem: Sym[_], mps: Map[Ctrl,Set[(Sym[_],Sym[_])]]) extends Issue {
  @stateful override def onUnresolved(traversal: String): Unit = {
    error(s"Ambiguous metapipes for readers/writers of $mem defined here:")
    error(stm(mem))
    error(mem.src)
    mps.foreach{case (pipe,accs) =>
      error(s"  metapipe: $pipe ")
      error(s"  accesses: ")
      accs.foreach{a =>
        error(s"    ${stm(a._1)} [${parentOf(a._1)}]")
        error(s"    ${stm(a._2)} [${parentOf(a._2)}]")
        error("")
      }
      error(stm(pipe.sym))
      error(pipe.sym.src)
      error("")
    }
    state.logError()
  }
}

