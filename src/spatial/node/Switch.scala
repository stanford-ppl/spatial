package spatial.node

import argon._
import argon.schedule.Schedule
import forge.tags._
import spatial.lang._

/** Custom scheduler for Switch nodes.
  * The scheduler always motions all operations except SwitchCases out of the Switch body
  */
object SwitchScheduler extends argon.schedule.Scheduler {
  override def mustMotion: Boolean = true

  /** Returns the schedule of the given scope. */
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
    val block = new Block[R](inputs,keep,result,effects,options)
    Schedule(block,motion,motionI)
  }
}

/** A single case within a Switch statement
  * NOTE: SwitchCase should never exist outside a Switch
  * @param body The operations done in the given case
  */
@op case class SwitchCase[R:Type](body: Block[R]) extends Control[R] {
  def iters = Nil
  def cchains = Nil
  def bodies = Seq(PseudoStage(Nil, Seq(body)))
}

/** A (nestable) hardware case matching statement
  * @param selects Associated conditions for each SwitchCase
  * @param body A list of [[SwitchCase]]s
  */
@op case class Switch[R:Type](selects: Seq[Bit], body: Block[R]) extends Control[R] {
  def iters = Nil
  def cchains = Nil
  def bodies = Seq(PseudoStage(Nil, Seq(body)))

  override def aliases = syms(cases.map(_.body.result))

  def cases: Seq[SwitchCase[R]] = {
    body.stms.collect{case Op(op:SwitchCase[_]) => op.asInstanceOf[SwitchCase[R]] }
  }

  override def inputs = syms(selects).toSeq ++ syms(body)
}
