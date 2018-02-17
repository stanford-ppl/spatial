package nova.core.static

import forge.tags._

import scala.collection.mutable

trait Scoping { this: Printing =>

  /**
    * Computes an *external* summary for a sequence of nodes
    * (Ignores reads/writes on data allocated within the scope)
    */
  @stateful def summarizeScope(impure: Seq[Impure]): Effects = {
    var effects: Effects = Effects.Pure
    val allocs = mutable.HashSet[Sym[_]]()
    val reads  = mutable.HashSet[Sym[_]]()
    val writes = mutable.HashSet[Sym[_]]()
    impure.foreach{case Impure(s,eff) =>
      if (eff.isMutable) allocs += s
      reads ++= eff.reads
      writes ++= eff.writes
      effects = effects andThen eff
    }
    effects.copy(reads = effects.reads diff allocs, writes = effects.writes diff allocs, antideps = impure)
  }

  /**
    * Stage the effects of an isolated block.
    * No assumptions about the current context remain valid.
    * TODO: Add code motion
    */
  @stateful private def stageScope[R](inputs: Seq[Sym[_]], block: => Sym[R], options: BlockOptions): Block[R] = {
    if (state == null) throw new Exception("Null state during stageScope")

    val saveImpure = state.impure
    val saveScope  = state.scope
    val saveCache  = state.cache
    // In an isolated or sealed blocks, don't allow CSE with outside statements
    // CSE with outer scopes should only occur if symbols are not allowed to escape,
    // which isn't true in either of these cases
    state.scope  = Nil
    state.impure = Nil
    state.cache  = Map.empty

    val result = block
    val impure = state.impure
    val scope  = state.scope.reverse

    state.scope  = saveScope
    state.cache  = saveCache    // prevents CSEing across inescapable blocks
    state.impure = saveImpure

    val effects = summarizeScope(impure)
    logs(s"Closing scope with result $result")
    logs(s"Effects: $effects")

    Block[R](inputs,scope,result,effects,options)
  }

  @stateful def stageBlock[R](block: => Sym[R], options: BlockOptions = BlockOptions.Normal): Block[R] = {
    stageScope(Nil, block, options)
  }
  @stateful def stageLambda1[A,R](a: Sym[A])(block: => Sym[R], options: BlockOptions = BlockOptions.Normal): Block[R] = {
    stageScope(Seq(a), block, options)
  }
  @stateful def stageLambda2[A,B,R](a: Sym[A], b: Sym[B])(block: => Sym[R], options: BlockOptions = BlockOptions.Normal): Block[R] = {
    stageScope(Seq(a,b), block, options)
  }
  @stateful def stageLambda3[A,B,C,R](a: Sym[A], b: Sym[B], c: Sym[C])(block: => Sym[R], options: BlockOptions = BlockOptions.Normal): Block[R] = {
    stageScope(Seq(a,b,c), block, options)
  }
  @stateful def stageLambdaN[R](inputs: Seq[Sym[_]])(block: => Sym[R], options: BlockOptions = BlockOptions.Normal): Block[R] = {
    stageScope(inputs, block, options)
  }

}
