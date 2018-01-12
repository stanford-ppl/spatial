package pcc.traversal
package transform

import pcc.core._

abstract class ForwardTransformer extends SubstTransformer with Traversal {
  final override val recurse = Recurse.Never

  /**
    * Determine a substitution rule for the given symbol.
    * By default, the rule is to mirror the symbol's node.
    * @return the symbol which should replace lhs
    */
  def transform[A:Sym](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    mirror(lhs,rhs)
  }

  /**
    * Determine a substitution or removal rule for the given symbol.
    * If the result is None, the symbol will be removed.
    * Otherwise, the given substitution rule will be registered.
    */
  def transformOrRemove[A:Sym](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Option[Sym[A]] = {
    Some(transform(lhs,rhs))
  }

  private def createSubstRule[A:Sym](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Unit = {
    val lhs2: Option[Sym[A]] = if (!subst.contains(lhs)) {
      // Untransformed case: no rule yet exists for this symbol
      val lhs2 = transformOrRemove(lhs, rhs)
      val lhs3 = subst.get(lhs).map(_.asInstanceOf[Sym[A]])
      (lhs2, lhs3) match {
        case (Some(s2), Some(s3)) if s2 != s3 =>
          throw new Exception(s"Illegal substitution: $lhs had rule $lhs -> $lhs3 when creating rule $lhs -> $lhs2")
        case (Some(s2), _) => Some(s2)
        case (_, Some(s3)) => Some(s3)
        case (None, None)  => None
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
      val lhs2: Sym[A] = f(lhs)
      val lhs3: Sym[A] = mirrorSym(lhs)
      if (lhs3 != lhs2 && lhs != lhs2) removeSym(lhs2)
      Some(lhs3)
    }
    lhs2.foreach{sub => if (sub != lhs) register(lhs -> sub) }
  }

  /**
    * Visit and transform each statement in the given block.
    * @return the substitution for the block's result
    */
  override protected def inlineBlock[T](block: Block[T]): Sym[T] = {
    inlineBlockWith(block){stms => stms.foreach(visit); f(block.result) }
  }

  /**
    * Visit and perform some transformation `func` over all statements in the block.
    * @return the substitution for the block's result
    */
  final protected def inlineBlockWith[T](block: Block[T])(func: Seq[Sym[_]] => Sym[T]): Sym[T] = {
    state.logTab += 1
    val result = func(block.stms)
    state.logTab -= 1
    result
  }

  final override protected def visit(lhs: Sym[_], rhs: Op[_]): Unit = {
    createSubstRule(lhs.asInstanceOf[Sym[Any]], rhs.asInstanceOf[Op[Any]])(mtyp(lhs), lhs.ctx)
  }

  final override protected def visitBlock[R](block: Block[R]): Block[R] = {
    state.logTab += 1
    val block2 = transformBlock(block)
    state.logTab -= 1
    block2
  }

  override protected def preprocess[S](block: Block[S]): Block[S] = {
    subst = Map.empty
    state.defCache = Map.empty
    super.preprocess(block)
  }

  final protected def mirrorSym[A](sym: Sym[A]): Sym[A] = sym match {
    case Op(rhs) => mirror(sym,rhs)
    case _ => sym
  }

  final protected def removeSym(sym: Sym[_]): Unit = {
    state.context = state.context.filterNot(_ == sym)
  }
}
