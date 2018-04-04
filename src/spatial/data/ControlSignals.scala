package spatial.data

import argon._

case class StreamInfo(a: Sym[_], b: Sym[_])
case class PortMap(memId: Int, argInId: Int, argOutId: Int)

/**
  * List of fifos or streams popped in a given controller, for handling streampipe control flow
  */
case class ListenStreams(listen: List[StreamInfo]) extends AnalysisData[ListenStreams]

object listensTo {
  def apply(x: Sym[_]): List[StreamInfo] = metadata[ListenStreams](x).map(_.listen).getOrElse(Nil)
  def update(x: Sym[_], listen: List[StreamInfo]): Unit = metadata.add(x, ListenStreams(listen))

  def apply(x: Ctrl): List[StreamInfo] = listensTo(x.s.get)
  def update(x: Ctrl, listen: List[StreamInfo]): Unit = listensTo(x.s.get) = listen
}

/**
  * List of fifos or streams popped in a given controller, for handling streampipe control flow
  */
case class AlignedTransfer(is: Boolean) extends StableData[AlignedTransfer]
object isAligned {
  def apply(x: Sym[_]): Boolean = metadata[AlignedTransfer](x).exists(_.is)
  def update(x: Sym[_], is: Boolean): Unit = metadata.add(x, AlignedTransfer(is))
}


/**
  * Map for tracking which control nodes are the tile transfer nodes for a given memory, since this
  * alters the enable signal
  **/
case class LoadMemCtrl(ctrl: List[Sym[_]]) extends AnalysisData[LoadMemCtrl]
object loadCtrlOf {
  def apply(x: Sym[_]): List[Sym[_]] = metadata[LoadMemCtrl](x).map(_.ctrl).getOrElse(Nil)
  def update(x: Sym[_], ctrl: List[Sym[_]]): Unit = metadata.add(x, LoadMemCtrl(ctrl))

  def apply(x: Ctrl): List[Sym[_]] = loadCtrlOf(x.s.get)
  def update(x: Ctrl, ctrl: List[Sym[_]]): Unit = loadCtrlOf(x.s.get) = ctrl
}

/**
  * List of fifos or streams pushed in a given controller, for handling streampipe control flow
  **/
case class PushStreams(push: List[StreamInfo]) extends AnalysisData[PushStreams]
object pushesTo {
  def apply(x: Sym[_]): List[StreamInfo] = metadata[PushStreams](x).map(_.push).getOrElse(Nil)
  def update(x: Sym[_], push: List[StreamInfo]): Unit = metadata.add(x, PushStreams(push))

  def apply(x: Ctrl): List[StreamInfo] = pushesTo(x.s.get)
  def update(x: Ctrl, push: List[StreamInfo]): Unit = pushesTo(x.s.get) = push
}

/**
  * Metadata for determining which memory duplicate(s) an access should correspond to.
  */
case class ArgMap(map: PortMap) extends StableData[ArgMap]
object argMapping {
  def apply(arg: Sym[_]): PortMap = metadata[ArgMap](arg).map{a => a.map}.getOrElse(PortMap(-1,-1,-1))
  def update(arg: Sym[_], id: PortMap ): Unit = metadata.add(arg, ArgMap(id))
}

/**
  * Fringe that the StreamIn or StreamOut is associated with
  **/
case class Fringe(fringe: Sym[_]) extends AnalysisData[Fringe]
object fringeOf {
  def apply(x: Sym[_]): Option[Sym[_]] = metadata[Fringe](x).map(_.fringe)
  def update(x: Sym[_], fringe: Sym[_]): Unit = metadata.add(x, Fringe(fringe))
}
