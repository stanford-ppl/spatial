package spatial.data

import argon._
import forge.tags._
import scala.collection.mutable.HashMap

case class UnusedAccess(flag: Boolean) extends StableData[UnusedAccess]

object isUnusedAccess {
  def apply(access: Sym[_]): Boolean = metadata[UnusedAccess](access).exists(_.flag)
  def update(access: Sym[_], flag: Boolean): Unit = metadata.add(access, UnusedAccess(flag))
}


case class MPendingUses(nodes: HashMap[Sym[_],Set[Sym[_]]]) extends FlowData[MPendingUses]
@data object pendingUses {
  def all: HashMap[Sym[_],Set[Sym[_]]] = globals[MPendingUses].map(_.nodes).getOrElse(HashMap.empty)
  def apply(x: Sym[_]): Set[Sym[_]] = pendingUses.all.getOrElse(x, Set.empty)
  def +=(entry: (Sym[_], Set[Sym[_]])): Unit = pendingUses.all += entry
  def reset(): Unit = globals.add(MPendingUses(HashMap.empty))
}


case class User(sym: Sym[_], blk: Ctrl)

/** List of consumers of a given node (for ephemeral node removal). */
case class MUsers(users: Set[User]) extends FlowData[MUsers]
object usersOf {
  def apply(x: Sym[_]): Set[User] = metadata[MUsers](x).map(_.users).getOrElse(Set.empty)
  def update(x: Sym[_], users: Set[User]): Unit = metadata.add(x, MUsers(users))
}