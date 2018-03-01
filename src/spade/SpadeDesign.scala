package spade

import core._
import nova.compiler.DSLApp
import nova.traversal._
import nova.traversal.codegen.dot._

trait SpadeDesign extends DSLApp {

  def runPasses[R](block: Block[R]): Unit = {
    lazy val printer = IRPrinter(state)
    lazy val irDotCodegen = IRDotCodegen(state)
    lazy val archDotCodegen = ArchDotCodegen(state)

    block ==>
      printer ==>
      irDotCodegen ==>
      archDotCodegen
  }

}
