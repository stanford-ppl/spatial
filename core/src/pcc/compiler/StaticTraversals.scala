package pcc.compiler

import pcc.core._
import pcc.data.FlowRules
import pcc.rewrites.RewriteRules
import pcc.traversal._
//import pcc.traversal.analysis._
import pcc.traversal.transform._

trait StaticTraversals extends Compiler {
  val rewrites = new RewriteRules {}
  val flows = new FlowRules {}

  override def createTraversalSchedule(state: State): Unit = {
    lazy val printer = IRPrinter(state)
//    lazy val identity = IdentityTransform(state)
    lazy val pipeInserter = PipeInserter(state)
    lazy val globalAllocation = GlobalAllocation(state)

    passes += printer
    passes += pipeInserter
    passes += printer
    passes += globalAllocation
    passes += printer
  }

}
