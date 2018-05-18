package spatial.data

import argon._
import forge.tags._
import scala.collection.mutable.HashMap

/** Flag marking unused accesses to memories which can be removed.
  *
  * Getter:  sym.isUnusedAccess
  * Setter:  sym.isUnusedAccess = (true | false)
  * Default: false
  */
case class UnusedAccess(flag: Boolean) extends StableData[UnusedAccess]

/** Tuple class for holding a user symbol and usage location. */
case class User(sym: Sym[_], blk: Ctrl)

/** Set of location-annotated consumers of a given node (for ephemeral node removal).
  *
  * Getter:  sym.users
  * Setter:  sym.users = (Set[User])
  * Default: empty set
  */
case class Users(users: Set[User]) extends ConsumerData[Users]

/** Set of local memory reads which each symbol uses
  * Used to detect accumulation cycles
  *
  * Getter:  sym.readUses
  * Setter:  sym.readUses
  * Default: empty set
  */
case class ReadUses(reads: Set[Sym[_]]) extends ConsumerData[ReadUses]


trait AccessData {

  implicit class UsageOps(s: Sym[_]) {
    def isUnusedAccess: Boolean = metadata[UnusedAccess](s).exists(_.flag)
    def isUnusedAccess_=(flag: Boolean): Unit = metadata.add(s, UnusedAccess(flag))

    def users: Set[User] = metadata[Users](s).map(_.users).getOrElse(Set.empty)
    def users_=(use: Set[User]): Unit = metadata.add(s, Users(use))

    def readUses: Set[Sym[_]] = metadata[ReadUses](s).map(_.reads).getOrElse(Set.empty)
    def readUses_=(use: Set[Sym[_]]): Unit = metadata.add(s, ReadUses(use))
  }

}
