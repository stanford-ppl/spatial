package pcc
import ir.Void

abstract class App extends Compiler with StaticTraversals {

  def main(): Void

  def stageProgram(args: Array[String]): Block[_] = {
    val block = stageBlock{ main() }
    block
  }

}
