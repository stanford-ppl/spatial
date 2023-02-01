package spatial.metadata.memory

import argon._
import spatial.util.MapStructType

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


/** Set of resetters or a given memory.
  *
  * Getter:  sym.resetters
  * Setter:  sym.resetters = (Set[ Sym[_] ])
  * Default: empty set
  */
case class Resetters(resetters: Set[Sym[_]]) extends Data[Resetters](SetBy.Flow.Consumer)

/** Symbol of pre-unrolled node that this memory was created from. (Used for accum/fma analysis)
  *
  * Getter:  sym.originalSym
  * Setter:  sym.originalSym = (Set[ Sym[_] ])
  * Default: empty set
  */
case class OriginalSym(forbiddenFruit: Sym[_]) extends Data[OriginalSym](SetBy.Analysis.Self)

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

/** Metadata defined on a RegRead that indicates which RegWrites to ignore when detecting RAW cycles.
  * A RegWrite should be ignored for this analysis if the RegRead occurs after the RegWrite in program order
  * but the RegWrite occurs in an if/then/else block and the RegRead is used to resolve a condition of that
  * same if/then/else.  I.E there is a if, else if, else, where the first branch writes to a reg and the second
  * branch is conditional on the value of this reg and the branches get squashed and the accesses absorb the conditions.
  *
  * Getter: sym.hotSwapPairings
  * Setter: sym.hotSwapPairings = Map[Sym[_], Set[Sym[_]]]
  * Setter: sym.hotSwapPairings(rd: Sym[_]) = Set[Sym[_]]
  * Default: Map()
  */
case class HotSwapPairings(pairings: Map[Sym[_], Set[Sym[_]]]) extends Data[HotSwapPairings](SetBy.Analysis.Self)


/** Metadata that links a FrameHostNew node to its AxiStreamBus StreamIn/Out object
  *
  * Getter: sym.interfaceStream
  * Setter: sym.interfaceStream = Sym[_]
  * Default: Map()
  */
case class InterfaceStream(stream: Sym[_]) extends Data[InterfaceStream](SetBy.Analysis.Self)

/**
  * Explicit memory name
  *
  * Getter:  sym.explicitName
  * Setter:  sym.explicitName = Some(String)
  * Default: None
  */
case class ExplicitName(name: String) extends Data[ExplicitName](SetBy.User)

/**
  * Memory Buffering Amount
  *
  * Getter: sym.bufferAmount
  * setter: sym.bufferAmount = Some(Int)
  * Default: Some(1)
  */

case class StreamBufferAmount(buffering: Int, min: Int = 0, max: Int = Int.MaxValue) extends Data[StreamBufferAmount](SetBy.User)

/**
  * Memory Buffering Index
  */

case class StreamBufferIndex(bufferIndex: spatial.lang.I32) extends Data[StreamBufferIndex](Transfer.Mirror)

case class FifoInits(values: Seq[Sym[_]]) extends Data[FifoInits](Transfer.Mirror)
