package pir

import argon.{DSLApp, _}
import argon.passes.IRPrinter
import nova.codegen.dot.{IRDotCodegen, PUDotCodegen}

trait PIRApp extends DSLApp {

  def runPasses[R](block: Block[R]): Unit = {
    lazy val printer = IRPrinter(state)
    lazy val irDotCodegen = IRDotCodegen(state)
    lazy val puDotCodegen = PUDotCodegen(state)

    block ==>
      printer ==>
      puDotCodegen ==>
      irDotCodegen
  }

}
