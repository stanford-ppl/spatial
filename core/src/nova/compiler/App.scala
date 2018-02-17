package nova.compiler

import nova.core._
import nova.lang.Void

abstract class App extends StaticTraversals {

  def main(): Void

  def stage(args: Array[String]): Block[_] = {
    val block = stageBlock{ main() }
    block
  }

}
