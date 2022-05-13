package argon
package transform

import argon.passes.Traversal
import utils.tags.instrument

abstract class ForwardTransformer extends SubstTransformer with Traversal {
  override val recurse = Recurse.Never

  /** Determine a substitution rule for the given symbol.
    *
    * Options when transforming a statement:
    *   0. Remove it: s -> Invalid. Statement will not appear in resulting graph, uses are disallowed.
    *   1. Mirror it: s -> s'. Substitution s' will be a copy of s with updated references
    *   2. Subst. it: s -> s'. Substitution s' will be swapped for s by all consumers.
    *
    * By default, the rule is to mirror the symbol's node.
    *
    * @return the symbol which should replace lhs
    */
  def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    mirror(lhs,rhs)
  }

  final protected def createSubstRule[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Unit = {
    val lhs2: Sym[A] = if (!subst.contains(lhs)) {
      // Untransformed case: no rule yet exists for this symbol
      val lhs2 = transform(lhs, rhs)
      subst.get(lhs) match {
        case Some(DirectSubst(lhs3)) if lhs2 != lhs3 =>
          throw new Exception(s"Conflicting substitutions: $lhs had rule $lhs -> $lhs3 when creating rule $lhs -> $lhs2")
        case _ => lhs2
      }
    }
    else {
      // Pre-transformed case
      // Case 1: Multiple traversals of same symbol in different scopes
      //   Can occur due to CSE across two scopes, for example.
      //   Action: Keep substitution rule
      // Case 2: Transformer has already visited this scope once
      //   Can occur if some higher scope pre-transformed this block
      //   Action: Mirror the existing symbol, scrub previous substitution from context
      //   to avoid having it show up in effects summaries.
      dbgs(s"$lhs already had substitution rule ${f(lhs)}!")

      val lhs2: Sym[A] = f(lhs)
      val lhs3: Sym[A] = mirrorSym(lhs2)
      if (lhs3 != lhs2 && lhs != lhs2) removeSym(lhs2)
      lhs3
    }
    if (lhs2 != lhs) register(lhs -> lhs2)
  }

  /** Perform some default transformation over all statements in block b as part of the current scope.
    * @return the transformed version of the block's result.
    */
  protected def inlineBlock[T](b: Block[T]): Sym[T] = inlineWith(b){stms =>
    stms.foreach(visit)
    f(b.result)
  }

  final override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    implicit val ctx: SrcCtx = lhs.ctx
    implicit val typ: Type[A] = lhs.tp
    createSubstRule(lhs, rhs)
  }

  final override protected def visitBlock[R](block: Block[R]): Block[R] = {
    state.logTab += 1
    val block2 = substituteBlock(block)
    state.logTab -= 1
    block2
  }


}
