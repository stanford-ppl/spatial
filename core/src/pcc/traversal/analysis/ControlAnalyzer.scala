package pcc
package traversal
package analysis

import pcc.ir.control._
import pcc.data._

case class ControlAnalyzer(IR: State) extends Traversal {
  override val name = "Control Analyzer"

  // Returns true if this is a controller
  private def annotateControl(lhs: Sym[_], rhs: Op[_]): Boolean = {
    val mapping = rhs.blocks.map{blk =>
      val hasCtrl = visitBlock(blk, {stms =>
        stms.exists{case Stm(l,r) => annotateControl(l,r); case _ => false }
      })
      blk -> hasCtrl
    }

    val isOut = mapping.map(_._2).fold(false){_||_}
    dbg(s"$lhs = $rhs [Outer:$isOut]")

    if (isControl(rhs)) isOuter(lhs) = isOut
    isControl(lhs)
  }

  override protected def visit(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Accel(_) => annotateControl(lhs, rhs)
    case _ => super.visit(lhs,rhs)
  }

}
