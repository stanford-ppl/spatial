package pcc.traversal

import pcc.core._
import pcc.util.Tri._

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
  final protected def runSingle[R](b: Block[R]): Block[R] = {
    val b2 = preprocess(b)
    val b3 = visitBlock(b2)
    postprocess(b3)
  }

  /**
    * Called to execute this traversal, including optional pre- and post- processing.
    * Default is to run pre-processing, then a single traversal, then post-processing
    */
  protected def process[R](block: Block[R]): Block[R] = runSingle(block)
  protected def preprocess[R](block: Block[R]): Block[R] = { block }
  protected def postprocess[R](block: Block[R]): Block[R] = { block }
  protected def visitBlock[R,A](block: Block[R], func: Seq[Sym[_]] => A): A = {
    state.logTab += 1
    val result = func(block.stms)
    state.logTab -= 1
    result
  }
  protected def visitBlock[R](block: Block[R]): Block[R] = visitBlock(block, {stms => stms.foreach(visit); block})

  final protected def visit(lhs: Sym[_]): Unit = lhs.op match {
    case Some(rhs) =>
      visit(lhs, rhs)
      if (recurse == Always) rhs.blocks.foreach{blk => visitBlock(blk) }
    case _ => // Do nothing
  }

  protected def visit(lhs: Sym[_], rhs: Op[_]): Unit = {
    if (recurse == Default) rhs.blocks.foreach{blk => visitBlock(blk) }
  }

}
