package argon
package passes

/**
  * Single traversal of the IR with pre- and post- processing
  */
trait Traversal extends Pass { self =>
  object Recurse extends Enumeration {
    type Recurse = Value
    // TODO[5]: Breadth-first recursive search mode
    val Always, Default, Never = Value
  }
  type Recurse = Recurse.Recurse
  import Recurse._

  // --- Options
  val recurse: Recurse = Default   // Recursive traversal of IR hierarchy
  protected var backend = ""
  
  // --- Methods

  /** Called to run the main part of this traversal. */
  override protected def process[R](block: Block[R]): Block[R] = visitBlock(block)

  /** Visits the statements in the block with the given visit function. */
  final protected def visitWith[R,A](block: Block[R])(func: Seq[Sym[_]] => A): A = {
    state.logTab += 1
    val result = func(block.stms)
    state.logTab -= 1
    result
  }

  protected def visitBlock[R](block: Block[R]): Block[R] = {
    visitWith(block){stms => stms.foreach(visit); block }
  }

  final protected def visit(lhs: Sym[Any]): Unit = lhs.op match {
    case Some(rhs) =>
      visit(lhs, rhs)
      if (recurse == Always) rhs.blocks.foreach{blk => visitBlock(blk) }
    case _ => // Do nothing
  }

  protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    if (recurse == Default) rhs.blocks.foreach{blk => visitBlock(blk) }
  }

}
