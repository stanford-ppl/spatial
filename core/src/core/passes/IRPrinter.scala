package core.passes

import core._

case class IRPrinter(IR: State) extends Traversal {
  override val name = "IR"

  override protected def postprocess[R](block: Block[R]): Block[R] = {
    dbgs("")
    dbgs(s"Global Metadata")
    dbgs(s"---------------")
    globals.foreach{(k,v) => dbgs(s"$k: $v") }
    super.postprocess(block)
  }

  private def printBlocks(lhs: Sym[_], blocks: Seq[Block[_]]): Unit = blocks.zipWithIndex.foreach{case (blk,i) =>
    state.logTab += 1
    dbgs(s"block $i: $blk {")
    state.logTab += 1
      dbgs(s"effects:  ${blk.effects}")
    visitBlock(blk)
    state.logTab -= 1
    dbgs(s"} // End of $lhs block #$i")
    state.logTab -= 1
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    if (rhs.blocks.nonEmpty) dbgs(s"$lhs = $rhs {") else dbgs(s"$lhs = $rhs")
    strMeta(lhs)

    printBlocks(lhs, rhs.blocks)

    if (rhs.blocks.nonEmpty) dbgs(s"} // End of $lhs")
  }
}
