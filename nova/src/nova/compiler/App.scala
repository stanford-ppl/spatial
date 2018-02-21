package nova.compiler

import core._
import spatial.lang.Void

abstract class App extends StaticTraversals {

  def main(): Void

  def stage(args: Array[String]): Block[_] = {
    val block = stageBlock{ main() }
    block
  }

}
