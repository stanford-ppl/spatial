package spatial.metadata.memory

import argon._
import forge.tags.data

/** Global set of all local memories.
  *
  * Getter:  LocalMems.all
  * Append:  LocalMems += (mem)
  * Default: empty set
  */
case class LocalMemories(memories: Set[Sym[_]]) extends Data[LocalMemories](GlobalData.Flow)
@data object LocalMemories {
  def all: Set[Sym[_]] = globals[LocalMemories].map(_.memories).getOrElse(Set.empty)
  def +=(mem: Sym[_]): Unit = globals.add(LocalMemories(LocalMemories.all + mem ))
  def -=(mem: Sym[_]): Unit = globals.add(LocalMemories(LocalMemories.all - mem ))
}
