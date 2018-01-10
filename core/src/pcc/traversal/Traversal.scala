package pcc
package traversal

import util.Tri._

/**
  * Single traversal of the IR with pre- and post- processing
  */
trait Traversal extends Pass { self =>
  object Recurse extends Enumeration {
    type Recurse = Value
    val Always, Default, Never = Value
  }
  type Recurse = Recurse.Recurse
  import Recurse._

  // --- Options
  val recurse: Recurse = Default   // Recursive traversal of IR hierarchy

  // --- Methods
  /** Run a single traversal, including pre- and post- processing **/
  final protected def runSingle[S](b: Block[S]): Block[S] = {
    val b2 = preprocess(b)
    val b3 = visitBlock(b2)
    postprocess(b3)
  }

  /**
    * Called to execute this traversal, including optional pre- and post- processing.
    * Default is to run pre-processing, then a single traversal, then post-processing
    */
  protected def process[S](block: Block[S]): Block[S] = runSingle(block)
  protected def preprocess[S](block: Block[S]): Block[S] = { block }
  protected def postprocess[S](block: Block[S]): Block[S] = { block }
  protected def visitBlock[S,R](block: Block[S], func: Seq[Sym[_]] => R): R = {
    state.logTab += 1
    val result = func(block.stms)
    state.logTab -= 1
    result
  }
  protected def visitBlock[S](block: Block[S]): Block[S] = visitBlock(block, {stms => stms.foreach(visit); block})

  final protected def visit(lhs: Sym[_]): Unit = lhs.rhs match {
    case Two(rhs) =>
      visit(lhs, rhs)
      if (recurse == Always) rhs.blocks.foreach{blk => visitBlock(blk) }
    case _ => // Do nothing
  }

  protected def visit(lhs: Sym[_], rhs: Op[_]): Unit = {
    if (recurse == Default) rhs.blocks.foreach{blk => visitBlock(blk) }
  }

}
