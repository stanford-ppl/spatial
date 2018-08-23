package spatial.metadata.control

import argon._

case class StreamInfo(a: Sym[_], b: Sym[_])
case class PortMap(memId: Int, argInId: Int, argOutId: Int)

/** List of fifos or streams popped in a given controller, for handling streampipe control flow.
  *
  * Getter:  sym.listensTo
  * Setter:  sym.listensTo = (List[StreamInfo])
  * Default: Nil
  */
case class ListenStreams(listen: List[StreamInfo]) extends Data[ListenStreams](Transfer.Remove)


/** List of fifos or streams pushed in a given controller, for handling streampipe control flow.
  *
  * Getter:  sym.pushesTo
  * Setter:  sym.pushesTo = (List[StreamInfo])
  * Default: Nil
  */
case class PushStreams(push: List[StreamInfo]) extends Data[PushStreams](Transfer.Remove)


/** List of fifos or streams popped in a given controller, for handling streampipe control flow.
  *
  * Getter:  sym.isAligned
  * Setter:  sym.isAligned = (true | false)
  * Default: false
  */
case class AlignedTransfer(is: Boolean) extends Data[AlignedTransfer](Transfer.Mirror)


/** Map for tracking which control nodes are the tile transfer nodes for a given memory, since this
  * alters the enable signal.
  *
  * Getter:  sym.loadCtrl
  * Setter:  sym.loadCtrl = (List[ Sym[_] ])
  * Default: Nil
  **/
case class LoadMemCtrl(ctrl: List[Sym[_]]) extends Data[LoadMemCtrl](Transfer.Remove)


/** TODO: Update description
  *
  * Getter:  sym.argMapping
  * Setter:  sym.argMapping = (PortMap)
  * Default: PortMap(-1,-1,-1)
  */
case class ArgMap(map: PortMap) extends Data[ArgMap](Transfer.Mirror)


/** Fringe that the StreamIn or StreamOut is associated with.
  *
  * Option:  sym.getFringe
  * Setter:  sym.setFringe(_ : Sym[_])
  * Default: undefined
  */
case class Fringe(fringe: Sym[_]) extends Data[Fringe](SetBy.Analysis.Consumer)
