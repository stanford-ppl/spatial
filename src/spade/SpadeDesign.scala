package spade

import core._
import core.passes.IRPrinter
import nova.compiler.DSLApp
import nova.codegen._
import nova.codegen.dot.{IRDotCodegen,ArchDotCodegen}

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
