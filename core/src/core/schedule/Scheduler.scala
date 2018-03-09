package core
package schedule

import scala.collection.mutable

trait Scheduler {
  /** If true overrides the compiler's current code motioning flag. */
  def mustMotion: Boolean

  /**
    * Computes an *external* summary for a sequence of nodes
    * (Ignores reads/writes on data allocated within the scope)
    */
  def summarizeScope(impure: Seq[Impure]): Effects = {
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
    effects.copy(reads = effects.reads diff allocs, writes = effects.writes diff allocs, antiDeps = impure)
  }

  /** Returns the schedule of the given scope. */
  def apply[R](
    inputs:  Seq[Sym[_]],
    result:  Sym[R],
    scope:   Seq[Sym[_]],
    impure:  Seq[Impure],
    options: BlockOptions,
    allowMotion: Boolean
  ): Schedule[R]

}

