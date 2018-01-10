package pcc

import traversal.IRPrinter
import traversal.analysis._
import traversal.transform._

trait StaticTraversals extends Compiler {

  override def createTraversalSchedule(state: State): Unit = {
    lazy val printer = IRPrinter(state)
    lazy val levelAnalyzer = ControlAnalyzer(state)

    passes += printer
    passes += levelAnalyzer
    passes += printer
  }

}
