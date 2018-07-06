package spatial.metadata.control

import argon._
import forge.tags.data

case class StreamLoads(ctrls: Set[Sym[_]]) extends Data[StreamLoads](GlobalData.Flow)
@data object StreamLoads {
  def all: Set[Sym[_]] = globals[StreamLoads].map(_.ctrls).getOrElse(Set.empty)
  def +=(ctrl: Sym[_]): Unit = globals.add(StreamLoads(StreamLoads.all + ctrl))
}


case class TileTransfers(ctrls: Set[Sym[_]]) extends Data[TileTransfers](GlobalData.Flow)
@data object TileTransfers {
  def all: Set[Sym[_]] = globals[TileTransfers].map(_.ctrls).getOrElse(Set.empty)
  def +=(ctrl: Sym[_]): Unit = globals.add(TileTransfers(TileTransfers.all + ctrl))
}


case class StreamEnablers(streams: Set[Sym[_]]) extends Data[StreamEnablers](GlobalData.Flow)
@data object StreamEnablers {
  def all: Set[Sym[_]] = globals[StreamEnablers].map(_.streams).getOrElse(Set.empty)
  def +=(stream: Sym[_]): Unit = globals.add(StreamEnablers(StreamEnablers.all + stream))
}


case class StreamHolders(streams: Set[Sym[_]]) extends Data[StreamHolders](GlobalData.Flow)
@data object StreamHolders {
  def all: Set[Sym[_]] = globals[StreamHolders].map(_.streams).getOrElse(Set.empty)
  def +=(stream: Sym[_]): Unit = globals.add(StreamHolders(StreamHolders.all + stream))
}


case class StreamParEnqs(ctrls: Set[Sym[_]]) extends Data[StreamParEnqs](GlobalData.Flow)
@data object StreamParEnqs {
  def all: Set[Sym[_]] = globals[StreamParEnqs].map(_.ctrls).getOrElse(Set.empty)
  def +=(ctrl: Sym[_]): Unit = globals.add(StreamParEnqs(StreamParEnqs.all + ctrl))
}
