package spatial.issues

import argon._
import forge.tags._
import spatial.data._
import spatial.util._

case class AmbiguousMetaPipes(mem: Sym[_], mps: Map[Ctrl,Set[(Sym[_],Sym[_])]]) extends Issue {
  @stateful override def onUnresolved(traversal: String): Unit = {
    error(mem.ctx, s"${mem.name.getOrElse("memory")} defined here is used across multiple pipelines.")
    error(s"Hierarchical buffering is currently disallowed.")
    error(mem.ctx)

    dbgs(stm(mem))
    mps.zipWithIndex.foreach{case ((pipe,accs),i) =>
      val adj = if (i == 0) "First" else "Conflicting"
      error(pipe.s.get.ctx, s"$adj pipeline defined here.", noError = true)
      error(pipe.s.get.ctx)
//      error("With accesses: ")
//      val accesses = accs.flatMap{case (s1,s2) => Seq(s1,s2) }
//      accesses.foreach{a =>
//        error(a.ctx, "Accessed here.")
//        error(a.ctx)
//      }
      error("")

      dbgs(s"  metapipe: $pipe ")
      dbgs(s"  accesses: ")
      accs.foreach{a =>


        dbgs(s"    ${stm(a._1)} [${a._1.parent}]")
        dbgs(s"    ${stm(a._2)} [${a._2.parent}]")
        dbgs("")
      }
      pipe.s.foreach{sym =>
        dbgs(stm(sym))
        dbgs(sym.ctx)
        dbgs("")
      }
    }
    error("")
  }
}

