package spatial.metadata.memory

import argon._
import forge.tags.data

/** Global set of all remote memories.
  *
  * Getter:  RemoteMems.all
  * Append:  RemoteMems += (mem)
  * Default: empty set
  */
case class RemoteMemories(memories: Set[Sym[_]]) extends Data[RemoteMemories](GlobalData.Flow)
@data object RemoteMemories {
  def all: Set[Sym[_]] = globals[RemoteMemories].map(_.memories).getOrElse(Set.empty)
  def +=(mem: Sym[_]): Unit = globals.add(RemoteMemories(RemoteMemories.all + mem ))
}
