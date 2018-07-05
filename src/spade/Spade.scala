package spade

import argon._
import argon.passes.IRPrinter
import pir.codegen._
import pir.codegen.dot.{ArchDotCodegen, IRDotCodegen}
import spatial.lang.Void

trait Spade extends Compiler {
  val desc: String = "Spade"
  val script: String = "spade"

  def main(): Void

  final def stageApp(args: Array[String]): Block[_] = stageBlock{ main() }

  def runPasses[R](block: Block[R]): Block[R] = {
    lazy val printer = IRPrinter(state, enable = config.enDbg)
    lazy val irDotCodegen = IRDotCodegen(state)
    lazy val archDotCodegen = ArchDotCodegen(state)

    block ==>
      printer ==>
      irDotCodegen ==>
      archDotCodegen
  }
}
