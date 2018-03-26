package spade

import argon.{DSLApp, _}
import argon.passes.IRPrinter
import nova.codegen._
import nova.codegen.dot.{ArchDotCodegen, IRDotCodegen}

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
