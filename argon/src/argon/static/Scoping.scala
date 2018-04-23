package argon
package static

import argon.schedule._
import forge.tags._
import utils.tags.instrument

trait Scoping {

  @stateful @inline private def reify[R](block: => R): R = {
    if (state.isStaging) state.logTab += 1
    val result = block
    if (state.isStaging) state.logTab -= 1
    result
  }

  @stateful def stageScope_Schedule[R](
    inputs: Seq[Sym[_]],
    result: Sym[R],
    scope:  Seq[Sym[_]],
    impure: Seq[Impure],
    options: BlockOptions,
    motion: Boolean,
    scheduler: Scheduler
  ): Block[R] = {
    val schedule = scheduler(inputs,result,scope,impure,options,motion)

    if (motion) {
      state.scope ++= schedule.motioned
      state.impure ++= schedule.motionedImpure
    }

    if (config.enLog) {
      logs(s"Completed block ${schedule.block}")
      logs(s"Inputs:  $inputs")
      schedule.block.stms.foreach{s => logs(s"  ${stm(s)}") }
      logs(s"Effects: ${schedule.block.effects}")
      logs(s"Escaping: ")
      schedule.motioned.foreach{s => logs(s"  ${stm(s)}")}
      val dropped = scope diff (schedule.block.stms ++ schedule.motioned)
      if (dropped.nonEmpty) {
        logs(s"Dropped: ")
        dropped.foreach{s => logs(s"  ${stm(s)}") }
      }
    }

    schedule.block
  }

  /** Stage the effects of an isolated scope with the given inputs.
    * Returns the scope as a schedulable list of statements
    *
    * @param inputs Bound inputs to this block (for lambda functions)
    * @param block Call by name reference to the scope
    */
  @stateful def stageScope_Start[R](
    inputs:  Seq[Sym[_]],
    options: BlockOptions = BlockOptions.Normal
  )(
    block: => R
  ): (R, Seq[Sym[_]], Seq[Impure], Scheduler, Boolean) = {
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

    state.cache  = saveCache
    state.scope  = saveScope
    state.impure = saveImpure

    (result, scope, impure, scheduler, motion)
  }


  /** Stage the effects of an isolated scope with the given inputs.
    * Schedule the resulting scope statements as a Block with the given or default scheduler.
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
    val (result, scope, impure, scheduler, motion) = stageScope_Start(inputs, options){ block }
    stageScope_Schedule(inputs, result, scope, impure, options, motion, scheduler)
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
