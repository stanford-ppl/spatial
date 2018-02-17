package nova.compiler

import nova.core._
import nova.data.FlowRules
import nova.poly.ISL

import nova.rewrites.RewriteRules
import nova.traversal._
import nova.traversal.analysis._
import nova.traversal.transform._
import nova.traversal.codegen.dot._

abstract class StaticTraversals extends Compiler {
  new RewriteRules {}
  new FlowRules {}

  val isPIR = false
  val isArchModel = false

  private implicit class BlockOps[R](block: Block[R]) {
    def ==>(pass: Pass): Block[R] = runPass(pass, block)
    def ==>(pass: (Boolean,Pass)): Block[R] = if (pass._1) runPass(pass._2,block) else block
  }
  private implicit class BooleanOps(x: Boolean) {
    def ?(pass: Pass): (Boolean,Pass) = (x,pass)
  }

  def runPasses[R](block: Block[R]): Unit = {
    implicit val isl: ISL = ISL()
    isl.startup()

    lazy val printer = IRPrinter(state)
    lazy val pipeInserter = PipeInserter(state)
    lazy val accessAnalyzer = AccessAnalyzer(state)
    lazy val memoryAnalyzer = MemoryAnalyzer(state)

    lazy val globalAllocation = GlobalAllocation(state)
    lazy val irDotCodegen = IRDotCodegen(state)
    lazy val puDotCodegen = PUDotCodegen(state)
    lazy val archDotCodegen = ArchDotCodegen(state)

    if (isPIR && !isArchModel) {
      block ==>
        printer ==>
        puDotCodegen ==>
        irDotCodegen
    }
    else if (isArchModel) {
      block ==>
        printer ==>
        irDotCodegen ==>
        archDotCodegen
    }
    else {
      block ==>
        printer ==>
        pipeInserter ==>
        printer ==>
        accessAnalyzer ==>
        printer ==>
        memoryAnalyzer ==>
        globalAllocation ==>
        printer ==>
        puDotCodegen ==>
        irDotCodegen
    }

    isl.shutdown(100)
  }

}
