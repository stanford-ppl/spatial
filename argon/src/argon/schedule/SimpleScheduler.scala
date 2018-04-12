package argon
package schedule

import scala.collection.mutable

class SimpleScheduler extends Scheduler {
  def mustMotion = false

  /** Returns the schedule of the given scope. */
  override def apply[R](
    inputs:  Seq[Sym[_]],
    result:  Sym[R],
    scope:   Seq[Sym[_]],
    impure:  Seq[Impure],
    options: BlockOptions,
    allowMotion: Boolean
  ): Schedule[R] = {
    val effects = summarizeScope(impure)

    val unused = mutable.HashSet.empty[Sym[_]]

    // Simple dead code elimination
    scope.reverseIterator.foreach{s =>
      val uses = s.consumers diff unused
      if (s != result && uses.isEmpty && s.effects.isIdempotent) unused += s
    }
    val keep  = scope.filter{s => !unused.contains(s) }

    val block = new Block[R](inputs,keep,result,effects,options)
    Schedule(block, Nil)
  }
}

object SimpleScheduler extends SimpleScheduler
