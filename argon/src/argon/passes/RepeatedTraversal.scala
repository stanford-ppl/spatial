package argon.passes
import argon._


trait RepeatableTraversal extends Traversal {
  var converged: Boolean = true
}

case class RepeatedTraversal(IR: argon.State, passes: Seq[Pass], postIter: (Int) => Seq[Pass] = (x:Int) => {Seq.empty}, maxIters: Int = 100) extends Pass {
  private def hasConverged: Boolean = {
    passes forall {
      case rt: RepeatableTraversal =>
        dbgs(s"Has Converged: ${rt.getClass} = ${rt.converged}")
        rt.converged
      case _: Pass => true
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
    var iters = 0
    // run all passes at least once
    do {
      resetConvergence(true)
      dbgs(s"Starting Iteration: $iters")
      iters += 1
      indent {
        (passes ++ postIter(iters)) foreach {
          pass =>
            dbgs(s"Starting Pass: $pass")
            blk = pass.run(block)
            dbgs(s"Ending Pass: $pass")
        }
      }
    } while (!hasConverged && iters < maxIters)
    blk
  }
}
