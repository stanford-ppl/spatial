package pcc.compiler

import pcc.core._
import pcc.node.Rewrites
import pcc.traversal._
import pcc.traversal.analysis._
import pcc.traversal.transform._

trait StaticTraversals extends Compiler {

  override def createTraversalSchedule(state: State): Unit = {
    val rewrites = new Rewrites {}

    lazy val printer = IRPrinter(state)
    lazy val levelAnalyzer = ControlAnalyzer(state)
    lazy val identity = IdentityTransform(state)
    lazy val pipeInserter = PipeInserter(state)

    passes += printer
    passes += levelAnalyzer
    passes += printer
    passes += identity
    passes += printer
    passes += pipeInserter
    passes += printer
  }

}
