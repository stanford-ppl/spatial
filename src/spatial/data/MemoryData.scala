package spatial.data

import argon._

/** Set of reader symbols for each local memory
  *
  * Getter:  sym.readers
  * Setter:  sym.readers = (Set[ Sym[_] ])
  * Default: empty set
  */
case class Readers(readers: Set[Sym[_]]) extends Data[Readers](SetBy.Flow.Consumer)


/**
  * Set of writer symbols for each local memory
  *
  * Getter:  sym.writers
  * Setter:  sym.writers = (Set[ Sym[_] ])
  * Default: empty set
  */
case class Writers(writers: Set[Sym[_]]) extends Data[Writers](SetBy.Flow.Consumer)


/** Set of resetters for a given memory.
  *
  * Getter:  sym.resetters
  * Setter:  sym.resetters = (Set[ Sym[_] ])
  * Default: empty set
  */
case class Resetters(resetters: Set[Sym[_]]) extends Data[Resetters](SetBy.Flow.Consumer)


/** Marks that a memory is never used (and can be removed)
  *
  * Getter: sym.isUnusedMemory
  * Setter: sym.isUnusedMemory = (true|false)
  * Default: false
  */
case class UnusedMemory(flag: Boolean) extends Data[UnusedMemory](SetBy.Analysis.Consumer)


trait MemoryData {

  implicit class MemoryAccessOps(s: Sym[_]) {
    def readers: Set[Sym[_]] = metadata[Readers](s).map(_.readers).getOrElse(Set.empty)
    def readers_=(rds: Set[Sym[_]]): Unit = metadata.add(s, Readers(rds))

    def writers: Set[Sym[_]] = metadata[Writers](s).map(_.writers).getOrElse(Set.empty)
    def writers_=(wrs: Set[Sym[_]]): Unit = metadata.add(s, Writers(wrs))

    def accesses: Set[Sym[_]] = s.readers ++ s.writers

    def resetters: Set[Sym[_]] = metadata[Resetters](s).map(_.resetters).getOrElse(Set.empty)
    def resetters_=(rst: Set[Sym[_]]): Unit = metadata.add(s, Resetters(rst))

    def isUnusedMemory: Boolean = metadata[UnusedMemory](s).exists(_.flag)
    def isUnusedMemory_=(flag: Boolean): Unit = metadata.add(s, UnusedMemory(flag))
  }

}