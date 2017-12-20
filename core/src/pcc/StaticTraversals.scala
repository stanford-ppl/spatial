package pcc

import traversal.IRPrinter

trait StaticTraversals extends Compiler {

  override def createTraversalSchedule(state: State): Unit = {
    lazy val printer = IRPrinter(state)

    passes += printer
  }

}
