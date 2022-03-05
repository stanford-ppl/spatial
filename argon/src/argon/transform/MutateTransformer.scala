package argon
package transform

import argon.node.Enabled
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

  protected var enables: Set[argon.lang.Bit] = Set.empty
  protected def withEns[T](ens: Set[argon.lang.Bit])(thunk: => T): T = {
    val tEnables = enables
    enables = enables ++ ens
    val result = thunk
    enables = tEnables
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

  final override protected def blockToFunction0[R](b: Block[R]): () => R = {
    () => isolateSubst(){
      inCopyMode(copy = true){ inlineBlock(b).unbox }
    }
  }

  /** Mutate this symbol's node with the current substitution rules. */
  final def update[A](lhs: Sym[A], rhs: Op[A]): Sym[A] = if (copyMode) mirror(lhs,rhs) else {
    implicit val ctx: SrcCtx = lhs.ctx
    try {
      updateNode(rhs)
      restageWithFlow(lhs){lhs2 => transferDataIfNew(lhs, lhs2) }
    }
    catch {case t: Throwable =>
      bug("Encountered exception while updating node")
      bug(s"$lhs = $rhs")
      throw t
    }
  }

  def updateNode[A](node: Op[A]): Unit = node match {
    case enabled: Enabled[_] =>
      enabled.updateEn(f, f(enables))
    case _ =>
      node.update(f)
  }

  override def mirrorNode[A](rhs: Op[A]): Op[A] = rhs match {
    case en:Enabled[A] => en.mirrorEn(f, f(enables))
    case _ => super.mirrorNode(rhs)
  }
}
