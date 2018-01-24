package pcc.data

import forge._
import pcc.core._
import pcc.lang._

/**
  * Global set of all local memories
  *
  * If undefined, assumed to be empty
  */
case class LocalMemories(memories: Set[Sym[_]]) extends ComplexData[LocalMemories]
@data object localMems {
  def all: Set[Sym[_]] = globals[LocalMemories].map(_.memories).getOrElse(Set.empty)
  def +=(mem: Sym[_]): Unit = globals.add(LocalMemories(localMems.all + mem ))
}

/**
  * Set of reader symbols for each local memory
  *
  * If undefined, assumed to be empty
  */
case class Readers(readers: Set[Sym[_]]) extends ComplexData[Readers]
@data object readersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Readers](x).map(_.readers).getOrElse(Set.empty)
  def update(x: Sym[_], readers: Set[Sym[_]]): Unit = metadata.add(x, Readers(readers))
}

/**
  * Set of writer symbols for each local memory
  *
  * If undefined, assumed to be empty
  */
case class Writers(writers: Set[Sym[_]]) extends ComplexData[Writers]
@data object writersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Writers](x).map(_.writers).getOrElse(Set.empty)
  def update(x: Sym[_], writers: Set[Sym[_]]): Unit = metadata.add(x, Writers(writers))
}

@data object accessesOf {
  def apply(x: Sym[_]): Set[Sym[_]] = writersOf(x) ++ readersOf(x)
}

/**
  * Set of local memory reads which each symbol uses
  * Used to detect accumulation cycles
  *
  * If undefined, assumed to be empty
  */
case class ReadUses(reads: Set[Sym[_]]) extends ComplexData[ReadUses]
@data object readUsesOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[ReadUses](x).map(_.reads).getOrElse(Set.empty)
  def update(x: Sym[_], reads: Set[Sym[_]]): Unit = metadata.add(x, ReadUses(reads))
}

/**
  * Flags that this symbol is associated with an accumulator
  * If this symbol is a memory, this memory is an accumulator
  * If this symbol is a memory access, the access is a read or write to an accumulator
  *
  * If undefined, assumed to be false
  */
case class Accumulator(isAccum: Boolean) extends ComplexData[Accumulator]
@data object isAccum {
  def apply(x: Sym[_]): Boolean = metadata[Accumulator](x).exists(_.isAccum)
  def update(x: Sym[_], flag: Boolean): Unit = metadata.add(x, Accumulator(flag))
}


/*case class Users(users: Set[Sym[_]]) extends ComplexData[Users]
@data object usersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Users](x).map(_.)
}*/