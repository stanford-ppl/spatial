package spatial.data

import argon._

/** Set of reader symbols for each local memory
  *
  * Getter:  sym.readers
  * Setter:  sym.readers = (Set[ Sym[_] ])
  * Default: empty set
  */
case class Readers(readers: Set[Sym[_]]) extends FlowData[Readers]


/**
  * Set of writer symbols for each local memory
  *
  * Getter:  sym.writers
  * Setter:  sym.writers = (Set[ Sym[_] ])
  * Default: empty set
  */
case class Writers(writers: Set[Sym[_]]) extends FlowData[Writers]


/** Set of resetters for a given memory.
  *
  * Getter:  sym.resetters
  * Setter:  sym.resetters = (Set[ Sym[_] ])
  * Default: empty set
  */
case class Resetters(resetters: Set[Sym[_]]) extends FlowData[Resetters]



trait MemoryData {

  implicit class MemoryAccessOps(s: Sym[_]) {
    def readers: Set[Sym[_]] = metadata[Readers](s).map(_.readers).getOrElse(Set.empty)
    def readers_=(rds: Set[Sym[_]]): Unit = metadata.add(s, Readers(readers))

    def writers: Set[Sym[_]] = metadata[Writers](s).map(_.writers).getOrElse(Set.empty)
    def writers_=(wrs: Set[Sym[_]]): Unit = metadata.add(s, Writers(writers))

    def accesses: Set[Sym[_]] = s.readers ++ s.writers

    def resetters: Set[Sym[_]] = metadata[Resetters](s).map(_.resetters).getOrElse(Set.empty)
    def resetters_=(rst: Set[Sym[_]]): Unit = metadata.add(s, Resetters(resetters))
  }

}