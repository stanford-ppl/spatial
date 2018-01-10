package pcc
package traversal
package analysis

import pcc.ir.control._
import pcc.data._

case class ControlAnalyzer(IR: State) extends Traversal {
  override val name = "Control Analyzer"

  private def annotateControl(lhs: Sym[_], rhs: Op[_]): Boolean = {
    val mapping = rhs.blocks.map{blk =>
      blk -> visitBlock(blk, {stms =>
        stms.exists{case Stm(l,r) => annotateControl(l,r); case _ => false }
      })
    }.toMap

    val isOut = mapping.values.fold(false){_||_}
    dbg(s"$lhs = $rhs [Outer:$isOut]")

    if (isControl(rhs)) isOuter(lhs) = isOut
    isControl(lhs) && isOut
  }

  override protected def visit(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Accel(_) => annotateControl(lhs, rhs)
    case _ => super.visit(lhs,rhs)
  }

}
