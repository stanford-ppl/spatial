package spatial.metadata.params

import argon._
import forge.tags.data

/** Global set of all dse params to ignore.
  *
  * Getter:  IgnoreParams.all
  * Append:  IgnoreParams += (mem)
  * Default: empty set
  */
case class IgnoreParams(params: Set[Sym[_]]) extends Data[IgnoreParams](GlobalData.Flow)
@data object IgnoreParams {
  def all: Set[Sym[_]] = globals[IgnoreParams].map(_.params).getOrElse(Set.empty)
  def +=(param: Sym[_]): Unit = globals.add(IgnoreParams(IgnoreParams.all + param ))
}

/** Global set of all tilesize params.
  *
  * Getter:  TileSizes.all
  * Append:  TileSizes += (mem)
  * Default: empty set
  */
case class TileSizes(params: Set[Sym[_]]) extends Data[TileSizes](GlobalData.Flow)
@data object TileSizes {
  def all: Set[Sym[_]] = globals[TileSizes].map(_.params).getOrElse(Set.empty)
  def +=(param: Sym[_]): Unit = globals.add(TileSizes(TileSizes.all + param ))
}


/** Global set of all par params.
  *
  * Getter:  ParParams.all
  * Append:  ParParams += (mem)
  * Default: empty set
  */
case class ParParams(params: Set[Sym[_]]) extends Data[ParParams](GlobalData.Flow)
@data object ParParams {
  def all: Set[Sym[_]] = globals[ParParams].map(_.params).getOrElse(Set.empty)
  def +=(param: Sym[_]): Unit = globals.add(ParParams(ParParams.all + param ))
}
