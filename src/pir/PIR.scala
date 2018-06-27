package pir

import argon._
import argon.passes.IRPrinter
import pir.codegen.dot.{IRDotCodegen, PUDotCodegen}
import spatial.lang.Void

trait PIR extends Compiler {
  val desc: String = "PIR compiler"
  val script: String = "spatial"

  def main(): Void

  final def stageApp(args: Array[String]): Block[_] = stageBlock{ main() }

  def runPasses[R](block: Block[R]): Block[R] = {
    lazy val printer = IRPrinter(state, enable = config.enDbg)
    lazy val irDotCodegen = IRDotCodegen(state)
    lazy val puDotCodegen = PUDotCodegen(state)

    block ==>
      printer ==>
      puDotCodegen ==>
      irDotCodegen
  }
}
