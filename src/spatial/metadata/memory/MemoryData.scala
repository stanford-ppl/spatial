package spatial.metadata.memory

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

/** Marks that a memory is a break for some controller
  *
  * Getter: sym.isBreaker
  * Setter: sym.isBreaker = (true|false)
  * Default: false
  */
case class Breaker(flag: Boolean) extends Data[Breaker](SetBy.Analysis.Self)


/** Marks that a memory is never used (and can be removed)
  *
  * Getter: sym.isUnusedMemory
  * Setter: sym.isUnusedMemory = (true|false)
  * Default: false
  */
case class UnusedMemory(flag: Boolean) extends Data[UnusedMemory](SetBy.Analysis.Consumer)


/** Set of accesses with iterators that were treated as random due to lockstep dephasing
  *
  * Getter:  sym.dephasedAccesses
  * Setter:  sym.dephasedAccesses = (Set[ Sym[_] ])
  * Default: empty set
  */
case class DephasedAccess(accesses: Set[Sym[_]]) extends Data[DephasedAccess](SetBy.Analysis.Self)
