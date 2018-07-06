package spatial.metadata.access

import argon._
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
