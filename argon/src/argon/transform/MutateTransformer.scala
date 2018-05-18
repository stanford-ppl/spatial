package argon
package transform

import utils.tags.instrument

abstract class MutateTransformer extends ForwardTransformer {
  override val recurse = Recurse.Default

  /** Determines whether the default transform rule is to mirror (copy) or update nodes. */
  protected var copyMode: Boolean = false

  protected def inCopyMode[A](copy: Boolean)(block: => A): A = {
    val saveCopy = copyMode
    copyMode = copy
    val result = block
    copyMode = saveCopy
    result
  }

  /** Transformation rule for the given symbol.
    *
    * Options when transforming a statement:
    *   0. Remove it: s -> Invalid. Statement will not appear in resulting graph, uses are disallowed.
    *   1. Update it: s -> s. Statement will not change (but inputs may be updated).
    *   2. Subst. it: s -> s'. Substitution s' will be used by consumers instead.
    *
    * By default, the rule is to update the symbol's node in place.
    * @return the symbol which should replace lhs
    */
  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    update(lhs,rhs)
  }

  /** Update the metadata on sym using current substitution rules. */
  def updateMetadata(sym: Sym[_]): Unit = {
    metadata.all(sym).toList.foreach{case (k,m) => mirror(m) match {
      case Some(m2) => if (!m.ignoreOnTransform) metadata.add(sym, k, merge(m, m2))
      case None     => metadata.remove(sym, k)
    }}
  }

  final override protected def blockToFunction0[R](b: Block[R]): () => R = {
    () => isolate(){
      inCopyMode(copy = true){ inlineBlock(b).unbox }
    }
  }

  /** Mutate this symbol's node with the current substitution rules. */
  final def update[A](lhs: Sym[A], rhs: Op[A]): Sym[A] = if (copyMode) mirror(lhs,rhs) else {
    implicit val ctx: SrcCtx = lhs.ctx
    //logs(s"$lhs = $rhs [Update]")
    try {
      updateNode(rhs)
      val lhs2 = restage(lhs)
      // TODO[5]: Small inefficiency where metadata created through flows is immediately mirrored
      if (lhs2 == lhs) updateMetadata(lhs2)
      lhs2
    }
    catch {case t: Throwable =>
      bug("Encountered exception while updating node")
      bug(s"$lhs = $rhs")
      throw t
    }
  }

  def updateNode[A](node: Op[A]): Unit = node.update(f)

}
