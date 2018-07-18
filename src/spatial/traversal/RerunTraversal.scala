package spatial.traversal

import argon._
import argon.passes.Traversal

trait RerunTraversal extends Traversal {
  protected var isRerun: Boolean = false

  def rerun(sym: Sym[_], block: Block[_]): Unit = {
    isRerun = true
    visitBlock(block)
    isRerun = false
  }
}
