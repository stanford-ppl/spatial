package spatial

import core._
import nova.compiler.DSLApp
import nova.poly.ISL
import nova.traversal._
import nova.traversal.analysis._
import nova.traversal.codegen.dot._
import nova.traversal.transform._

trait SpatialApp extends DSLApp {

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

    isl.shutdown(100)
  }

}
