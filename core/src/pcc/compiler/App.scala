package pcc.compiler

import pcc.core._
import pcc.lang.Void

abstract class App extends Compiler with StaticTraversals {

  def main(): Void

  def stageProgram(args: Array[String]): Block[_] = {
    val block = stageBlock{ main() }
    block
  }

}
