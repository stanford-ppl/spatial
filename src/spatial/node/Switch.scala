package spatial.node

import core._
import core.schedule.Schedule
import forge.tags._
import spatial.lang._

object SwitchScheduler extends core.schedule.Scheduler {
  override def mustMotion: Boolean = true

  /** Returns the schedule of the given scope. **/
  override def apply[R](
    inputs:  Seq[Sym[_]],
    result:  Sym[R],
    scope:   Seq[Sym[_]],
    impure:  Seq[Impure],
    options: BlockOptions,
    allowMotion: Boolean
  ): Schedule[R] = {
    val (keep,motion) = scope.partition{_.op.exists(_.isInstanceOf[SwitchCase[_]])}
    val (keepI,motionI) = impure.partition(_.sym.op.exists(_.isInstanceOf[SwitchCase[_]]))
    val effects = summarizeScope(keepI)
    val result = keep.last.asInstanceOf[Sym[R]]
    val block = Block[R](inputs,keep,result,effects,options)
    Schedule(block,motion,motionI)
  }
}

@op case class SwitchCase[R:Type](body: Block[R]) extends Control[R] {
  def iters = Nil
  def cchains = Nil
  def bodies = Seq(Nil -> Seq(body))
}

@op case class Switch[R:Type](selects: Seq[Bit], body: Block[R]) extends Control[R] {
  def iters = Nil
  def cchains = Nil
  def bodies = Seq(Nil -> Seq(body))

  override def aliases = syms(cases.map(_.body.result))

  def cases: Seq[SwitchCase[R]] = {
    body.stms.collect{case Op(op:SwitchCase[_]) => op.asInstanceOf[SwitchCase[R]] }
  }

  override def inputs = syms(selects) ++ syms(body)
}
