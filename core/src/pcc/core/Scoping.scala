package pcc.core

import forge._
import pcc.data.{Effects,Effectful}

import scala.collection.mutable

trait Scoping { this: Printing =>

  /**
    * Computes an *external* summary for a sequence of nodes
    * (Ignores reads/writes on data allocated within the scope)
    */
  @stateful def summarizeScope(context: Seq[Sym[_]]): Effects = {
    var effects: Effects = Effects.Pure
    val allocs = new mutable.HashSet[Sym[_]]
    def clean(xs: Set[Sym[_]]) = xs diff allocs
    for (s@Effectful(u2, _) <- context) {
      if (u2.isMutable) allocs += s
      effects = effects andThen u2.copy(reads = clean(u2.reads), writes = clean(u2.writes))
    }
    effects
  }

  /**
    * Stage the effects of an isolated block.
    * No assumptions about the current context remain valid.
    * TODO: Add code motion
    */
  @stateful private def stageScope[R](inputs: Seq[Sym[_]], block: => Sym[R], options: BlockOptions): Block[R] = {
    if (state == null) throw new Exception("Null state during stageScope")

    val saveContext = state.context
    val saveCache = state.defCache
    // In an isolated or sealed blocks, don't allow CSE with outside statements
    // CSE with outer scopes should only occur if symbols are not allowed to escape,
    // which isn't true in either of these cases
    state.defCache = Map.empty
    state.context = Nil

    val result = block
    val impure = state.context.collect{case sym@Effectful(eff,_) if !eff.isPure => sym }
    val stms   = state.context.reverse

    state.context = saveContext

    // Reset contents of defCache
    // -- prevents CSEing across inescapable blocks
    state.defCache = saveCache

    val effects = summarizeScope(impure)

    Block[R](inputs,stms,result,effects,impure,options)
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
