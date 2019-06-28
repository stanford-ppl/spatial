package spatial.issues

import argon._
import forge.tags._
import spatial.metadata.control._

case class PotentialBufferHazard(mem: Sym[_], bad: List[(Sym[_],scala.Int)]) extends Issue {
  @stateful override def onUnresolved(traversal: String): Unit = {
    error(mem.ctx, s"${mem.name.getOrElse("memory")} defined here has potential buffer hazard.")
    error(s"This must be resolved manually by declaring ${mem.name.getOrElse("memory")} with either ")
    error(s"the .buffer or .nonbuffer flag, depending on the desired behavior (i.e.: val s = SRAM[T](N).buffer).")
    error("")
    error(s"Note that there are other ways to create a buffer hazard, but reading before writing is")
    error(s"the most common way to get unexpected behavior from buffering.  (TODO: Use iterator analysis to detect true hazards)")
    error(mem.ctx)

    error(stm(mem))
    bad.foreach{case (a,i)=>
      error("")

      error(s"  access ${a} connects to port ${i}: ")
      error(s"    ${a.ctx}: ${a.ctx.content.getOrElse("<?:?:?>")}")
      error("")
    }
    error("")
  }
}

