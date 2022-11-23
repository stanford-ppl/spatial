package spatial.metadata.access

import argon._
import argon.lang.{Bit, I32, Idx, Num}
import forge.tags.stateful
import spatial.metadata.control.Blk

/** Flag marking unused accesses to memories which can be removed.
  *
  * Getter:  sym.isUnusedAccess
  * Setter:  sym.isUnusedAccess = (true | false)
  * Default: false
  */
case class UnusedAccess(flag: Boolean) extends Data[UnusedAccess](SetBy.Analysis.Consumer)

/** Tuple class for holding a user symbol and usage location. */
case class User(sym: Sym[_], blk: Blk)

/** Set of location-annotated consumers of a given node (for ephemeral node removal).
  *
  * Getter:  sym.users
  * Setter:  sym.users = (Set[User])
  * Default: empty set
  */
case class Users(users: Set[User]) extends Data[Users](SetBy.Analysis.Consumer)

/** Set of local memory reads which each symbol uses
  * Used to detect accumulation cycles
  *
  * Getter:  sym.readUses
  * Setter:  sym.readUses
  * Default: empty set
  */
case class ReadUses(reads: Set[Sym[_]]) extends Data[ReadUses](SetBy.Flow.Consumer)


/** Set a tag so we can figure out which PriorityDeqs are grouped together
  *
  * Getter:  sym.prDeqGrp
  * Setter:  sym.prDeqGrp
  * Default: none
  */
case class PriorityDeqGroup(grp: Int) extends Data[PriorityDeqGroup](SetBy.Analysis.Self)

case class DoesNotConflictWith(group: Set[Sym[_]]) extends Data[DoesNotConflictWith](Transfer.Mirror) {
  override def mirror(f: Tx): DoesNotConflictWith = DoesNotConflictWith(f(group))
}

// DependencyEdges are Access-to-Access dependencies
// src syms should happen before dst syms.
trait DependencyEdge {
  def src: Set[Sym[_]]
  def dst: Set[Sym[_]]
  def srcSend(ts: TimeStamp)(implicit state: argon.State): Bit
  def dstRecv(ts: TimeStamp)(implicit state: argon.State): Bit
  def srcIterators: Set[Sym[Num[_]]]
  def dstIterators: Set[Sym[Num[_]]]

  def accessedMems(implicit state: argon.State): Set[Sym[_]] = (src ++ dst).flatMap(_.accessedMem)
}

trait CoherentEdge { this: DependencyEdge =>
  def mem: Sym[_]
}

// Map from iterator to current iteration
case class TimeStamp(timeMap: Map[Sym[Num[_]], Num[_]]) {
  def apply[T: Num](s: Sym[T]): T = timeMap(s.asInstanceOf[Sym[Num[T]]]).asInstanceOf[T]
}

case class DependencyEdges(edges: Seq[DependencyEdge]) extends Data[DependencyEdges](Transfer.Mirror)
