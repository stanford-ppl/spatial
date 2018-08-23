package spatial.metadata

import argon._
import forge.tags._

import scala.collection.mutable.HashMap

/** A map from ephemeral nodes to all ephemeral nodes they (indirectly) use.
  *
  * Used for usage analysis when removing ephemeral nodes.
  * Getter:  pendingUses.all   --- for entire map
  * Getter:  pendingUses(sym)  --- for an individual symbol
  * Setter:  pendingUses += sym -> uses
  * Reset:   pendingUses.reset()
  * Default: Empty map
  */
class PendingUses(val nodes: HashMap[Sym[_],Set[Sym[_]]]) extends Data[PendingUses](GlobalData.Analysis)
@data object PendingUses {
  def all: HashMap[Sym[_],Set[Sym[_]]] = globals[PendingUses].map(_.nodes).getOrElse(HashMap.empty)
  def apply(x: Sym[_]): Set[Sym[_]] = PendingUses.all.getOrElse(x, Set.empty)
  def contains(x: Sym[_]): Boolean = PendingUses.all.contains(x)
  def +=(entry: (Sym[_], Set[Sym[_]])): Unit = PendingUses.all += entry
  def reset(): Unit = globals.add(new PendingUses(HashMap.empty))
}







