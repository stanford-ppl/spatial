package spatial.data

import argon._
import forge.tags._
import spatial.lang._

case class StreamLoads(ctrls: Set[Sym[_]]) extends FlowData[StreamLoads]
@data object streamLoadCtrls {
  def all: Set[Sym[_]] = globals[StreamLoads].map(_.ctrls).getOrElse(Set.empty)
  def +=(ctrl: Sym[_]): Unit = globals.add(StreamLoads(streamLoadCtrls.all + ctrl))
}

case class TileTransfers(ctrls: Set[Sym[_]]) extends FlowData[TileTransfers]
@data object tileTransferCtrls {
  def all: Set[Sym[_]] = globals[TileTransfers].map(_.ctrls).getOrElse(Set.empty)
  def +=(ctrl: Sym[_]): Unit = globals.add(TileTransfers(tileTransferCtrls.all + ctrl))
}

case class MStreamEnablers(streams: Set[Sym[_]]) extends FlowData[MStreamEnablers]
@data object streamEnablers {
  def all: Set[Sym[_]] = globals[MStreamEnablers].map(_.streams).getOrElse(Set.empty)
  def +=(stream: Sym[_]): Unit = globals.add(MStreamEnablers(streamEnablers.all + stream))
}

case class MStreamHolders(streams: Set[Sym[_]]) extends FlowData[MStreamHolders]
@data object streamHolders {
  def all: Set[Sym[_]] = globals[MStreamHolders].map(_.streams).getOrElse(Set.empty)
  def +=(stream: Sym[_]): Unit = globals.add(MStreamHolders(streamHolders.all + stream))
}

case class MStreamParEnqs(ctrls: Set[Sym[_]]) extends FlowData[MStreamParEnqs]
@data object streamParEnqs {
  def all: Set[Sym[_]] = globals[MStreamParEnqs].map(_.ctrls).getOrElse(Set.empty)
  def +=(ctrl: Sym[_]): Unit = globals.add(MStreamParEnqs(streamParEnqs.all + ctrl))
}


