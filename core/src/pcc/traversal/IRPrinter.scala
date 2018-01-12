package pcc.traversal

import pcc.core._
import pcc.util.strMeta

case class IRPrinter(IR: State) extends Traversal {
  override val name: String = "IR Printer"

  private def printBlocks(lhs: Sym[_], blocks: Seq[Block[_]]): Unit = blocks.zipWithIndex.foreach{case (blk,i) =>
    state.logTab += 1
    dbgs(s"block $i: $blk {")
    state.logTab += 1
    dbgs(s"effects:  ${blk.effects}")
    dbgs(s"impure:   ${blk.impure}")
    dbgs(s"isolated: ${blk.options.isol}")
    dbgs(s"sealed:   ${blk.options.seal}")
    visitBlock(blk)
    state.logTab -= 1
    dbgs(s"} // End of $lhs block #$i")
    state.logTab -= 1
  }

  override protected def visit(lhs: Sym[_], rhs: Op[_]): Unit = {
    if (rhs.blocks.nonEmpty) dbgs(s"$lhs = $rhs {") else dbgs(s"$lhs = $rhs")
    strMeta(lhs)

    printBlocks(lhs, rhs.blocks)

    if (rhs.blocks.nonEmpty) dbgs(s"} // End of $lhs")
  }
}
