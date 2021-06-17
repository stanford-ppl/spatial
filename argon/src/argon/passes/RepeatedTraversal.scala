package argon.passes
import argon.Block


trait RepeatableTraversal extends Traversal {
  var converged: Boolean = true
}

case class RepeatedTraversal(IR: argon.State, passes: Seq[Traversal]) extends Pass {
  private def hasConverged: Boolean = {
    passes forall {
      case rt: RepeatableTraversal =>
        rt.converged
      case _: Traversal => true
    }
  }

  private def resetConvergence(status: Boolean): Unit ={
    passes foreach {
      case rt: RepeatableTraversal =>
        rt.converged = status
      case _ =>
    }
  }

  override def process[R](block: Block[R]): Block[R] = {
    var blk = block
    // run all passes at least once
    do {
      resetConvergence(true)
      passes foreach {
        pass =>
          blk = pass.run(block)
      }
    } while (!hasConverged)
    blk
  }
}
