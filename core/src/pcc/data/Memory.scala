package pcc.data

import forge._
import pcc.core._
import pcc.lang._

case class LocalMemories(memories: Set[Sym[_]]) extends ComplexData[LocalMemories]
@data object localMems {
  def all: Set[Sym[_]] = globals[LocalMemories].map(_.memories).getOrElse(Set.empty)
  def +=(mem: Sym[_]): Unit = globals.add(LocalMemories(localMems.all + mem ))
}

case class Readers(readers: Set[Sym[_]]) extends ComplexData[Readers]
@data object readersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Readers](x).map(_.readers).getOrElse(Set.empty)
  def update(x: Sym[_], readers: Set[Sym[_]]): Unit = metadata.add(x, Readers(readers))
}

case class Writers(writers: Set[Sym[_]]) extends ComplexData[Writers]
@data object writersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Writers](x).map(_.writers).getOrElse(Set.empty)
  def update(x: Sym[_], writers: Set[Sym[_]]): Unit = metadata.add(x, Writers(writers))
}

/*case class Users(users: Set[Sym[_]]) extends ComplexData[Users]
@data object usersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Users](x).map(_.)
}*/