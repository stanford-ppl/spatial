package core
package schedule

class SimpleScheduler extends Scheduler {
  /** Returns the schedule of the given scope. **/
  override def apply[R](
    inputs:  Seq[Sym[_]],
    result:  Sym[R],
    scope:   Seq[Sym[_]],
    impure:  Seq[Impure],
    options: BlockOptions,
    allowMotion: Boolean
  ): Schedule[R] = {
    val effects = summarizeScope(impure)
    val block = Block[R](inputs,scope,result,effects,options)
    Schedule(block, Nil)
  }
}

object SimpleScheduler extends SimpleScheduler
