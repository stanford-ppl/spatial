package pcc.compiler

import pcc.core._
import pcc.traversal._
import pcc.traversal.analysis._
import pcc.traversal.transform._

trait StaticTraversals extends Compiler {

  override def createTraversalSchedule(state: State): Unit = {
    lazy val printer = IRPrinter(state)
    lazy val levelAnalyzer = ControlAnalyzer(state)
    lazy val identity = IdentityTransform(state)

    passes += printer
    passes += levelAnalyzer
    passes += printer
    passes += identity
    passes += printer
  }

}
