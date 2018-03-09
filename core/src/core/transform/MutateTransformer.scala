package core
package transform

abstract class MutateTransformer extends ForwardTransformer {
  override val recurse = Recurse.Default
  override val allowOldSymbols: Boolean = true

  /*
   * Options when transforming a statement:
   *   0. Remove it: s -> None. Statement will not appear in resulting graph.
   *   1. Update it: s -> Some(s). Statement will not change except for inputs.
   *   2. Subst. it: s -> Some(s'). Substitution s' will appear instead.
   */

  /**
    * Determine a transformation rule for the given symbol.
    * By default, the rule is to update the symbol's node in place.
    * @return the symbol which should replace lhs
    */
  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    update(lhs,rhs)
  }

  /**
    * Update the metadata on sym using current substitution rules
    * NOTE: We don't erase invalid metadata here to avoid issues with Effects, etc.
    */
  def updateMetadata(sym: Sym[_]): Unit = {
    val data = metadata.all(sym).flatMap{case (k,m) => mirror(m) : Option[Metadata[_]] }
    metadata.addAll(sym, data)
  }

  /** Mutate this symbol's node with the current substitution rules. */
  def update[A](lhs: Sym[A], rhs: Op[A]): Sym[A] = {
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

  /**
    * Visit and transform each statement in the given block.
    * @return the substitution for the block's result
    */
  override protected def inlineBlock[T](block: Block[T]): Sym[T] = {
    inlineBlockWith(block){stms => stms.foreach(visit); f(block.result) }
  }

}
