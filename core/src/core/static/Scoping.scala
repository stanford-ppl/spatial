package core
package static

import core.schedule._
import forge.tags._

trait Scoping {

  /** Stage the effects of an isolated scope with the given inputs.
    *
    * @param inputs Bound inputs to this block (for lambda functions)
    * @param block Call by name reference to the scope
    * @param options Scheduling options for the scope (default is BlockOptions.Normal)
    * @param scheduler Scheduler used to order statements (default is SimpleScheduler)
    */
  @stateful def stageScope[R](
    inputs:    Seq[Sym[_]],
    block:     => Sym[R],
    options:   BlockOptions = BlockOptions.Normal,
    scheduler: Scheduler = SimpleScheduler
  ): Block[R] = {
    if (state == null) throw new Exception("Null state during stageScope")

    val saveImpure = state.impure
    val saveScope  = state.scope
    val saveCache  = state.cache
    // In an isolated or sealed blocks, don't allow CSE with outside statements
    // CSE with outer scopes should only occur if symbols are not allowed to escape,
    // which isn't true in either of these cases
    state.scope  = Vector.empty
    state.impure = Vector.empty
    state.cache  = Map.empty

    val result = block
    val scope  = state.scope
    val impure = state.impure
    val motion = saveScope != null
    val sched = scheduler(inputs,result,scope,impure,options,motion)

    state.cache  = saveCache

    state.scope  = saveScope
    state.impure = saveImpure
    if (motion) {
      state.scope ++= sched.motioned
      state.impure ++= sched.motionedImpure
    }

    sched.block
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
