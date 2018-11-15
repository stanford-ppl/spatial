package spatial.traversal

import argon._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._

case class BreakAnalyzer(IR: State) extends BlkTraversal {

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    if (lhs.isBreak){
      val ancestor: Sym[_] = lhs.ancestors.reverse.collectFirst{case x if x.s.isDefined && !x.s.get.isBranch => x.s.get}.get
      val lane = ancestor.getBreakInfo.getOrElse(Seq()).size
      dbgs(s"Break node $lhs ancestors: ${lhs.ancestors.reverse}")
      dbgs(s"  Detected $ancestor to be innermost loop control")
      dbgs(s"  Detected $lane preceding breaks for this ancestor")
      lhs.breakOwner = ancestor
      ancestor.addBreakInfo(lhs)

      if (ancestor.cchains.map{_.constPars.product}.product > 1) {
        warn(s"Note that break (${lhs.ctx}) does not necessarily break lanes")
        warn(s"    above the triggered lane, and does not")
        warn(s"    rewind modifications made in later pipeline stages")        
      }

    } else super.visit(lhs,rhs)
  }

}
