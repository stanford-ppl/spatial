package pcc.compiler

import pcc.core._
import pcc.data.FlowRules
import pcc.rewrites.RewriteRules
import pcc.traversal._
import pcc.traversal.analysis._
import pcc.traversal.transform._
import pcc.traversal.codegen.dot._

trait StaticTraversals extends Compiler {
  val rewrites = new RewriteRules {}
  val flows = new FlowRules {}

  val isPIR = false

  def runPasses[R](block: Block[R]): Unit = {
    lazy val printer = IRPrinter(state)
    lazy val pipeInserter = PipeInserter(state)
    lazy val accessAnalyzer = AccessAnalyzer(state)

    lazy val globalAllocation = GlobalAllocation(state)
    lazy val irDotCodegen = IRDotCodegen(state)
    lazy val puDotCodegen = PUDotCodegen(state)

    block ==>
      printer ==>
      (!isPIR ? pipeInserter) ==>
      (!isPIR ? printer) ==>
      accessAnalyzer ==>
      (!isPIR ? globalAllocation) ==>
      (!isPIR ? printer) ==>
      (isPIR ? irDotCodegen) ==>
      (isPIR ? puDotCodegen)
  }

}
