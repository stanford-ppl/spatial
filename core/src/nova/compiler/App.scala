package nova.compiler

import nova.core._
import nova.lang.Void

abstract class App extends Compiler with StaticTraversals {

  def main(): Void

  def stageProgram(args: Array[String]): Block[_] = {
    val block = stageBlock{ main() }
    block
  }

}
