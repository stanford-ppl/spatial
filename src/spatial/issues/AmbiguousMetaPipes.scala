package spatial.issues

import argon._
import forge.tags._
import spatial.data._
import spatial.util._

case class AmbiguousMetaPipes(mem: Sym[_], mps: Map[Ctrl,Set[(Sym[_],Sym[_])]]) extends Issue {
  @stateful override def onUnresolved(traversal: String): Unit = {
    error(s"Ambiguous metapipes for readers/writers of $mem defined here:")
    error(stm(mem))
    error(mem.ctx)
    mps.foreach{case (pipe,accs) =>
      error(s"  metapipe: $pipe ")
      error(s"  accesses: ")
      accs.foreach{a =>
        error(s"    ${stm(a._1)} [${a._1.parent}]")
        error(s"    ${stm(a._2)} [${a._2.parent}]")
        error("")
      }
      pipe.s.foreach{sym =>
        error(stm(sym))
        error(sym.ctx)
        error("")
      }
    }
    state.logError()
  }
}

