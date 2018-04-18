package argon
package static

import argon.schedule._
import forge.tags._
import utils.tags.instrument

trait Scoping {

  @stateful @inline private def reify[R](block: => Sym[R]): Sym[R] = {
    if (state.isStaging) state.logTab += 1
    val result = block
    if (state.isStaging) state.logTab -= 1
    result
  }

  @inline private def schedule[R](
    inputs: Seq[Sym[_]],
    result: Sym[R],
    scope:  Seq[Sym[_]],
    impure: Seq[Impure],
    options: BlockOptions,
    motion: Boolean,
    scheduler: Scheduler
  ): Schedule[R] = scheduler(inputs,result,scope,impure,options,motion)

  /** Stage the effects of an isolated scope with the given inputs.
    *
    * @param inputs Bound inputs to this block (for lambda functions)
    * @param block Call by name reference to the scope
    * @param options Scheduling options for the scope (default is BlockOptions.Normal)
    */
  @stateful def stageScope[R](
    inputs:  Seq[Sym[_]],
    options: BlockOptions = BlockOptions.Normal
  )(
    block:   => Sym[R],
  ): Block[R] = {
    // TODO[2]: Add code motion scheduler when enabled
    lazy val defaultSched = if (state.mayMotion) SimpleScheduler else SimpleScheduler
    val scheduler = options.sched.getOrElse(defaultSched)
    if (state eq null) throw new Exception("Null state during stageScope")

    val saveImpure = state.impure
    val saveScope  = state.scope
    val saveCache  = state.cache
    val motion = saveScope != null && (scheduler.mustMotion || state.mayMotion)
    // In an isolated or sealed blocks, don't allow CSE with outside statements
    // CSE with outer scopes should only occur if symbols are not allowed to escape,
    // which isn't true in either of these cases
    state.newScope(motion)

    val result = reify(block)
    val scope  = state.scope
    val impure = state.impure
    val sched = schedule(inputs,result,scope,impure,options,motion,scheduler)

    state.cache  = saveCache
    state.scope  = saveScope
    state.impure = saveImpure
    if (motion) {
      state.scope ++= sched.motioned
      state.impure ++= sched.motionedImpure
    }

    if (config.enLog) {
      logs(s"Completed block ${sched.block}")
      sched.block.stms.foreach{s => logs(s"  ${stm(s)}") }
      logs(s"Effects: ${sched.block.effects}")
      logs(s"Escaping: ")
      sched.motioned.foreach{s => logs(s"  ${stm(s)}")}
      val dropped = scope diff (sched.block.stms ++ sched.motioned)
      if (dropped.nonEmpty) {
        logs(s"Dropped: ")
        dropped.foreach{s => logs(s"  ${stm(s)}") }
      }
    }

    sched.block
  }

  @stateful def stageBlock[R](block: => Sym[R], options: BlockOptions = BlockOptions.Normal): Block[R] = {
    stageScope(Nil, options)(block)
  }
  @stateful def stageLambda1[A,R](a: Sym[A])(block: => Sym[R], options: BlockOptions = BlockOptions.Normal): Lambda1[A,R] = {
    stageScope(Seq(a), options)(block).asLambda1[A]
  }
  @stateful def stageLambda2[A,B,R](a: Sym[A], b: Sym[B])(block: => Sym[R], options: BlockOptions = BlockOptions.Normal): Lambda2[A,B,R] = {
    stageScope(Seq(a,b), options)(block).asLambda2[A,B]
  }
  @stateful def stageLambda3[A,B,C,R](a: Sym[A], b: Sym[B], c: Sym[C])(block: => Sym[R], options: BlockOptions = BlockOptions.Normal): Lambda3[A,B,C,R] = {
    stageScope(Seq(a,b,c), options)(block).asLambda3[A,B,C]
  }

}
