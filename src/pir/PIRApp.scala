package pir

import core._
import nova.compiler.DSLApp
import nova.traversal._
import nova.traversal.codegen.dot._

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
