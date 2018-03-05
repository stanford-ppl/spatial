package spatial.data

import forge.tags._
import core._
import spatial.lang._

/**
  * Global set of all local memories
  *
  * If undefined, assumed to be empty
  */
case class LocalMemories(memories: Set[Sym[_]]) extends FlowData[LocalMemories]
@data object localMems {
  def all: Set[Sym[_]] = globals[LocalMemories].map(_.memories).getOrElse(Set.empty)
  def +=(mem: Sym[_]): Unit = globals.add(LocalMemories(localMems.all + mem ))
}

/**
  * Set of reader symbols for each local memory
  *
  * If undefined, assumed to be empty
  */
case class Readers(readers: Set[Sym[_]]) extends FlowData[Readers]
object readersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Readers](x).map(_.readers).getOrElse(Set.empty)
  def update(x: Sym[_], readers: Set[Sym[_]]): Unit = metadata.add(x, Readers(readers))
}

/**
  * Set of writer symbols for each local memory
  *
  * If undefined, assumed to be empty
  */
case class Writers(writers: Set[Sym[_]]) extends FlowData[Writers]
object writersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Writers](x).map(_.writers).getOrElse(Set.empty)
  def update(x: Sym[_], writers: Set[Sym[_]]): Unit = metadata.add(x, Writers(writers))
}

object accessesOf {
  def apply(x: Sym[_]): Set[Sym[_]] = writersOf(x) ++ readersOf(x)
}

/**
  * Set of local memory reads which each symbol uses
  * Used to detect accumulation cycles
  *
  * If undefined, assumed to be empty
  */
case class ReadUses(reads: Set[Sym[_]]) extends FlowData[ReadUses]
object readUsesOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[ReadUses](x).map(_.reads).getOrElse(Set.empty)
  def update(x: Sym[_], reads: Set[Sym[_]]): Unit = metadata.add(x, ReadUses(reads))
}

sealed abstract class AccumType {
  def |(that: AccumType): AccumType
  def >(that: AccumType): Boolean
  final def >=(that: AccumType): Boolean = this > that || this == that
}
object AccumType {
  case object Fold extends AccumType {
    def |(that: AccumType): AccumType = this
    def >(that: AccumType): Boolean = that != Fold
  }
  case object Buff extends AccumType {
    def |(that: AccumType): AccumType = that match {case Fold => Fold; case _ => Buff}
    def >(that: AccumType): Boolean = (that | Reduce) match {case Reduce => true; case _ => false }
  }
  case object Reduce extends AccumType {
    def |(that: AccumType): AccumType = that match {case None => Reduce; case _ => that}
    def >(that: AccumType): Boolean = that match {case None => true; case _ => false }
  }
  case object None extends AccumType {
    def |(that: AccumType): AccumType = that
    def >(that: AccumType): Boolean = false
  }
}

/**
  * Flags that this symbol is associated with an accumulator
  * If this symbol is a memory, this memory is an accumulator
  * If this symbol is a memory access, the access is a read or write to an accumulator
  *
  * If undefined, assumed to be false
  */
case class Accumulator(tp: AccumType) extends StableData[Accumulator]
object accumTypeOf {
  def apply(x: Sym[_]): AccumType = metadata[Accumulator](x).map(_.tp).getOrElse(AccumType.None)
  def update(x: Sym[_], tp: AccumType): Unit = metadata.add(x, Accumulator(tp))
}


/*case class Users(users: Set[Sym[_]]) extends ComplexData[Users]
@data object usersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Users](x).map(_.)
}*/