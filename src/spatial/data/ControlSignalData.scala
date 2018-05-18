package spatial.data

import argon._

case class StreamInfo(a: Sym[_], b: Sym[_])
case class PortMap(memId: Int, argInId: Int, argOutId: Int)

/** List of fifos or streams popped in a given controller, for handling streampipe control flow.
  *
  * Getter:  sym.listensTo
  * Setter:  sym.listensTo = (List[StreamInfo])
  * Default: Nil
  */
case class ListenStreams(listen: List[StreamInfo]) extends InputData[ListenStreams]


/** List of fifos or streams pushed in a given controller, for handling streampipe control flow.
  *
  * Getter:  sym.pushesTo
  * Setter:  sym.pushesTo = (List[StreamInfo])
  * Default: Nil
  */
case class PushStreams(push: List[StreamInfo]) extends InputData[PushStreams]


/** List of fifos or streams popped in a given controller, for handling streampipe control flow.
  *
  * Getter:  sym.isAligned
  * Setter:  sym.isAligned = (true | false)
  * Default: false
  */
case class AlignedTransfer(is: Boolean) extends StableData[AlignedTransfer]


/** Map for tracking which control nodes are the tile transfer nodes for a given memory, since this
  * alters the enable signal.
  *
  * Getter:  sym.loadCtrl
  * Setter:  sym.loadCtrl = (List[ Sym[_] ])
  * Default: Nil
  **/
case class LoadMemCtrl(ctrl: List[Sym[_]]) extends InputData[LoadMemCtrl]


/** TODO: Update description
  *
  * Getter:  sym.argMapping
  * Setter:  sym.argMapping = (PortMap)
  * Default: PortMap(-1,-1,-1)
  */
case class ArgMap(map: PortMap) extends StableData[ArgMap]


/** Fringe that the StreamIn or StreamOut is associated with.
  *
  * Option:  sym.getFringe
  * Setter:  sym.setFringe(_ : Sym[_])
  * Default: undefined
  */
case class Fringe(fringe: Sym[_]) extends ConsumerData[Fringe]



trait ControlSignalData {

  implicit class ControlSignalOps(s: Sym[_]) {
    def listensTo: List[StreamInfo] = metadata[ListenStreams](s).map(_.listen).getOrElse(Nil)
    def listensTo_=(listen: List[StreamInfo]): Unit = metadata.add(s, ListenStreams(listen))

    def pushesTo: List[StreamInfo] = metadata[PushStreams](s).map(_.push).getOrElse(Nil)
    def pushesTo_=(push: List[StreamInfo]): Unit = metadata.add(s, PushStreams(push))

    def isAligned: Boolean = metadata[AlignedTransfer](s).exists(_.is)
    def isAligned_=(flag: Boolean): Unit = metadata.add(s, AlignedTransfer(flag))

    def loadCtrl: List[Sym[_]] = metadata[LoadMemCtrl](s).map(_.ctrl).getOrElse(Nil)
    def loadCtrl_=(ls: List[Sym[_]]): Unit = metadata.add(s, LoadMemCtrl(ls))

    def argMapping: PortMap = metadata[ArgMap](s).map{a => a.map}.getOrElse(PortMap(-1,-1,-1))
    def argMapping_=(id: PortMap): Unit = metadata.add(s, ArgMap(id))

    def getFringe: Option[Sym[_]] = metadata[Fringe](s).map(_.fringe)
    def setFringe(fringe: Sym[_]): Unit = metadata.add(s, Fringe(fringe))
  }

  implicit class ControlSignalOpsCtrl(ctrl: Ctrl) {
    def listensTo: List[StreamInfo] = ctrl.s.map(_.listensTo).getOrElse(Nil)
    def listensTo_=(listen: List[StreamInfo]): Unit = ctrl.s.foreach{s => s.listensTo = listen }

    def pushesTo: List[StreamInfo] = ctrl.s.map(_.pushesTo).getOrElse(Nil)
    def pushesTo_=(push: List[StreamInfo]): Unit = ctrl.s.foreach{s => s.pushesTo = push }

    def loadCtrl: List[Sym[_]] = ctrl.s.map(_.loadCtrl).getOrElse(Nil)
    def loadCtrl_=(ls: List[Sym[_]]): Unit = ctrl.s.foreach{s => s.loadCtrl = ls }
  }

}